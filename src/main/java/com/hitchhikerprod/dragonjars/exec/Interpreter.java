package com.hitchhikerprod.dragonjars.exec;

import com.hitchhikerprod.dragonjars.DragonWarsApp;
import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.HuffmanDecoder;
import com.hitchhikerprod.dragonjars.data.Images;
import com.hitchhikerprod.dragonjars.data.MapData;
import com.hitchhikerprod.dragonjars.data.ModifiableChunk;
import com.hitchhikerprod.dragonjars.data.RomImageDecoder;
import com.hitchhikerprod.dragonjars.data.StringDecoder;
import com.hitchhikerprod.dragonjars.data.TextureDecoder;
import com.hitchhikerprod.dragonjars.exec.instructions.*;
import com.hitchhikerprod.dragonjars.ui.RootWindow;
import javafx.event.EventHandler;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class Interpreter {
    private static final int MASK_LOW = 0x000000ff;
    private static final int MASK_HIGH = 0x0000ff00;
    private static final int MASK_WORD = 0x0000ffff;

    public static final int PARTY_SEGMENT = 1;

    private final DragonWarsApp app;

    /* Utility classes */

    private final RomImageDecoder romImageDecoder;
    private final StringDecoder stringDecoder;
    private final TextureDecoder textureDecoder;
    private final MapData mapDecoder;

    /* Memory space */

    private final Memory memory;
    private final Heap heap;

    private final Deque<Byte> stack = new ArrayDeque<>(); // one-byte values
    private final byte[] bufferD1B0 = new byte[896 * 2]; // 0x380 words

    private int mul_result; // 0x1166:4
    private int div_result; // 0x116a:4

    private int draw_borders; // 0x253e
    private int bbox_x0; // 0x2547 (ch)
    private int bbox_y0; // 0x2549 (px)
    private int bbox_x1; // 0x254a (ch)
    private int bbox_y1; // 0x254c (px)

    private int strlen_313c;
    private List<Integer> string_313e = new ArrayList<>();
    private List<Integer> titleString = List.of(); // 0x273a (len) 0x273b:16 (string)

    public int x_3166;
    public int y_3168;
    public int x_31ed; // default for draw_char
    public int y_31ef; // default for draw_char

    private int mem_342f = 0x00;
    private int mem_3430 = 0x00;
    private int bg_color_3431 = 0xffff;

    private int struct_id_4d33 = 0xff;
    private int struct_id_4d4e = 0xff;

    /* Architectural registers */

    // In assembly, metaregister CS contains the address of the segment to which the current code segment
    // has been loaded. Here I store the structure index instead.
    private int cs = -1;
    private int ds = -1;

    private int ip = -1;

    private boolean width;
    private int ax;
    private int bx;
    private boolean flagCarry; // 0x0001
    private boolean flagZero;  // 0x0040
    private boolean flagSign;  // 0x0080

    // debugging information
    private int instructionsExecuted = 0;

    public Interpreter(DragonWarsApp app, List<Chunk> dataChunks) {
        this.app = app;
        this.memory = new Memory(
                dataChunks.getLast(),
                dataChunks.subList(0, dataChunks.size() - 1)
        );
        this.heap = new Heap();

        this.width = false;
        this.ax = 0;
        this.bx = 0;
        this.flagCarry = false;
        this.flagZero = false;
        this.flagSign = false;

        this.romImageDecoder = new RomImageDecoder(this.memory().getCodeChunk());
        this.stringDecoder = new StringDecoder(this.memory().getCodeChunk());
        this.textureDecoder = new TextureDecoder(this.memory().getCodeChunk());
        this.mapDecoder = new MapData(stringDecoder);
    }

    public Interpreter init() {
        final boolean testMode = Objects.isNull(app);

        if (!testMode) loadFromCodeSegment(0xd1b0, 0, bufferD1B0, 80);

        // cs:0150  ax <- 0x0000
        // cs:0155  heap[0x00..0x7f] <- 0x00
        // cs:0157  di <- 0xb6a2 [chunks]
        // cs:015a  ax <- 0xffff
        // cs:015b  cx <- 0x0180
        // cs:015e  chunk_map[0x0000..0x017f] <- 0x00

        // create segments 0 and 1, which have the same segment address, to store party data [0x0e00 bytes]
        final ModifiableChunk partyChunk = new ModifiableChunk(new byte[0x0e00]);
        memory().addSegment(partyChunk, 0xffff, 0x0001, Frob.FROZEN);
        memory().addSegment(partyChunk, 0xffff, 0x0e00, Frob.FROZEN);

        // build "x50" multiplication table
        // which sets ax to 0x2a..
        // cs:0166  al <- 0xff
        // cs:0168  frob.4d32 <- 0xff
        heap(0x56).write(0xffff, 2);
        heap(0x5a).write(0xffff, 2);
        // cs:0177  struct_idx.4d33 <- 0xff
        heap(0x08).write(0xff);
        // cs:017d  inc ax  (ax <- 0x2b00)
        heap(0xdc).write(0x00);
        // 0x377c <- 0x00  ; @0x017e
        setBackground(0x00);
        // run_opening_titles, which we already did
        // erase_video_buffer():
        //   set the frob for segment_idx[4d33] to 0x02
        //     that was init'd to 0xffff, but now is 0xb9c0, i'm just not sure HOW
        //     also this doesn't have any effect because there's no segment loaded?
        //   but it also includes:
        if (!testMode) drawGameplayCorners();

        setBBox(0x01, 0x27, 0x08, 0xb8);
        if (!testMode) draw_borders = 0xff;
        drawStringAndResetBBox();

        this.instructionsExecuted = 0;
        // [width] <- 0x00
        // [3923] <- 0x00

        return this; // for call chaining
    }

    /**
     * Start the interpreter from the provided chunk ID (NOT segment) and address.
     */
    public void start(int chunk, int addr) {
        final int startingSegment = getSegmentForChunk(chunk, Frob.CLEAN);
        Address nextIP = new Address(startingSegment, addr);
        mainLoop(nextIP);
    }

    /**
     * Start the interpreter from the provided Address, which contains a segment/address pair.
     */
    public void start(Address startPoint) {
        System.out.println("** restart");
        mainLoop(startPoint);
    }

    private void mainLoop(Address startPoint) {
        Address nextIP = startPoint;
        while (Objects.nonNull(nextIP)) {
            this.cs = nextIP.segment();
            if (this.ds == -1) this.ds = this.cs;
            this.ip = nextIP.offset();
            final int opcode = memory().read(nextIP, 1);
            System.out.format("%02x %08x %02x\n", cs, ip, opcode);
            final Instruction ins = decodeOpcode(opcode);
            nextIP = ins.exec(this);
            this.instructionsExecuted++;
        }
    }

    public int instructionsExecuted() {
        return this.instructionsExecuted;
    }

    public Memory memory() {
        return this.memory;
    }

    public Heap.Access heap(int index) {
        return heap.get(index);
    }

    public StringDecoder stringDecoder() {
        return this.stringDecoder;
    }

    /**
     * Examines the segment table to translate a chunk ID into a segment ID. If the chunk doesn't exist in the segment
     * table, a new segment will be allocated for it; if it does exist, its frob will be overwritten with the new value.
     * @param chunkId The desired chunk ID
     * @param frob The desired new value of the segment's frob
     * @return The segment ID of the (new) segment
     */
    public int getSegmentForChunk(int chunkId, Frob frob) {
        int segmentId = memory().lookupChunkId(chunkId);
        if (segmentId == -1 || (chunkId & 0x8000) > 0) {
            final ModifiableChunk newChunk = memory().copyDataChunk(chunkId);
            segmentId = memory().getFreeSegmentId();
            memory().setSegment(segmentId, newChunk, chunkId, newChunk.getSize(), frob);
        }
        // should there be a "don't overwrite frob 0xff" guard here?
        memory().setSegmentFrob(segmentId, frob);
        return segmentId;
    }

    /**
     * Marks a segment as available to be unloaded, but doesn't actually remove it. If a subsequent call to
     * getSegmentForChunk() occurs for the same chunk, there's a chance that the existing segment will be revived
     * (rather than unloading and reloading it).
     * @param segmentId
     */
    public void freeSegment(int segmentId) {
        if (memory().getSegmentFrob(segmentId) != Frob.FROZEN) {
            memory().setSegmentFrob(segmentId, Frob.EMPTY);
        }
    }

    public record Rectangle(int x0, int x1, int y0, int y1) {}

    public Rectangle getBBox() {
        return new Rectangle(bbox_x0, bbox_y0, bbox_x1, bbox_y1);
    }

    public void setBBox(int x0, int x1, int y0, int y1) {
        bbox_x0 = x0;
        bbox_y0 = y0;
        bbox_x1 = x1;
        bbox_y1 = y1;
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

    public int getMulResult() {
        return mul_result;
    }

    public void setMulResult(int mulResult) {
        this.mul_result = mulResult;
    }

    public int getDivResult() {
        return div_result;
    }

    public void setDivResult(int divResult) {
        this.div_result = divResult;
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
        this.ax = ((val & MASK_LOW) << 8) | (this.ax & MASK_LOW);
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

    public boolean isWide() {
        return width;
    }

    public void setWidth(boolean width) {
        this.width = width;
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
    public void lowLevelDrawChar(int index, int x, int y, boolean invert) {
        final byte[] bitmask = new byte[8];
        loadFromCodeSegment(0xb9a2, (index & 0x7f) * 8, bitmask, 8);
        app.drawBitmask(bitmask, x, y, invert);
    }

    private void loadFromCodeSegment(int base, int offset, byte[] dest, int length) {
        final int addr = base - 0x0100 + offset;
        final List<Byte> bytes = memory().getCodeChunk().getBytes(addr, length);
        for (int i = 0; i < length; i++) {
            dest[i] = bytes.get(i);
        }
    }

    public void drawStringAndResetBBox() {
        drawString313e();
        if (draw_borders != 0x00) {
            drawHud();
        }
        draw_borders = 0x00;
        setBBox(0x01, 0x27, 0x98, 0xb8);
        x_31ed = 0x01;
        y_31ef = 0x98;
    }

    public void drawString313e() {
        for (int i = 0; i < strlen_313c; i++) {
            lowLevelDrawChar(string_313e.get(i), x_31ed * 8, y_31ef, bg_color_3431 == 0);
            x_31ed += 8;
        }
        strlen_313c = 0;
        x_3166 = x_31ed;
    }

    public void fillRectangle() {
        app.drawRectangle(bg_color_3431, bbox_x0 * 8, bbox_y0, bbox_x1 * 8, bbox_y1);
        x_31ed = bbox_x0; // 0x32a8
        x_3166 = bbox_x0;
        y_31ef = bbox_y0;
        strlen_313c = 0;
    }

    public void setBackground() {
        setBackground(mem_342f);
    }

    /**
     * Sets the background color of forthcoming text to WHITE, unless the input value has bit 0x10 set, in which
     * case the background color is set to BLACK.
     */
    public void setBackground(int al) {
        // confirm: input 0x10 -> al 0x02 -> bx 0x0000
        this.bg_color_3431 = ((al & 0x10) > 0) ? 0x0000 : 0xffff;
        this.mem_342f = this.mem_3430 & 0xff;
        this.mem_3430 = al & 0xff;
    }

    public void drawString(List<Integer> s) {
        int x = x_31ed;
        int y = y_31ef;
        for (int ch : s) {
            if (ch == 0x8d) {
                x = x_31ed;
                y += 8;
            } else {
                lowLevelDrawChar(ch, x * 8, y, bg_color_3431 == 0);
                x += 1;
            }
        }
        x_31ed = x;
        y_31ef = y;
    }

    public void drawString(String s, int x, int y, boolean invert) {
        int fx = x * 8;
        int fy = y * 8;
        for (char ch : s.toCharArray()) {
            lowLevelDrawChar(ch, fx, fy, invert);
            fx += 8;
        }
    }

    public void drawGameplayCorners() {
        // TODO: refactor this
        // we only need to build this buffer once and make it static, really.
        getImageWriter(writer -> {
            final int[] buffer = new int[0x3e80];
            for (int i = 3; i >= 0; i--) {
                romImageDecoder.decodeCorner(buffer, i);
            }
            romImageDecoder.reorg(buffer, writer, 0x08, 0x88, 0x50);
        });
    }

    private boolean boundsCheck(int regionId) {
        expandBBox();

        final byte[] rec = new byte[4]; // x0, y0, x1, y1
        loadFromCodeSegment(0x2644, 4 * regionId, rec, 4);
        final int rec_x0 = rec[0] & 0xff;
        final int rec_y0 = rec[1] & 0xff;
        final int rec_x1 = rec[2] & 0xff;
        final int rec_y1 = rec[3] & 0xff;

        if ((rec_x0 >= bbox_x1) || (rec_y0 >= bbox_y1) || (bbox_x0 >= rec_x1) || (bbox_y0 >= rec_y1)) {
            shrinkBBox();
            return false;
        } else {
            shrinkBBox();
            return true;
        }
    }

    public void drawHud() {
        draw_borders = 0x00;
        for (int regionId = 0; regionId < 14; regionId++) {
            if (boundsCheck(regionId)) {
                switch (regionId) {
                    case 0x09 -> { // the vertical column with spell icons
                        final int finalRegionId = regionId;
                        getImageWriter(writer -> romImageDecoder.decodeRomImage(finalRegionId, writer));
                        // ax <- 0x0000
                        // [4a71] <- ax
                        // [4a73] <- ax
                        // CF <- 1
                    }
                    case 0x0a -> { // viewport
                        // if bounds_check(0xa)
                        //   ... we already did that
                        // check_and_push_video_data(0xa)
                        //   which seems to bitblast the most recent buffer data to the video card
                        // TODO: keep a separate 'video' buffer for the viewport
                        //   (and maybe reblast the 'corners' afterwards)
                    }
                    case 0x0b -> {
                        heap(0x18).write(0x00, 7); // heap[18:1e] <- 0x00
                        drawPartyInfoArea();
                    }
                    case 0x0c -> drawMapTitle();
                    case 0x0d -> { // message pane, 0x267c
                        setBBox(0x01, 0x27, 0x98, 0xb8);
                        fillRectangle();
                    }
                    default -> {
                        final int finalRegionId = regionId;
                        getImageWriter(writer -> romImageDecoder.decodeRomImage(finalRegionId, writer));
                        // checkAndPushVideoData() this should just clear the video buffer, which we don't need to do
                    }
                }
            }
        }
    }

    private void drawPartyInfoArea() { // 0x1a08
        if (! boundsCheck(0x0b)) return;

        final int save_31ed = x_31ed;
        final int save_31ef = y_31ef;
        final int save_heap_06 = heap(0x06).read();

        // set the indirect function to draw_char()

        for (int charId = 0; charId < 7; charId++) {
            // Assembly loops in the other direction, but our bar-drawing code needs to go this way
            // to avoid mishaps with the black pixels between bars.
            final int heapIndex = 0x18 + charId;
            int ax = heap(heapIndex).read();
            if ((ax & 0x80) > 0) continue;
            ax = ((ax & 0x02) > 0) ? 0x01 : 0x10;
            drawCharacterInfo(charId, ax);
            heap(heapIndex).write(0xff);
        }

        heap(0x06).write(save_heap_06);
        x_31ed = save_31ed;
        y_31ef = save_31ef;
    }

    private void drawCharacterInfo(int charId, int flag) {
        // input flag is either 0x10 or 0x01
        // final int simpleFlag = flag & 0x0f; // either 0 or 1
        setBackground(flag);

        x_31ed = 0x1b;
        y_31ef = (charId << 4) + 0x20;

        final int charsInParty = heap(0x1f).read();
        if (charId >= charsInParty) {
            getImageWriter(writer -> {
                final int black = Images.convertColorIndex(0);
                for (int dy = 0; dy < 0x10; dy++) {
                    for (int x = x_31ed * 8; x < 0x27 * 8; x++) {
                        writer.setArgb(x, y_31ef + dy, black);
                    }
                }
            });
            setBackground();
            return;
        }

        final int charBaseAddress = heap(0x0a + charId).read() << 8;

        final List<Integer> nameCh = getCharacterName(charBaseAddress);
        indentTo(0x1b + ((0x0d - nameCh.size()) >> 1));
        drawString(nameCh);
        indentTo(0x27);

        x_31ed = 0x1b;

        final int statuses = memory().read(PARTY_SEGMENT, charBaseAddress + 0x4c, 2);
        for (int i = 3; i >= 0; i--) {
            final int mask = memory().getCodeChunk().getUnsignedByte(0x1a61 + i);
            if ((statuses & mask) > 0) {
                drawStatusHelper(i);
                setBackground();
                return;
            }
        }

        drawBarHelper(0x08, charBaseAddress + 0x14, 12);
        drawBarHelper(0x0b, charBaseAddress + 0x18, 10);
        drawBarHelper(0x0e, charBaseAddress + 0x1c, 9);

        setBackground();
    }

    public void indentFromBbox(int dx) {
        indentTo(bbox_x0 + dx);
    }

    private void indentTo(int limit) {
        while (x_31ed < limit) drawString(List.of(0xa0));
    }

    private List<Integer> getCharacterName(int charBaseAddress) {
        final List<Integer> nameCh = new ArrayList<>();
        int pointer = charBaseAddress;
        int ch = 0x80;
        while ((ch & 0x80) > 0) {
            ch = memory().read(PARTY_SEGMENT, pointer, 1);
            nameCh.add(ch);
            pointer++;
        }
        return nameCh;
    }

    private void drawStatusHelper(int i) {
        final int wordAddress = memory().getCodeChunk().read(0x1a69 + (2 * i), 2) - 0x100;
        final int xOffset = memory().getCodeChunk().getUnsignedByte(0x1a65 + i);
        this.stringDecoder.decodeString(memory().getCodeChunk(), wordAddress);
        final List<Integer> chars = new ArrayList<>();
        chars.add(0xe9); // 'i'
        chars.add(0xf3); // 's'
        chars.add(0xa0); // ' '
        chars.addAll(stringDecoder.getDecodedChars());
        y_31ef += 8;
        indentTo(xOffset);
        drawString(chars);
        indentTo(0x27);
    }

    private void drawBarHelper(int y, int attributeAddr, int color) {
        getImageWriter(writer -> {
            final int cur = memory().read(PARTY_SEGMENT, attributeAddr, 2);
            final int max = memory().read(PARTY_SEGMENT, attributeAddr + 2, 2);
            final int barWidth = 0x60 * cur / max;
            final int black = Images.convertColorIndex(0);
            final int colorValue = Images.convertColorIndex(color);
            for (int dx = 0; dx < barWidth; dx++) {
                writer.setArgb((x_31ed * 8) + dx, y_31ef + y, colorValue);
                writer.setArgb((x_31ed * 8) + dx, y_31ef + y + 1, colorValue);
                writer.setArgb((x_31ed * 8) + dx, y_31ef + y + 2, black);
            }
        });
    }

    private void markSegment4d33Dirty() { // 0x4bc2
        final int segmentId = struct_id_4d33;
        if (segmentId != 0xff) {
            memory().setSegmentFrob(segmentId, Frob.DIRTY);
            struct_id_4d4e = 0x00;
            struct_id_4d33 = 0xff;
        }
    }

    // FIXME, need map chunk decoder and texture decoder
    public void drawCurrentViewport() {
        markSegment4d33Dirty();
        final int mapId = heap(0x02).read();
        // N.B. these are Huffman encoded, so the map decoder will decompress them. Don't try to read them directly
        final ModifiableChunk primaryData = memory().copyDataChunk(mapId + 0x46);
        // final ModifiableChunk primaryData = memory().copyDataChunk(0x10);
        final ModifiableChunk secondaryData = memory().copyDataChunk(mapId + 0x1e);
        mapDecoder.parse(mapId, primaryData, false, secondaryData);

        heap(0x21).write(mapDecoder.getMaxX());
        heap(0x22).write(mapDecoder.getMaxY());
        heap(0x23).write(mapDecoder.getFlags());
        setTitleString(mapDecoder.getTitleChars());

        drawRoofTexture();
    }

    private void drawRoofTexture() {
        final int partyX = heap(0x01).read();
        final int partyY = heap(0x00).read();
        final int squareData = mapDecoder.getSquare(partyX, partyY);
        // we want .... .... .**. .... .... ....
        final int index = (squareData >> 13) & 0x03;
        final int roofTextureFakeId = mapDecoder.getRoofTexture(index);
        if (roofTextureFakeId == 1) {
            // [1011/seg] <- segment for chunk 0x6f, which was stored in [57c4] somehow
            final Chunk compressedChunk = memory().copyDataChunk(0x6f);
            final HuffmanDecoder huffman = new HuffmanDecoder(compressedChunk);
            final ModifiableChunk textureChunk = new ModifiableChunk(huffman.decode());
            // [100f/adr] <- 0x0000
            // [352e/x0] <- 0x0000
            // [3532/y0] <- 0x0000
            // [100e] <- 0x00
            // bx <- 0x0004
            final int[] buffer = new int[0x3e80];
            for (int i = 0; i < 0x3e80; i++) buffer[i] = 0x11;
            textureDecoder.setBuffer(buffer);

            textureDecoder.entrypoint0ca7(textureChunk, 0x0000, 0x4, 0x0, 0x0, 0x0);

            getImageWriter(writer -> {
                int pointer = 0;
                for (int y = 0x08; y < 0x88; y++) {
                    for (int x = 0x02; x < 0x16; x++) {
                        Images.convertEgaData(writer, (z) -> buffer[z] & 0xff, pointer, 8 * x, y);
                        pointer += 4;
                    }
                }
            });

            // pick_gfx_page()
            //decodeRoofTexture(0x6f);
        } else {
            // Draw a pre-programmed checkerboard; I don't bother reading data from the executable
            getImageWriter(writer -> {
                for (int y = 0x08; y < 0x88; y++) {
                    Function<Integer, Integer> getter;
                    if (y < 0x38)
                        getter = (y % 2 == 0) ? (z) -> 0x40 : (z) -> 0x04;
                    else
                        getter = (z) -> 0x00;
                    for (int x = 0x02; x < 0x16; x++) {
                        Images.convertEgaData(writer, getter, 0, 8 * x, y);
                    }
                }
            });
        }
    }

    public void setTitleString(List<Integer> chars) {
        if (chars.size() > 16) {
            this.titleString = List.copyOf(chars.subList(0, 16));
        } else {
            this.titleString = List.copyOf(chars);
        }
        drawMapTitle();
    }

    private void drawMapTitle() { // 0x2cd4
        // x_31ed and y_31ef are preserved across this call, but I don't use them
        // in this implementation. They're also more clever about only decoding the
        // image bytes for spaces where they aren't writing characters.
        for (int x = 0; x < 16; x++) {
            final int pictureId = 0x1b + x;
            getImageWriter(writer -> romImageDecoder.decodeRomImage(pictureId, writer));
        }

        setBackground(0x10);
        int x = 0x04 + ((16 - this.titleString.size()) / 2);
        for (int ch : this.titleString) {
            lowLevelDrawChar(ch, x * 8, 0, bg_color_3431 == 0);
            x += 1;
        }
        setBackground();
    }

    /**
     * Draws an empty box on the screen.
     */
    public void drawModal(Address addr) {
        final int x0 = memory().read(addr, 1);         // ch adr
        final int y0 = memory().read(addr.incr(1), 1); // pix adr
        final int x1 = memory().read(addr.incr(2), 1);
        final int y1 = memory().read(addr.incr(3), 1);
        boolean invert = bg_color_3431 == 0;

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
        setBBox(x0, x1, y0, y1);
        // copy 0x2547/bbox/x0,y0 to 0x31ed/x0,y0
        x_31ed = x0;
        y_31ef = y0;

        int x;
        int y = y0;
        // draw top border
        for (x = x0; x < x1; x += 1) {
            lowLevelDrawChar(0x01, x * 8, y, invert);
        }
        lowLevelDrawChar(0x00, x0 * 8, y, invert);
        lowLevelDrawChar(0x02, (x1 - 1) * 8, y, invert);
        // draw vertical edges
        y += 8;
        while (y < y1 - 8) {
            lowLevelDrawChar(0x03, x0 * 8, y, invert);
            lowLevelDrawChar(0x04, (x1 - 1) * 8, y, invert);
            y += 8;
        }
        // draw bottom border
        for (x = x0; x < x1; x += 1) {
            lowLevelDrawChar(0x06, x * 8, y, invert);
        }
        lowLevelDrawChar(0x05, x0 * 8, y, invert);
        lowLevelDrawChar(0x07, (x1 - 1) * 8, y, invert);

        shrinkBBox();
        fillRectangle();
    }

    public void setPrompt(Map<KeyCode, Address> prompts) {
        final EventHandler<KeyEvent> keyHandler = event -> {
            for (KeyCode code : prompts.keySet()) {
                if (event.getCode() == code) {
                    this.start(prompts.get(code));
                }
            }
        };
        app.setKeyHandler(keyHandler);
    }

    private int byteToInt(byte b) {
        return MASK_LOW & ((int) b);
    }

    private byte intToByte(int i) {
        return (byte)(i & MASK_LOW);
    }

    private void getImageWriter(Consumer<PixelWriter> fn) {
        final Image image = RootWindow.getInstance().getImage();
        if (image instanceof WritableImage wimage) {
            final PixelWriter writer = wimage.getPixelWriter();
            fn.accept(writer);
        }
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
            case 0x5c -> new RecurseOverParty();
            case 0x5d -> new LoadAXPartyAttribute();
            case 0x5e -> new StoreAXPartyAttribute();
            case 0x5f -> new SetPartyFlag();
            case 0x60 -> new ClearPartyFlag();
            case 0x61 -> new TestPartyFlag();
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
            // case 0x71 -> new RunSpecialEvent(); // TODO
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
            case 0x80 -> new IndentAX();
            case 0x81 -> (i) -> Instructions.printNumber(i, i.getAX(true)); // print 4d number
            case 0x82 -> (i) -> Instructions.printNumber(i, i.getMulResult()); // print 9d number
            // case 0x83 -> new IndirectChar();
            // case 0x84 -> new AllocateSegment(); // should just be getSegmentForChunk() but the call pattern is weird
            case 0x85 -> new FreeSegmentAL();
            case 0x86 -> new LoadChunkAX();
            // case 0x87 -> new PersistChunk();
            // case 0x88 -> new WaitForEscapeKey();
            case 0x89 -> new ReadKeySwitch();
            // case 0x8a -> new ShowMonsterImage();
            case 0x8b -> (i) -> { i.drawCurrentViewport(); return i.getIP().incr(); };
            // case 0x8c -> new ShowYesNoModal();
            // case 0x8d -> new PromptAndReadInput();
            case 0x8e -> Instructions.NOOP;
            case 0x8f -> new StrToInt();
            // case 0x90 -> new PlaySoundEffect();
            // case 0x91 -> new PauseUntilKey();
            // case 0x92 -> new PauseUntilKeyOrTime();
            case 0x93 -> new PushBL();
            case 0x94 -> new PopBL();
            case 0x95 -> new SetCursor();
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
            default -> throw new IllegalArgumentException("Unimplemented opcode " + opcode);
        };
    }
}
