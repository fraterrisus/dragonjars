package com.hitchhikerprod.dragonjars.exec;

import com.hitchhikerprod.dragonjars.DragonWarsApp;
import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.ModifiableChunk;
import com.hitchhikerprod.dragonjars.data.RomImageDecoder;
import com.hitchhikerprod.dragonjars.exec.instructions.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

public class Interpreter {
    private static final int MASK_LOW = 0x000000ff;
    private static final int MASK_HIGH = 0x0000ff00;
    private static final int MASK_WORD = 0x0000ffff;

    private final DragonWarsApp app;

    /* Memory space */

    private final List<Chunk> allChunks;
    private final List<ModifiableChunk> segments = new ArrayList<>();
    private final List<Integer> chunkSizes = new ArrayList<>();
    private final List<Integer> chunkFrobs = new ArrayList<>();
    private final List<Integer> chunkIds = new ArrayList<>();

    private final Deque<Byte> stack = new ArrayDeque<>(); // one-byte values
    private final int[] heap = new int[256];
    private final byte[] bufferD1B0 = new byte[896 * 2]; // 0x380 words

    private int draw_borders; // 0x253e
    private int bbox_x0; // 0x2547 (ch)
    private int bbox_y0; // 0x2549 (px)
    private int bbox_x1; // 0x254a (ch)
    private int bbox_y1; // 0x254c (px)

    private int strlen_313c;
    private List<Integer> string_313e = new ArrayList<>();

    private int x_3166;
    private int y_3168;
    private int x_31ed; // default for draw_char
    private int y_31ef; // default for draw_char

    private int invert_3431;

    /* Architectural registers */

    // In assembly, metaregister CS contains the address of the segment to which the current code segment
    // has been loaded. Here I store the structure index instead.
    private int cs;
    private int ds;

    private int ip;

    private boolean width;
    private int ax;
    private int bx;
    private boolean flagCarry; // 0x0001
    private boolean flagZero;  // 0x0040
    private boolean flagSign;  // 0x0080

    // debugging information
    private int instructionsExecuted;

    public Interpreter(DragonWarsApp app, List<Chunk> dataChunks, int initialChunk, int initialAddr) {
        this.app = app;
        this.allChunks = dataChunks;
        this.width = false;
        this.ax = 0;
        this.bx = 0;
        this.flagCarry = false;
        this.flagZero = false;
        this.flagSign = false;
        this.instructionsExecuted = 0;

        // cs:0150  ax <- 0x0000
        // cs:0155  heap[0x00..0x7f] <- 0x00
        // cs:0157  di <- 0xb6a2 [chunks]
        // cs:015a  ax <- 0xffff
        // cs:015b  cx <- 0x0180
        // cs:015e  chunk_map[0x0000..0x017f] <- 0x00

        // create segments 0 and 1, which have the same segment address, to store party data [0x0e00 bytes]
        final ModifiableChunk partyChunk = new ModifiableChunk(new byte[0x0e00]);
        this.chunkIds.add(0xffff); // doesn't correspond to a disk segment
        this.chunkIds.add(0xffff);
        this.chunkFrobs.add(0xff); // don't touch me
        this.chunkFrobs.add(0xff);
        this.chunkSizes.add(0x0001); // shrug?
        this.chunkSizes.add(0x0e00); // accurate
        this.segments.add(partyChunk);
        this.segments.add(partyChunk);

        // build "x50" multiplication table
        // which sets ax to 0x2a..
        // cs:0166  al <- 0xff
        // cs:0168  frob.4d32 <- 0xff
        setHeapBytes(0x56, 2, 0xffff);
        setHeapBytes(0x5a, 2, 0xffff);
        // cs:0177  struct_idx.4d33 <- 0xff
        setHeapBytes(0x08, 1, 0xff);
        // cs:017d  inc ax  (ax <- 0x2b00)
        setHeapBytes(0xdc, 1, 0x00);
        // set_invert():
        //   si = (ax >> 3) & 0x10 which is 0x0000, wtf
        //   [3431]:2 <- [3433 + si]:2, but [3433] is init'd to 0xffff
        //   [342f]:1 <- [3430], which is 0x00
        invert_3431 = 0xffff;
        // run_opening_titles, which we already did
        // erase_video_buffer():
        //   set the frob for segment_idx[4d33] to 0x02
        //     that was init'd to 0xffff, but now is 0xb9c0, i'm just not sure HOW
        //     also this doesn't have any effect because there's no segment loaded?
        app.drawGameplayCorners();
        // 0x377c <- 0x00

        this.cs = getSegmentForChunk(initialChunk, Frob.CLEAN);
        this.ip = initialAddr;
        this.ds = this.cs;
        drawString313e();
        setBBox(0x01, 0x27, 0x08, 0xb8);
        draw_borders = 0xff;
        drawStringAndResetBBox();
        setInvertChar(0x00);
    }

    public void start() {
        System.arraycopy(INITIAL_VALUES_d1b0, 0, this.bufferD1B0, 0, INITIAL_VALUES_d1b0.length);
        this.instructionsExecuted = 0;

        Address nextIP = new Address(this.cs, this.ip);
        while (Objects.nonNull(nextIP)) {
            this.cs = nextIP.segment();
            this.ip = nextIP.offset();
            final int opcode = readByte(nextIP);
            System.out.format("%02x %08x %02x\n", cs, ip, opcode);
            final Instruction ins = decodeOpcode(opcode);
            nextIP = ins.exec(this);
            this.instructionsExecuted++;
        }
    }

    public int instructionsExecuted() {
        return this.instructionsExecuted;
    }

