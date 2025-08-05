package com.hitchhikerprod.dragonjars.exec;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.instructions.*;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

public class Interpreter {
    private static final int MASK_LOW = 0x000000ff;
    private static final int MASK_HIGH = 0x0000ff00;
    private static final int MASK_WORD = 0x0000ffff;

    private final List<Chunk> dataChunks;
    private Chunk chunk;
    private Address thisIP;
    private Address nextIP;

    private final Deque<Integer> stack = new ArrayDeque<>();
    private final int[] heap = new int[256];
    // TODO: image+memory as segmented by ds

    private boolean width;
    private int cs; // two bytes, although this is thisIP.chunk(), right?
    private int ds; // two bytes
    private int ax; // sometimes one byte, sometimes two
    private int bx; // sometimes one byte, sometimes two
    private boolean flagCarry; // 0x0001
    private boolean flagZero;  // 0x0040
    private boolean flagSign;  // 0x0080

    public Interpreter(List<Chunk> dataChunks, int initialChunk, int initialAddr) {
        this.dataChunks = dataChunks;
        this.thisIP = new Address(-1, -1);
        this.nextIP = new Address(initialChunk, initialAddr);
        this.width = false;
        this.cs = 0;
        this.ds = 0;
        this.ax = 0;
        this.bx = 0;
        this.flagCarry = false;
        this.flagZero = false;
        this.flagSign = false;
    }

    public boolean getCarry() {
        return flagCarry;
    }

    public void setCarry(boolean flag) {
        this.flagCarry = flag;
    }

    public boolean getZero() {
        return flagZero;
    }

    public void setZero(boolean flag) {
        this.flagZero = flag;
    }

    public void start() {
        while (Objects.nonNull(nextIP)) {
            if (nextIP.chunk() != thisIP.chunk()) {
                chunk = getChunk(nextIP.chunk());
            }
            thisIP = nextIP;
            final int opcode = readByte(thisIP.offset());
            final Instruction ins = decodeOpcode(opcode);
            nextIP = ins.exec(this);
        }
    }

    public Chunk getChunk(int index) {
        return dataChunks.get(index);
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

    public void setAX(int val) {
        if (width) {
            this.ax = val & MASK_WORD;
        } else {
            setAL(val);
        }
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

    public void setBX(int val) {
        if (width) {
            this.bx = val & MASK_WORD;
        } else {
            setBL(val);
        }
    }

    public int getCS() {
        return this.cs & MASK_WORD;
    }

    public void setCS(int val) {
        this.cs = val & MASK_WORD;
    }

    public int getDS() {
        return this.ds & MASK_WORD;
    }

    public void setDS(int val) {
        this.ds = val & MASK_WORD;
    }

    public void push(int val) {
        this.stack.push(val);
    }

    public int pop() {
        return this.stack.pop() & MASK_WORD;
    }

    public void setHeap(int index, int val) {
        this.heap[index & MASK_LOW] = val & MASK_LOW;
        if (width) {
            this.heap[(index & MASK_LOW) + 1] = (val & MASK_HIGH) >> 8;
        }
    }

    public int getHeap(int index) {
        return (width) ? getHeapWord(index) : getHeapByte(index);
    }

    public int getHeapWord(int index) {
        final int lo = this.heap[index & MASK_LOW] & MASK_LOW;
        final int hi = this.heap[(index + 1) & MASK_LOW] & MASK_LOW;
        return (hi << 8) | lo;
    }

    public int getHeapByte(int index) {
        return this.heap[index & MASK_LOW] & MASK_LOW;
    }

    public boolean isWide() {
        return width;
    }

    public void setWidth(boolean width) {
        this.width = width;
    }

    /**
     * Retrieves a word (two bytes) from the current chunk pointer. Does not increment the pointer.
     * @param offset
     */
    public int readWord(int offset) {
        return chunk.getWord(offset);
    }

    /**
     * Retrieves a single byte from the current chunk pointer. Does not increment the pointer.
     * @param offset
     */
    public int readByte(int offset) {
        return chunk.getUnsignedByte(offset);
    }

    private Instruction decodeOpcode(int opcode) {
        return switch (opcode) {
            case 0x00 -> Instruction.SET_WIDE;
            case 0x01 -> Instruction.SET_NARROW;
            case 0x02 -> new PushDS();
            case 0x03 -> new PopDS();
            case 0x04 -> new PushCS();
            case 0x05 -> new LoadBLHeap();
            case 0x06 -> new LoadBLImm();
            case 0x07 -> new LoadBLZero();
            case 0x08 -> new StoreBLHeap();
            case 0x09 -> new LoadAXImm();
            case 0x0a -> new LoadAXHeap();
            case 0x0b -> new LoadAXHeapOffset();
            // case 0x0c -> new LoadAX();
            // case 0x0d -> new LoadAXOffset();
            // case 0x0e -> new LoadAXIndirect();
            case 0x0f -> new LoadAXLongPtr();
            // case 0x10 -> new LoadAXIndirectImm();
            case 0x11 -> new StoreZeroHeap();
            case 0x12 -> new StoreAXHeap();
            case 0x13 -> new StoreAXHeapOffset();
            // case 0x14 -> new StoreAX();
            // case 0x15 -> new StoreAXOffset();
            // case 0x16 -> new StoreAXIndirect();
            // case 0x17 -> new StoreAXLongPtr();
            // case 0x18 -> new StoreAXIndirectImm();
            case 0x19 -> new MoveHeap();
            case 0x1a -> new StoreImmHeap();
            // case 0x1b -> new MoveData();
            // case 0x1c -> new StoreImm();
            // case 0x1d -> new BufferCopy();
            case 0x1e -> new ExitInstruction(); // "kill executable"
            case 0x1f -> Instruction.NOOP; // "read chunk table"
            // 20 sends the (real) IP to 0x0000, which is probably a segfault
            case 0x21 -> new MoveALBL();
            case 0x22 -> new MoveBXAX();
            case 0x23 -> new IncHeap();
            case 0x24 -> new IncAX();
            case 0x25 -> new IncBL();
            // case 0x26 -> new DecHeap();
            // case 0x27 -> new DecAX();
            // case 0x28 -> new DecBL();
            // case 0x29 -> new LeftShiftHeap();
            // case 0x2a -> new LeftShiftAX();
            // case 0x2b -> new LeftShiftBL();
            // case 0x2c -> new RightShiftHeap();
            // case 0x2d -> new RightShiftAX();
            // case 0x2e -> new RightShiftBL();
            case 0x2f -> new AddAXHeap();
            // case 0x30 -> new AddAXImm(); // with carry
            // case 0x31 -> new SubAXHeap(); // with carry
            // case 0x32 -> new SubAXImm(); // with carry
            case 0x5a -> new ExitInstruction(); // "stop executing instruction stream"
            default -> throw new IllegalArgumentException("Unknown opcode " + opcode);
        };
    }
}
