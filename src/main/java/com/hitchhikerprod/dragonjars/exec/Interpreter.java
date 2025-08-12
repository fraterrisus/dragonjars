package com.hitchhikerprod.dragonjars.exec;

import com.hitchhikerprod.dragonjars.DragonWarsApp;
import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.ModifiableChunk;
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

    private final List<Chunk> allChunks;
    private final List<ModifiableChunk> loadedChunks;
    private Address thisIP;
    private Address nextIP;

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

    private boolean width;
    // Metaregister CS is the index of the code chunk
    // Likewise, DS is the index of the data chunk
    private int dataChunkIndex;
    private int ax; // sometimes one byte, sometimes two
    private int bx; // sometimes one byte, sometimes two
    private boolean flagCarry; // 0x0001
    private boolean flagZero;  // 0x0040
    private boolean flagSign;  // 0x0080

    // debugging information
    private int instructionsExecuted;

    public Interpreter(DragonWarsApp app, List<Chunk> dataChunks, int initialChunk, int initialAddr) {
        this.app = app;
        this.allChunks = dataChunks;
        this.loadedChunks = new ArrayList<>(dataChunks.size());
        for (int i = 0; i < dataChunks.size(); i++) {
            loadedChunks.add(null);
        }
        this.thisIP = new Address(-1, -1);
        this.nextIP = new Address(initialChunk, initialAddr);
        this.width = false;
        this.dataChunkIndex = 0;
        this.ax = 0;
        this.bx = 0;
        this.flagCarry = false;
        this.flagZero = false;
        this.flagSign = false;
        this.instructionsExecuted = 0;

        loadChunk(0x00);
        loadChunk(0x01);
        loadChunk(initialChunk);
        drawString313e();
        resetBbox();
        this.invert_3431 = 0x00;
    }

    public void start() {
        System.arraycopy(D1B0_INITIAL_VALUES, 0, this.bufferD1B0, 0, D1B0_INITIAL_VALUES.length);
        this.instructionsExecuted = 0;

        while (Objects.nonNull(nextIP)) {
            thisIP = nextIP;
            final int opcode = readByte(this.getIP());
            System.out.format("%02x %08x %02x\n", thisIP.chunk(), thisIP.offset(), opcode);
            final Instruction ins = decodeOpcode(opcode);
            nextIP = ins.exec(this);
            this.instructionsExecuted++;
        }
    }

    public int instructionsExecuted() {
        return this.instructionsExecuted;
    }

    public void loadChunk(int chunkId) {
        if (Objects.nonNull(loadedChunks.get(chunkId))) return;
        final Chunk readOnlyChunk = allChunks.get(chunkId);
        final List<Byte> raw = readOnlyChunk.getBytes(0, readOnlyChunk.getSize());
        for (int i = 0; i <= readOnlyChunk.getSize() % 256; i++) {
            raw.add((byte)0x00);
        }
        final ModifiableChunk chunk = new ModifiableChunk(raw);
        loadedChunks.set(chunkId, chunk);
    }

    public void unloadChunk(int chunkId) {
        if (Objects.nonNull(loadedChunks.get(chunkId))) {
            loadedChunks.set(chunkId, null);
        }
    }

    public void resetBbox() {
        draw_borders = 0x00;

        bbox_x0 = 0x01;
        bbox_x1 = 0x27;
        bbox_y0 = 0x98;
        bbox_y1 = 0xb8;

        x_31ed = 0x01;
        y_31ef = 0x98;
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

    public Chunk getChunk(int index) {
        return loadedChunks.get(index);
    }

    public Address getIP() {
        return thisIP;
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
        return this.thisIP.chunk();
    }

    public int getDS() {
        return this.dataChunkIndex;
    }

    public void setDS(int val) {
        if (val != this.dataChunkIndex) {
            loadChunk(val);
            this.dataChunkIndex = val;
        }
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

    /** Retrieves a single byte from chunk data. */
    public int readByte(int chunk, int offset) {
        return Objects.requireNonNull(loadedChunks.get(chunk)).getUnsignedByte(offset);
    }

    /** Retrieves a single byte from chunk data. */
    public int readByte(Address addr) {
        return readByte(addr.chunk(), addr.offset());
    }

    /** Retrieves a word (two bytes) from chunk data. */
    public int readWord(int chunk, int offset) {
        return Objects.requireNonNull(loadedChunks.get(chunk)).getWord(offset);
    }

    /** Retrieves a word (two bytes) from chunk data. */
    public int readWord(Address addr) {
        return readWord(addr.chunk(), addr.offset());
    }

    public int readData(int chunk, int offset, int size) {
        return Objects.requireNonNull(loadedChunks.get(chunk)).getData(offset, size);
    }

    public int readData(Address addr, int size) {
        return readData(addr.chunk(), addr.offset(), size);
    }

    public void writeByte(int chunk, int offset, int value) {
        final ModifiableChunk c = Objects.requireNonNull(loadedChunks.get(chunk));
        c.setByte(offset, value);
    }

    public void writeByte(Address addr, int value) {
        writeByte(addr.chunk(), addr.offset(), value);
    }
    
    public void writeWord(int chunk, int offset, int value) {
        final ModifiableChunk c = Objects.requireNonNull(loadedChunks.get(chunk));
        c.setWord(offset, value);
    }

    public void writeWord(Address addr, int value) {
        writeWord(addr.chunk(), addr.offset(), value);
    }

    public void writeWidth(int chunk, int addr, int value) {
        if (isWide()) {
            writeWord(chunk, addr, value);
        } else {
            writeByte(chunk, addr, value);
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
        System.arraycopy(B9A2_INITIAL_VALUES, (index & 0x7f) * 8, bitmask, 0, 8);
        app.drawBitmask(bitmask, x, y, invert);
    }

    public void drawStringAndResetBBox() {
        drawString313e();
        if (draw_borders != 0x00) {
            drawHudBorders();
        }
        resetBbox();
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
            drawChar(ch, x_31ed * 8, y_31ef, invert_3431 != 0);
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

    public void drawHudBorders() {
        // TODO?
    }

    /**
     * Draws an empty box on the screen.
     * @param x0 Pixel coordinate of the left side of the box's frame.
     * @param y0 Pixel coordinate of the top of the box's frame.
     * @param x1 Pixel coordinate of the right side of the box's frame.
     * @param y1 Pixel coordinate of the bottom of the box's frame.
     */
    public void drawModal(int x0, int y0, int x1, int y1) {
        int x;
        int y = y0;
        for (x = x0; x < x1; x += 8) {
            drawChar(0x01, x, y, false);
        }
        drawChar(0x00, x0, y, false);
        drawChar(0x02, x1 - 8, y, false);

        y += 8;
        while (y < y1) {
            for (x = x0; x < x1; x += 8) {
                drawChar(0x7f, x, y, false);
            }
            drawChar(0x03, x0, y, false);
            drawChar(0x04, x1 - 8, y, false);
            y += 8;
        }

        for (x = x0; x < x1; x += 8) {
            drawChar(0x06, x, y, false);
        }
        drawChar(0x05, x0, y, false);
        drawChar(0x07, x1 - 8, y, false);
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
            case 0x1f -> Instructions.NOOP; // "read chunk table"
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
            case 0x85 -> (i) -> { // FreeStructMemory
                if (0x01 < i.getAL() && i.getAL() != 0xff) {
                    i.unloadChunk(i.getAL());
                }
                return i.getIP().incr();
            };
            case 0x86 -> (i) -> { i.loadChunk(i.getAL()); return i.getIP().incr(); };
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
            case 0x9e -> new GetChunkSize();
            // case 0x9f -> new YouWin();
            default -> throw new IllegalArgumentException("Unknown opcode " + opcode);
        };
    }

    // Character bitmaps
    private static final byte[] B9A2_INITIAL_VALUES = {
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

    private static final byte[] D1B0_INITIAL_VALUES = {
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