    /**
     * Examines the segment table to translate a chunk ID into a segment ID. Note that this may have the side effect
     * of loading the requested chunk into a new segment, if it doesn't exist.
     * @param chunkId The desired chunk ID
     * @param frob The desired new value of the segment's frob
     * @return The segment ID of the (new) segment
     */
    public int getSegmentForChunk(int chunkId, Frob frob) {
        int segmentId = chunkIds.indexOf(chunkId);
        if (segmentId == -1 || (chunkId & 0x8000) > 0) {
            final ModifiableChunk newChunk = new ModifiableChunk(allChunks.get(chunkId));
            final int firstFreeSegment = chunkFrobs.indexOf(0x00);
            if (firstFreeSegment == -1) {
                segmentId = segments.size();
                segments.add(newChunk);
                chunkFrobs.add(frob.value());
                chunkIds.add(chunkId);
                chunkSizes.add(newChunk.getSize());
            } else {
                segmentId = firstFreeSegment;
                segments.set(segmentId, newChunk);
                chunkFrobs.set(segmentId, frob.value());
                chunkIds.set(segmentId, chunkId);
                chunkSizes.set(segmentId, newChunk.getSize());
            }
        }
        // should there be a "don't overwrite frob 0xff" guard here?
        chunkFrobs.set(segmentId, frob.value());
        return segmentId;
    }

    public Frob getFrob(int segmentId) {
        final int value = chunkFrobs.get(segmentId);
        return Frob.of(value);
    }

    public void setFrob(int segmentId, Frob frob) {
        chunkFrobs.set(segmentId, frob.value());
    }

    public void unloadSegment(int segmentId) {
        if (chunkFrobs.get(segmentId) != 0xff) {
            chunkFrobs.set(segmentId, 0x00);
        }
    }

    public void setBBox(int x0, int x1, int y0, int y1) {
        bbox_x0 = x0;
        bbox_y0 = y0;
        bbox_x1 = x1;
        bbox_y1 = y1;
    }

    public void resetBBox() {
        draw_borders = 0x00;
        setBBox(0x01, 0x27, 0x98, 0xb8);
        x_31ed = 0x01;
        y_31ef = 0x98;
    }

    public void expandBBox() {
        bbox_y1 = bbox_y1 + 8;
        bbox_y0 = bbox_y0 - 8;
        bbox_x1 = bbox_x1 + 1;
        bbox_x0 = bbox_x0 - 1;
    }

    public void shrinkBBox() {
        bbox_y1 = bbox_y1 - 8;
        bbox_y0 = bbox_y0 + 8;
        bbox_x1 = bbox_x1 - 1;
        bbox_x0 = bbox_x0 + 1;
    }

    public boolean getCarryFlag() {
        return flagCarry;
    }

    public void setCarryFlag(boolean flag) {
        this.flagCarry = flag;
    }

    public boolean getZeroFlag() {
        return flagZero;
    }

    public void setZeroFlag(boolean flag) {
        this.flagZero = flag;
    }

    public boolean getSignFlag() {
        return flagSign;
    }

    public void setSignFlag(boolean flag) {
        this.flagSign = flag;
    }

    public Chunk getSegment(int index) {
        return segments.get(index);
    }

    public Address getIP() {
        return new Address(this.cs, this.ip);
    }

    public int getAL() {
        return this.ax & MASK_LOW;
    }

    public int getAX(boolean forceWide) {
        return (forceWide || width) ? this.ax & MASK_WORD : this.ax & MASK_LOW;
    }

    public int getAX() {
        return getAX(false);
    }

    public void setAL(int val) {
        this.ax = (this.ax & MASK_HIGH) | (val & MASK_LOW);
    }

    public void setAH(int val) {
        this.ax = ((val & 0x000000ff) << 8) | (this.ax & MASK_LOW);
    }

    public void setAX(int val, boolean forceWide) {
        if (forceWide || width) {
            this.ax = val & MASK_WORD;
        } else {
            setAL(val);
        }
    }

    /** Writes the value to the AX register. In WIDE mode this method writes both AL and AH; in NARROW mode it only
     * writes AL, and AH is untouched. */
    public void setAX(int val) {
        setAX(val, false);
    }

    public int getBL() {
        return this.bx & MASK_LOW;
    }

    public int getBX(boolean forceWide) {
        return (forceWide || width) ? this.bx & MASK_WORD : this.bx & MASK_LOW;
    }

    public int getBX() {
        return getBX(false);
    }

    public void setBL(int val) {
        this.bx = (this.bx & MASK_HIGH) | (val & MASK_LOW);
    }

    public void setBH(int val) {
        this.bx = ((val & 0x000000ff) << 8) | (this.bx & MASK_LOW);
    }

    public void setBX(int val) {
        if (width) {
            this.bx = val & MASK_WORD;
        } else {
            setBL(val);
        }
    }

    public int getCS() {
        return this.cs;
    }

    public int getDS() {
        return this.ds;
    }

    public void setDS(int val) {
        System.out.format("  ds <- 0x%02x\n", val);
        this.ds = val;
    }

    public void push(int val) {
        this.stack.push(intToByte(val));
    }

    public int pop() {
        return byteToInt(this.stack.pop());
    }

    public void setHeap(int index, int val) {
        this.heap[index & MASK_LOW] = val & MASK_LOW;
        if (width) {
            this.heap[(index & MASK_LOW) + 1] = (val & MASK_HIGH) >> 8;
        }
    }

    public void setHeapBytes(int index, int count, int val) {
        for (int i = 0; i < count; i++) {
            this.heap[(index + i) & MASK_LOW] = val & MASK_LOW;
            val = val >> 8;
        }
    }

    public int getHeap(int index) {
        return getHeapBytes(index, (width) ? 2 : 1);
    }

    public int getHeapBytes(int index, int count) {
        int value = 0;
        for (int i = count - 1; i >= 0; i--) {
            value = value << 8;
            value = value | (this.heap[(index & MASK_LOW) + i] & MASK_LOW);
        }
        return value;
    }

    public boolean isWide() {
        return width;
    }

    public void setWidth(boolean width) {
        this.width = width;
    }

    /** Retrieves a single byte from segment data. */
    public int readByte(int segmentId, int offset) {
        return Objects.requireNonNull(segments.get(segmentId)).getUnsignedByte(offset);
    }

    /** Retrieves a single byte from segment data. */
    public int readByte(Address addr) {
        return readByte(addr.segment(), addr.offset());
    }

    /** Retrieves a word (two bytes) from segment data. */
    public int readWord(int segmentId, int offset) {
        return Objects.requireNonNull(segments.get(segmentId)).getWord(offset);
    }

