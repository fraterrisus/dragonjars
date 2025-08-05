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
    private Chunk codeChunk;
    private Address thisIP;
    private Address nextIP;

    private final Deque<Integer> stack = new ArrayDeque<>(); // one-byte values
    private final int[] heap = new int[256];
    // TODO: image+memory as segmented by ds

    private boolean width;
    // Metaregister CS is the index of the code chunk
    // Likewise, DS is the index of the data chunk
    private int dataChunkIndex;
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
        this.dataChunkIndex = 0;
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
                codeChunk = getChunk(nextIP.chunk());
            }
            thisIP = nextIP;
            final int opcode = readByte(this.getIP());
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
        return this.thisIP.chunk();
    }

    public int getDS() {
        return this.dataChunkIndex;
    }

    public void setDS(int val) {
        this.dataChunkIndex = val;
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

    /** Retrieves a word (two bytes) from chunk data. */
    public int readWord(int chunk, int offset) {
        return dataChunks.get(chunk).getWord(offset);
    }

    /** Retrieves a word (two bytes) from chunk data. */
    public int readWord(Address addr) {
        return readWord(addr.chunk(), addr.offset());
    }

    /** Retrieves a single byte from chunk data. */
    public int readByte(int chunk, int offset) {
        return dataChunks.get(chunk).getUnsignedByte(offset);
    }

    /** Retrieves a single byte from chunk data. */
    public int readByte(Address addr) {
        return readByte(addr.chunk(), addr.offset());
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
            // case 0x33 -> new MulAXHeap();
            // case 0x34 -> new MulAXImm();
            // case 0x35 -> new DivAXHeap();
            // case 0x36 -> new DivAXImm();
            // case 0x37 -> new AndAXHeap();
            // case 0x38 -> new AndAXImm();
            // case 0x39 -> new OrAXHeap();
            // case 0x3a -> new OrAXImm();
            // case 0x3b -> new XorAXHeap();
            // case 0x3c -> new XorAXImm();
            // The CMP instructions flip the carry bit before writing, which makes JC and JNC
            // behave in the opposite manner. But ADD doesn't flip carry.
            // case 0x3d -> new CmpAXHeap();
            // case 0x3e -> new CmpAXImm();
            // case 0x3f -> new CmpBXHeap();
            // case 0x40 -> new CmpBXImm(); // wide or no?
            // case 0x41 -> new JumpCarry();
            // case 0x42 -> new JumpNotCarry();
            // case 0x43 -> new JumpAbove();
            // case 0x44 -> new JumpEqual();
            // case 0x45 -> new JumpNotEqual();
            // case 0x46 -> new JumpSign();
            // case 0x47 -> new JumpNotSign();
            // case 0x48 -> new TestHeapSign();
            // case 0x49 -> new LoopBX();
            // case 0x4a -> new LoopBXLimit();
            case 0x4b -> (i) -> { i.setCarry(true); return i.getIP().incr(Instruction.OPCODE); };
            case 0x4c -> (i) -> { i.setCarry(false); return i.getIP().incr(Instruction.OPCODE); };
            // case 0x4d -> new RandomAX();
            // case 0x4e -> new SetHeapBit();
            // case 0x4f -> new ClearHeapBit();
            // case 0x50 -> new TestHeapBit();
            // case 0x51 -> new ArrayMax();
            // case 0x52 -> new Jump();
            // case 0x53 -> new Call();
            // case 0x54 -> new Return();
            case 0x55 -> new PopAX();
            case 0x56 -> new PushAX();
            // case 0x57 -> new LongJump();
            // case 0x58 -> new LongCall();
            // case 0x59 -> new LongReturn();
            case 0x5a -> new ExitInstruction(); // "stop executing instruction stream"
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
            // case 0x66 -> new TestHeap();
            // case 0x67 -> new DropItem();
            case 0x8e -> Instruction.NOOP;
            // case 0x93 -> new PushBL();
            // case 0x94 -> new PopBL();
            // case 0x99 -> new TestAX();
            // case 0x9f -> new YouWin();
            default -> throw new IllegalArgumentException("Unknown opcode " + opcode);
        };
    }
}