    /** Retrieves a word (two bytes) from segment data. */
    public int readWord(Address addr) {
        return readWord(addr.segment(), addr.offset());
    }

    public int readData(int segmentId, int offset, int size) {
        return Objects.requireNonNull(segments.get(segmentId)).getData(offset, size);
    }

    public int readData(Address addr, int size) {
        return readData(addr.segment(), addr.offset(), size);
    }

    public void writeByte(int segmentId, int offset, int value) {
        final ModifiableChunk c = Objects.requireNonNull(segments.get(segmentId));
        c.setByte(offset, value);
    }

    public void writeByte(Address addr, int value) {
        writeByte(addr.segment(), addr.offset(), value);
    }
    
    public void writeWord(int segmentId, int offset, int value) {
        final ModifiableChunk c = Objects.requireNonNull(segments.get(segmentId));
        c.setWord(offset, value);
    }

    public void writeWord(Address addr, int value) {
        writeWord(addr.segment(), addr.offset(), value);
    }

    public void writeWidth(int segmentId, int addr, int value) {
        if (isWide()) {
            writeWord(segmentId, addr, value);
        } else {
            writeByte(segmentId, addr, value);
        }
    }

    public int readBufferD1B0(int offset) {
        return byteToInt(this.bufferD1B0[offset]);
    }

    public void writeBufferD1B0(int offset, int value) {
        this.bufferD1B0[offset] = intToByte(value);
    }

    /**
     * Draws a character on the screen. Character indices are looked up on the array at 0xb9a2, which contains a set of
     * eight bytes representing an 8x8 grid of pixels.
     * @param index The character index; ASCII codes generally do what you think.
     * @param x Screen coordinate in pixels.
     * @param y Screen coordinate in pixels.
     * @param invert If true, draws white-on-black instead of black-on-white.
     */
    public void drawChar(int index, int x, int y, boolean invert) {
        final byte[] bitmask = new byte[8];
        System.arraycopy(INITIAL_VALUES_b9a2, (index & 0x7f) * 8, bitmask, 0, 8);
        app.drawBitmask(bitmask, x, y, invert);
    }

    public void drawStringAndResetBBox() {
        drawString313e();
        if (draw_borders != 0x00) {
            drawHudBorders();
        }
        resetBBox();
    }

    public void drawString313e() {
        for (int i = 0; i < strlen_313c; i++) {
            drawChar(string_313e.get(i), x_31ed * 8, y_31ef, false);
            x_31ed += 8;
        }
        strlen_313c = 0;
        x_3166 = x_31ed;
    }

    public void fillRectangle() {
        app.drawRectangle(invert_3431, bbox_x0, bbox_y0, bbox_x1, bbox_y1);
    }

    public void setCharCoordinates(int x, int y) {
        this.x_31ed = x;
        this.y_31ef = y;
    }

    public void setInvertChar(int invert) {
        this.invert_3431 = invert;
    }

    public void drawString(List<Integer> s) {
        for (int ch : s) {
            drawChar(ch, (x_31ed) * 8, (y_31ef), invert_3431 != 0);
            x_31ed += 1;
        }
    }

    public void drawString(String s, int x, int y, boolean invert) {
        int fx = x * 8;
        int fy = y * 8;
        for (char ch : s.toCharArray()) {
            drawChar(ch, fx, fy, invert);
            fx += 8;
        }
    }

    private boolean forceBoundsCheck(int regionId) {
        expandBBox();
        final Rectangle region = HUD_REGIONS_2644.get(regionId);
        if (region.x0() >= bbox_x1) { shrinkBBox(); return false; }
        if (region.y0() >= bbox_y1) { shrinkBBox(); return false; }
        if (bbox_x0 >= region.x1()) { shrinkBBox(); return false; }
        if (bbox_y0 >= region.y1()) { shrinkBBox(); return false; }
        shrinkBBox();
        return true;
    }

    public void drawHudBorders() {
        draw_borders = 0x00;
        int regionId = 0;
        if (forceBoundsCheck(regionId)) {
            switch(regionId) {
                case 0x09 -> {}
                case 0x0a -> {}
                case 0x0b -> {}
                case 0x0c -> {}
                case 0x0d -> {}
                default -> {

                }
            }
        }
    }

    /**
     * Draws an empty box on the screen.
     */
    public void drawModal(Address addr) {
        final int x0 = readByte(addr) * 8; // ch adr -> pix adr
        final int y0 = readByte(addr.incr(1)); // this is already a pix adr
        final int x1 = readByte(addr.incr(2)) * 8;
        final int y1 = readByte(addr.incr(3));
        boolean invert = invert_3431 != 0;

        // four immediates: 16 00 28 98 (combat window)
        // written as words to 0x253f/tmp
        // draw_string_313e (to empty buffer??)
        // if 0x253e/draw_borders > 0 {           <- come back to this
        //   expand bounding box()
        //   do some bounds checking?...   ???
        //   shrink bounding box()
        //   draw_hud_borders()
        // }
        // copy 0x253f/tmp to 0x2547/bbox
        // copy 0x2547/bbox/x0,y0 to 0x31ed/x0,y0
        // al <- 0x80 // top-left double border char
        // draw_modal_border() {
        //   print al
        //   al++
        //   for (i=0x31ed/x0; i<0x2547/x1; i++) print al
        //   al++
        //   print al
        // }
        // draw vertical edges
        // draw bottom border
        // shrink bbox
        // call fill_rectangle() and return

        int x;
        int y = y0;
        for (x = x0; x < x1; x += 8) {
            drawChar(0x01, x, y, invert);
        }
        drawChar(0x00, x0, y, invert);
        drawChar(0x02, x1 - 8, y, invert);

        y += 8;
        while (y < y1) {
            for (x = x0; x < x1; x += 8) {
                drawChar(0x7f, x, y, invert);
            }
            drawChar(0x03, x0, y, invert);
            drawChar(0x04, x1 - 8, y, invert);
            y += 8;
        }

        for (x = x0; x < x1; x += 8) {
            drawChar(0x06, x, y, invert);
        }
        drawChar(0x05, x0, y, invert);
        drawChar(0x07, x1 - 8, y, invert);

        shrinkBBox();
    }

    private int byteToInt(byte b) {
        return MASK_LOW & ((int) b);
    }

    private byte intToByte(int i) {
        return (byte)(i & MASK_LOW);
    }

    private Instruction decodeOpcode(int opcode) {
        return switch (opcode) {
            case 0x00 -> Instructions.SET_WIDE;
            case 0x01 -> Instructions.SET_NARROW;
            case 0x02 -> Instructions.PUSH_DS;
            case 0x03 -> Instructions.POP_DS;
            case 0x04 -> Instructions.PUSH_CS;
            case 0x05 -> new LoadBLHeap();
            case 0x06 -> new LoadBLImm();
            case 0x07 -> new LoadBLZero();
            case 0x08 -> new StoreBLHeap();
            case 0x09 -> new LoadAXImm();
            case 0x0a -> new LoadAXHeap();
            case 0x0b -> new LoadAXHeapOffset();
            case 0x0c -> new LoadAX();
            case 0x0d -> new LoadAXOffset();
            case 0x0e -> new LoadAXIndirect();
            case 0x0f -> new LoadAXLongPtr();
            case 0x10 -> new LoadAXIndirectImm();
            case 0x11 -> new StoreZeroHeap();
            case 0x12 -> new StoreAXHeap();
            case 0x13 -> new StoreAXHeapOffset();
            case 0x14 -> new StoreAX();
            case 0x15 -> new StoreAXOffset();
            case 0x16 -> new StoreAXIndirect();
            case 0x17 -> new StoreAXLongPtr();
            case 0x18 -> new StoreAXIndirectImm();
            case 0x19 -> new MoveHeap();
            case 0x1a -> new StoreImmHeap();
            case 0x1b -> new MoveData();
            case 0x1c -> new StoreImm();
            case 0x1d -> new BufferCopy();
            case 0x1e -> Instructions.EXIT; // "kill executable" aka "you lost"
            case 0x1f -> Instructions.NOOP; // "read segment table"
            //   0x20 sends the (real) IP to 0x0000, which is probably a segfault
            case 0x21 -> new MoveALBL();
            case 0x22 -> new MoveBXAX();
            case 0x23 -> new IncHeap();
            case 0x24 -> new IncAX();
            case 0x25 -> new IncBL();
            case 0x26 -> new DecHeap();
            case 0x27 -> new DecAX();
            case 0x28 -> new DecBL();
            case 0x29 -> new LeftShiftHeap();
            case 0x2a -> new LeftShiftAX();
            case 0x2b -> new LeftShiftBL();
            case 0x2c -> new RightShiftHeap();
            case 0x2d -> new RightShiftAX();
            case 0x2e -> new RightShiftBL();
            case 0x2f -> new AddAXHeap();
            case 0x30 -> new AddAXImm();
            case 0x31 -> new SubAXHeap();
            case 0x32 -> new SubAXImm();
            case 0x33 -> new MulAXHeap();
            case 0x34 -> new MulAXImm();
            case 0x35 -> new DivAXHeap();
            case 0x36 -> new DivAXImm();
            case 0x37 -> new AndAXHeap();
            case 0x38 -> new AndAXImm();
            case 0x39 -> new OrAXHeap();
            case 0x3a -> new OrAXImm();
            case 0x3b -> new XorAXHeap();
            case 0x3c -> new XorAXImm();
            case 0x3d -> new CmpAXHeap();
            case 0x3e -> new CmpAXImm();
            case 0x3f -> new CmpBLHeap();
            case 0x40 -> new CmpBLImm();
            // The SUB and CMP instructions flip the carry bit before writing, which makes
            // JC and JNC behave in the opposite manner. But ADD doesn't flip carry.
            case 0x41 -> new JumpIf((i) -> ! i.getCarryFlag());
            case 0x42 -> new JumpIf((i) -> i.getCarryFlag());
            case 0x43 -> new JumpIf((i) -> i.getCarryFlag() & ! i.getZeroFlag()); // "above"
            case 0x44 -> new JumpIf((i) -> i.getZeroFlag()); // "equal"
            case 0x45 -> new JumpIf((i) -> ! i.getZeroFlag()); // "not equal"
            case 0x46 -> new JumpIf((i) -> i.getSignFlag());
            case 0x47 -> new JumpIf((i) -> ! i.getSignFlag());
            case 0x48 -> new TestAndSetHeapSign();
            case 0x49 -> new LoopBX();
            case 0x4a -> new LoopBXLimit();
            case 0x4b -> Instructions.SET_CARRY;
            case 0x4c -> Instructions.CLEAR_CARRY;
            case 0x4d -> new RandomAX();
            case 0x4e -> new FlagSetAL();
            case 0x4f -> new FlagClearAL();
            case 0x50 -> new FlagTestAL();
            case 0x51 -> new ArrayMax();
            case 0x52 -> new JumpIf((i) -> true);
            case 0x53 -> new Call();
            case 0x54 -> new Return();
            case 0x55 -> new PopAX();
            case 0x56 -> new PushAX();
            case 0x57 -> new LongJump();
            case 0x58 -> new LongCall();
            case 0x59 -> new LongReturn();
            case 0x5a -> Instructions.EXIT; // "stop executing instruction stream"
            // case 0x5b -> new EraseSquareSpecial();
            // case 0x5c -> new RecurseOverParty();
            // case 0x5d -> new LoadAXPartyAttribute();
            // case 0x5e -> new StoreAXPartyAttribute();
            // case 0x5f -> new SetPartyFlag();
            // case 0x60 -> new ClearPartyFlag();
            // case 0x61 -> new TestPartyFlag();
            // case 0x62 -> new SearchPartyFlag();
            // case 0x63 -> new RecurseOverInventory();
            // case 0x64 -> new PickUpItem();
            // case 0x65 -> new SearchItem();
            case 0x66 -> new TestHeap();
            // case 0x67 -> new DropItem();
            // case 0x68 -> new ReadInventory();
            // case 0x69 -> new WriteInventory();
            case 0x6a -> new IsPartyInBox();
            // case 0x6b -> new RunAway();
            // case 0x6c -> new StepForward();
            // case 0x6d -> new DrawAutomap();
            // case 0x6e -> new DrawCompass();
            // case 0x6f -> new RotateMapView();
            // case 0x70 -> new UnrotateMapView();
            // case 0x71 -> new RunSpecialEvent();
            // case 0x72 -> new UseItem();
            case 0x73 -> Instructions.COPY_HEAP_3E_3F;
            case 0x74 -> new DrawModal();
            case 0x75 -> (i) -> { i.drawStringAndResetBBox(); return i.getIP().incr(); };
            case 0x76 -> Instructions.FILL_BBOX;
            case 0x77 -> (i) -> Instructions.compose(i, Instructions.FILL_BBOX, new DecodeStringCS());
            case 0x78 -> new DecodeStringCS();
            case 0x79 -> (i) -> Instructions.compose(i, Instructions.FILL_BBOX, new DecodeStringDS());
            case 0x7a -> new DecodeStringDS();
            case 0x7b -> new DecodeTitleStringCS();
            case 0x7c -> new DecodeTitleStringDS();
            // case 0x7d -> new IndirectCharName();
            // case 0x7e -> new IndirectCharItem();
            // case 0x7f -> new IndirectString();
            // case 0x80 -> new Indent();
            // case 0x81 -> new PrintAX4d(); // 4-digit number
            // case 0x82 -> new PrintHeap9d(); // 9-digit number
            // case 0x83 -> new IndirectChar();
            // case 0x84 -> new AllocateSegment();
            case 0x85 -> new FreeSegmentAL();
            case 0x86 -> new LoadChunkAX();
            // case 0x87 -> new PersistChunk();
            // case 0x88 -> new WaitForEscapeKey();
            // case 0x89 -> new ReadKeySwitch();
            // case 0x8a -> new ShowMonsterImage();
            // case 0x8b -> new DrawCurrentViewport();
            // case 0x8c -> new ShowYesNoModal();
            // case 0x8d -> new PromptAndReadInput();
            case 0x8e -> Instructions.NOOP;
            case 0x8f -> new StrToInt();
            // case 0x90 -> new PlaySoundEffect();
            // case 0x91 -> new PauseUntilKey();
            // case 0x92 -> new PauseUntilKeyOrTime();
            case 0x93 -> new PushBL();
            case 0x94 -> new PopBL();
            // case 0x95 -> new SetCursor();
            // case 0x96 -> new EraseLine();
            // case 0x97 -> new LoadAXPartyField();
            // case 0x98 -> new StoreAXPartyField();
            case 0x99 -> new TestAX();
            case 0x9a -> new LoadHeapOnes();
            case 0x9b -> new FlagSetImm();
            case 0x9c -> new FlagClearImm();
            case 0x9d -> new FlagTestImm();
            case 0x9e -> new GetSegmentSize();
            // case 0x9f -> new YouWin();
            default -> throw new IllegalArgumentException("Unknown opcode " + opcode);
        };
    }

    public record Rectangle(int x0, int x1, int y0, int y1) {};

    private static final List<Rectangle> HUD_REGIONS_2644 = List.of(
            new Rectangle(0x00, 0x28, 0xb8, 0xc0), // text area, bottom border
            new Rectangle(0x00, 0x01, 0x98, 0xb8), // text area, left border
            new Rectangle(0x27, 0x28, 0x98, 0xb8), // text area, right border
            new Rectangle(0x00, 0x28, 0x90, 0x98), // text area, top border
            new Rectangle(0x27, 0x28, 0x00, 0x90), // combat area, right border
            new Rectangle(0x1b, 0x27, 0x00, 0x20), // combat area, top border
            new Rectangle(0x00, 0x02, 0x00, 0x90), // viewport, left border
            new Rectangle(0x02, 0x04, 0x00, 0x08), // viewport, top-left
            new Rectangle(0x14, 0x16, 0x00, 0x08), // viewport, top-right
            new Rectangle(0x16, 0x1b, 0x00, 0x90), // compass bar
            new Rectangle(0x02, 0x16, 0x08, 0x90), // viewport
            new Rectangle(0x1b, 0x27, 0x08, 0x78), // combat area
            new Rectangle(0x04, 0x14, 0x00, 0x08), // title bar (viewport top-center)
            new Rectangle(0x01, 0x27, 0x98, 0xb8)  // text area
    );

    // Character bitmaps
    private static final byte[] INITIAL_VALUES_b9a2 = {
            (byte)0xff, (byte)0xff, (byte)0xc0, (byte)0xc0, (byte)0xcf, (byte)0xcf, (byte)0xcc, (byte)0xcc,
            (byte)0xff, (byte)0xff, (byte)0x00, (byte)0x00, (byte)0xff, (byte)0xff, (byte)0x00, (byte)0x00,
            (byte)0xff, (byte)0xff, (byte)0x03, (byte)0x03, (byte)0xf3, (byte)0xf3, (byte)0x33, (byte)0x33,
            (byte)0xcc, (byte)0xcc, (byte)0xcc, (byte)0xcc, (byte)0xcc, (byte)0xcc, (byte)0xcc, (byte)0xcc,
            (byte)0x33, (byte)0x33, (byte)0x33, (byte)0x33, (byte)0x33, (byte)0x33, (byte)0x33, (byte)0x33,
            (byte)0xcc, (byte)0xcc, (byte)0xcf, (byte)0xcf, (byte)0xc0, (byte)0xc0, (byte)0xff, (byte)0xff,
            (byte)0x00, (byte)0x00, (byte)0xff, (byte)0xff, (byte)0x00, (byte)0x00, (byte)0xff, (byte)0xff,
            (byte)0x33, (byte)0x33, (byte)0xf3, (byte)0xf3, (byte)0x03, (byte)0x03, (byte)0xff, (byte)0xff,
            (byte)0x55, (byte)0xaa, (byte)0xaa, (byte)0xaa, (byte)0xaa, (byte)0xaa, (byte)0xaa, (byte)0xff,
            (byte)0x55, (byte)0x9e, (byte)0x9e, (byte)0x9e, (byte)0x9e, (byte)0x9e, (byte)0x9e, (byte)0xff,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0xff, (byte)0xc0, (byte)0xc0, (byte)0xc0, (byte)0xc0, (byte)0xc0, (byte)0xc0, (byte)0xc0,
            (byte)0xff, (byte)0x03, (byte)0x03, (byte)0x03, (byte)0x03, (byte)0x03, (byte)0x03, (byte)0x03,
            (byte)0xc0, (byte)0xc0, (byte)0xc0, (byte)0xc0, (byte)0xc0, (byte)0xc0, (byte)0xc0, (byte)0xff,
            (byte)0x03, (byte)0x03, (byte)0x03, (byte)0x03, (byte)0x03, (byte)0x03, (byte)0x03, (byte)0xff,
            (byte)0xc0, (byte)0xc0, (byte)0xc0, (byte)0xc0, (byte)0xc0, (byte)0xc0, (byte)0xc0, (byte)0xc0,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x3f,
            (byte)0x3f, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x30, (byte)0x30, (byte)0x30, (byte)0x30, (byte)0x30, (byte)0x30, (byte)0x30, (byte)0x30,
            (byte)0xc0, (byte)0xc1, (byte)0xc3, (byte)0xc7, (byte)0xc7, (byte)0xc1, (byte)0xc1, (byte)0xc0,
            (byte)0x03, (byte)0xc3, (byte)0xe3, (byte)0xf3, (byte)0xf3, (byte)0xc3, (byte)0xc3, (byte)0x03,
            (byte)0xc0, (byte)0xc1, (byte)0xc1, (byte)0xc7, (byte)0xc7, (byte)0xc3, (byte)0xc1, (byte)0xc0,
            (byte)0x03, (byte)0xc3, (byte)0xc3, (byte)0xf3, (byte)0xf3, (byte)0xe3, (byte)0xc3, (byte)0x03,
            (byte)0x03, (byte)0x03, (byte)0x03, (byte)0x03, (byte)0x03, (byte)0x03, (byte)0x03, (byte)0x03,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0xff,
            (byte)0xff, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x30, (byte)0x78, (byte)0x78, (byte)0x30, (byte)0x30, (byte)0x00, (byte)0x30, (byte)0x00,
            (byte)0x6c, (byte)0x6c, (byte)0x6c, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x6c, (byte)0x6c, (byte)0xfe, (byte)0x6c, (byte)0xfe, (byte)0x6c, (byte)0x6c, (byte)0x00,
            (byte)0x30, (byte)0x7c, (byte)0xc0, (byte)0x78, (byte)0x0c, (byte)0xf8, (byte)0x30, (byte)0x00,
            (byte)0x00, (byte)0xc6, (byte)0xcc, (byte)0x18, (byte)0x30, (byte)0x66, (byte)0xc6, (byte)0x00,
            (byte)0x38, (byte)0x6c, (byte)0x38, (byte)0x76, (byte)0xdc, (byte)0xcc, (byte)0x76, (byte)0x00,
            (byte)0x60, (byte)0x60, (byte)0xc0, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x18, (byte)0x30, (byte)0x60, (byte)0x60, (byte)0x60, (byte)0x30, (byte)0x18, (byte)0x00,
            (byte)0x60, (byte)0x30, (byte)0x18, (byte)0x18, (byte)0x18, (byte)0x30, (byte)0x60, (byte)0x00,
            (byte)0x00, (byte)0x66, (byte)0x3c, (byte)0xff, (byte)0x3c, (byte)0x66, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x30, (byte)0x30, (byte)0xfc, (byte)0x30, (byte)0x30, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x30, (byte)0x30, (byte)0x60,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0xfc, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x30, (byte)0x30, (byte)0x00,
            (byte)0x06, (byte)0x0c, (byte)0x18, (byte)0x30, (byte)0x60, (byte)0xc0, (byte)0x80, (byte)0x00,
            (byte)0x7c, (byte)0xc6, (byte)0xce, (byte)0xde, (byte)0xf6, (byte)0xe6, (byte)0x7c, (byte)0x00,
            (byte)0x30, (byte)0x70, (byte)0x30, (byte)0x30, (byte)0x30, (byte)0x30, (byte)0xfc, (byte)0x00,
            (byte)0x78, (byte)0xcc, (byte)0x0c, (byte)0x38, (byte)0x60, (byte)0xcc, (byte)0xfc, (byte)0x00,
            (byte)0x78, (byte)0xcc, (byte)0x0c, (byte)0x38, (byte)0x0c, (byte)0xcc, (byte)0x78, (byte)0x00,
            (byte)0x1c, (byte)0x3c, (byte)0x6c, (byte)0xcc, (byte)0xfe, (byte)0x0c, (byte)0x1e, (byte)0x00,
            (byte)0xfc, (byte)0xc0, (byte)0xf8, (byte)0x0c, (byte)0x0c, (byte)0xcc, (byte)0x78, (byte)0x00,
            (byte)0x38, (byte)0x60, (byte)0xc0, (byte)0xf8, (byte)0xcc, (byte)0xcc, (byte)0x78, (byte)0x00,
            (byte)0xfc, (byte)0xcc, (byte)0x0c, (byte)0x18, (byte)0x30, (byte)0x30, (byte)0x30, (byte)0x00,
            (byte)0x78, (byte)0xcc, (byte)0xcc, (byte)0x78, (byte)0xcc, (byte)0xcc, (byte)0x78, (byte)0x00,
            (byte)0x78, (byte)0xcc, (byte)0xcc, (byte)0x7c, (byte)0x0c, (byte)0x18, (byte)0x70, (byte)0x00,
            (byte)0x00, (byte)0x30, (byte)0x30, (byte)0x00, (byte)0x00, (byte)0x30, (byte)0x30, (byte)0x00,
            (byte)0x00, (byte)0x30, (byte)0x30, (byte)0x00, (byte)0x00, (byte)0x30, (byte)0x30, (byte)0x60,
            (byte)0x18, (byte)0x30, (byte)0x60, (byte)0xc0, (byte)0x60, (byte)0x30, (byte)0x18, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0xfc, (byte)0x00, (byte)0x00, (byte)0xfc, (byte)0x00, (byte)0x00,
            (byte)0x60, (byte)0x30, (byte)0x18, (byte)0x0c, (byte)0x18, (byte)0x30, (byte)0x60, (byte)0x00,
            (byte)0x78, (byte)0xcc, (byte)0x0c, (byte)0x18, (byte)0x30, (byte)0x00, (byte)0x30, (byte)0x00,
            (byte)0x7c, (byte)0xc6, (byte)0xde, (byte)0xde, (byte)0xde, (byte)0xc0, (byte)0x78, (byte)0x00,
            (byte)0x30, (byte)0x78, (byte)0xcc, (byte)0xcc, (byte)0xfc, (byte)0xcc, (byte)0xcc, (byte)0x00,
            (byte)0xfc, (byte)0x66, (byte)0x66, (byte)0x7c, (byte)0x66, (byte)0x66, (byte)0xfc, (byte)0x00,
            (byte)0x3c, (byte)0x66, (byte)0xc0, (byte)0xc0, (byte)0xc0, (byte)0x66, (byte)0x3c, (byte)0x00,
            (byte)0xf8, (byte)0x6c, (byte)0x66, (byte)0x66, (byte)0x66, (byte)0x6c, (byte)0xf8, (byte)0x00,
            (byte)0xfe, (byte)0x62, (byte)0x68, (byte)0x78, (byte)0x68, (byte)0x62, (byte)0xfe, (byte)0x00,
            (byte)0xfe, (byte)0x62, (byte)0x68, (byte)0x78, (byte)0x68, (byte)0x60, (byte)0xf0, (byte)0x00,
            (byte)0x3c, (byte)0x66, (byte)0xc0, (byte)0xc0, (byte)0xce, (byte)0x66, (byte)0x3e, (byte)0x00,
            (byte)0xcc, (byte)0xcc, (byte)0xcc, (byte)0xfc, (byte)0xcc, (byte)0xcc, (byte)0xcc, (byte)0x00,
            (byte)0x78, (byte)0x30, (byte)0x30, (byte)0x30, (byte)0x30, (byte)0x30, (byte)0x78, (byte)0x00,
            (byte)0x1e, (byte)0x0c, (byte)0x0c, (byte)0x0c, (byte)0xcc, (byte)0xcc, (byte)0x78, (byte)0x00,
            (byte)0xe6, (byte)0x66, (byte)0x6c, (byte)0x78, (byte)0x6c, (byte)0x66, (byte)0xe6, (byte)0x00,
            (byte)0xf0, (byte)0x60, (byte)0x60, (byte)0x60, (byte)0x62, (byte)0x66, (byte)0xfe, (byte)0x00,
            (byte)0xc6, (byte)0xee, (byte)0xfe, (byte)0xfe, (byte)0xd6, (byte)0xc6, (byte)0xc6, (byte)0x00,
            (byte)0xc6, (byte)0xe6, (byte)0xf6, (byte)0xde, (byte)0xce, (byte)0xc6, (byte)0xc6, (byte)0x00,
            (byte)0x38, (byte)0x6c, (byte)0xc6, (byte)0xc6, (byte)0xc6, (byte)0x6c, (byte)0x38, (byte)0x00,
            (byte)0xfc, (byte)0x66, (byte)0x66, (byte)0x7c, (byte)0x60, (byte)0x60, (byte)0xf0, (byte)0x00,
            (byte)0x78, (byte)0xcc, (byte)0xcc, (byte)0xcc, (byte)0xdc, (byte)0x78, (byte)0x1c, (byte)0x00,
            (byte)0xfc, (byte)0x66, (byte)0x66, (byte)0x7c, (byte)0x6c, (byte)0x66, (byte)0xe6, (byte)0x00,
            (byte)0x78, (byte)0xcc, (byte)0xe0, (byte)0x70, (byte)0x1c, (byte)0xcc, (byte)0x78, (byte)0x00,
            (byte)0xfc, (byte)0xb4, (byte)0x30, (byte)0x30, (byte)0x30, (byte)0x30, (byte)0x78, (byte)0x00,
            (byte)0xcc, (byte)0xcc, (byte)0xcc, (byte)0xcc, (byte)0xcc, (byte)0xcc, (byte)0xfc, (byte)0x00,
            (byte)0xcc, (byte)0xcc, (byte)0xcc, (byte)0xcc, (byte)0xcc, (byte)0x78, (byte)0x30, (byte)0x00,
            (byte)0xc6, (byte)0xc6, (byte)0xc6, (byte)0xd6, (byte)0xfe, (byte)0xee, (byte)0xc6, (byte)0x00,
            (byte)0xc6, (byte)0xc6, (byte)0x6c, (byte)0x38, (byte)0x38, (byte)0x6c, (byte)0xc6, (byte)0x00,
            (byte)0xcc, (byte)0xcc, (byte)0xcc, (byte)0x78, (byte)0x30, (byte)0x30, (byte)0x78, (byte)0x00,
            (byte)0xfe, (byte)0xc6, (byte)0x8c, (byte)0x18, (byte)0x32, (byte)0x66, (byte)0xfe, (byte)0x00,
            (byte)0x78, (byte)0x60, (byte)0x60, (byte)0x60, (byte)0x60, (byte)0x60, (byte)0x78, (byte)0x00,
            (byte)0xc0, (byte)0x60, (byte)0x30, (byte)0x18, (byte)0x0c, (byte)0x06, (byte)0x02, (byte)0x00,
            (byte)0x78, (byte)0x18, (byte)0x18, (byte)0x18, (byte)0x18, (byte)0x18, (byte)0x78, (byte)0x00,
            (byte)0x10, (byte)0x38, (byte)0x6c, (byte)0xc6, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0xff,
            (byte)0x30, (byte)0x30, (byte)0x18, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x78, (byte)0x0c, (byte)0x7c, (byte)0xcc, (byte)0x76, (byte)0x00,
            (byte)0xe0, (byte)0x60, (byte)0x60, (byte)0x7c, (byte)0x66, (byte)0x66, (byte)0xdc, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x78, (byte)0xcc, (byte)0xc0, (byte)0xcc, (byte)0x78, (byte)0x00,
            (byte)0x1c, (byte)0x0c, (byte)0x0c, (byte)0x7c, (byte)0xcc, (byte)0xcc, (byte)0x76, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x78, (byte)0xcc, (byte)0xfc, (byte)0xc0, (byte)0x78, (byte)0x00,
            (byte)0x38, (byte)0x6c, (byte)0x60, (byte)0xf0, (byte)0x60, (byte)0x60, (byte)0xf0, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x76, (byte)0xcc, (byte)0xcc, (byte)0x7c, (byte)0x0c, (byte)0xf8,
            (byte)0xe0, (byte)0x60, (byte)0x6c, (byte)0x76, (byte)0x66, (byte)0x66, (byte)0xe6, (byte)0x00,
            (byte)0x30, (byte)0x00, (byte)0x70, (byte)0x30, (byte)0x30, (byte)0x30, (byte)0x78, (byte)0x00,
            (byte)0x0c, (byte)0x00, (byte)0x0c, (byte)0x0c, (byte)0x0c, (byte)0xcc, (byte)0xcc, (byte)0x78,
            (byte)0xe0, (byte)0x60, (byte)0x66, (byte)0x6c, (byte)0x78, (byte)0x6c, (byte)0xe6, (byte)0x00,
            (byte)0x70, (byte)0x30, (byte)0x30, (byte)0x30, (byte)0x30, (byte)0x30, (byte)0x78, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0xcc, (byte)0xfe, (byte)0xfe, (byte)0xd6, (byte)0xc6, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0xf8, (byte)0xcc, (byte)0xcc, (byte)0xcc, (byte)0xcc, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x78, (byte)0xcc, (byte)0xcc, (byte)0xcc, (byte)0x78, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0xdc, (byte)0x66, (byte)0x66, (byte)0x7c, (byte)0x60, (byte)0xf0,
            (byte)0x00, (byte)0x00, (byte)0x76, (byte)0xcc, (byte)0xcc, (byte)0x7c, (byte)0x0c, (byte)0x1e,
            (byte)0x00, (byte)0x00, (byte)0xdc, (byte)0x76, (byte)0x66, (byte)0x60, (byte)0xf0, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x7c, (byte)0xc0, (byte)0x78, (byte)0x0c, (byte)0xf8, (byte)0x00,
            (byte)0x10, (byte)0x30, (byte)0x7c, (byte)0x30, (byte)0x30, (byte)0x34, (byte)0x18, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0xcc, (byte)0xcc, (byte)0xcc, (byte)0xcc, (byte)0x76, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0xcc, (byte)0xcc, (byte)0xcc, (byte)0x78, (byte)0x30, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0xc6, (byte)0xd6, (byte)0xfe, (byte)0xfe, (byte)0x6c, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0xc6, (byte)0x6c, (byte)0x38, (byte)0x6c, (byte)0xc6, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0xcc, (byte)0xcc, (byte)0xcc, (byte)0x7c, (byte)0x0c, (byte)0xf8,
            (byte)0x00, (byte)0x00, (byte)0xfc, (byte)0x98, (byte)0x30, (byte)0x64, (byte)0xfc, (byte)0x00,
            (byte)0x1c, (byte)0x30, (byte)0x30, (byte)0xe0, (byte)0x30, (byte)0x30, (byte)0x1c, (byte)0x00,
            (byte)0x18, (byte)0x18, (byte)0x18, (byte)0x00, (byte)0x18, (byte)0x18, (byte)0x18, (byte)0x00,
            (byte)0xe0, (byte)0x30, (byte)0x30, (byte)0x1c, (byte)0x30, (byte)0x30, (byte)0xe0, (byte)0x00,
            (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
    };

    private static final byte[] INITIAL_VALUES_d1b0 = {
            (byte) 0x00, (byte) 0x00, (byte) 0xe0, (byte) 0x05,
            (byte) 0x64, (byte) 0x0a, (byte) 0x74, (byte) 0x0b,
            (byte) 0xb4, (byte) 0x0b, (byte) 0xb4, (byte) 0x0c,
            (byte) 0xe6, (byte) 0x0d, (byte) 0x2a, (byte) 0x0f,
            (byte) 0x72, (byte) 0x0f, (byte) 0x93, (byte) 0x10,
            (byte) 0x93, (byte) 0x11, (byte) 0xd3, (byte) 0x11,
            (byte) 0x13, (byte) 0x12, (byte) 0x53, (byte) 0x12,
            (byte) 0x93, (byte) 0x12, (byte) 0xb4, (byte) 0x13,
            (byte) 0xf4, (byte) 0x13, (byte) 0xf4, (byte) 0x14,
            (byte) 0x04, (byte) 0x16, (byte) 0x04, (byte) 0x1a,
            (byte) 0x04, (byte) 0x1b, (byte) 0x36, (byte) 0x1c,
            (byte) 0x87, (byte) 0x1c, (byte) 0xc7, (byte) 0x1c,
            (byte) 0xd7, (byte) 0x1d, (byte) 0xd7, (byte) 0x1e,
            (byte) 0xd6, (byte) 0x1f, (byte) 0x1e, (byte) 0x20,
            (byte) 0x1e, (byte) 0x24, (byte) 0x5e, (byte) 0x24,
            (byte) 0x5e, (byte) 0x25, (byte) 0x5e, (byte) 0x26,
            (byte) 0x9e, (byte) 0x26, (byte) 0x9e, (byte) 0x27,
            (byte) 0xae, (byte) 0x28, (byte) 0xae, (byte) 0x29,
            (byte) 0x93, (byte) 0x2a, (byte) 0xbf, (byte) 0x2b,
            (byte) 0xbf, (byte) 0x2c, (byte) 0xe0, (byte) 0x2d
    };
}
