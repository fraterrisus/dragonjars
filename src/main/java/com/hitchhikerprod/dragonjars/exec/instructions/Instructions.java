package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.hitchhikerprod.dragonjars.exec.instructions.Instruction.OPCODE;

public class Instructions {
    public static final Instruction SET_WIDE = (i) -> {
        i.setWidth(true);
        return i.getIP().incr(OPCODE);
    };

    public static final Instruction SET_NARROW = (i) -> {
        i.setWidth(false);
        i.setAH(0x00);
        return i.getIP().incr(OPCODE);
    };

    public static final Instruction PUSH_CS = (i) -> {
        i.pushByte(i.getCS());
        return i.getIP().incr(OPCODE);
    };

    public static final Instruction PUSH_DS = (i) -> {
        i.pushByte(i.getDS());
        return i.getIP().incr(OPCODE);
    };

    public static final Instruction POP_DS = (i) -> {
        i.setDS(i.popByte());
        return i.getIP().incr(OPCODE);
    };

    public static final Instruction CLEAR_CARRY = (i) -> {
        i.setCarryFlag(false);
        return i.getIP().incr(OPCODE);
    };

    public static final Instruction SET_CARRY = (i) -> {
        i.setCarryFlag(true);
        return i.getIP().incr(OPCODE);
    };

    public static final Instruction COPY_HEAP_3F_3E = (i) -> {
        final int value = Heap.get(Heap.NEXT_SPECIAL).read();
        Heap.get(Heap.RECENT_SPECIAL).write(value);
        return i.getIP().incr(OPCODE);
    };

    public static final Instruction FILL_BBOX = (i) -> {
        i.fillRectangle();
        return i.getIP().incr();
    };

    public static final Instruction NOOP = (i) -> i.getIP().incr(OPCODE);

    public static final Instruction SOFT_EXIT = Interpreter::finish;

    public static final Instruction HARD_EXIT = (i) -> {
        i.shutdown();
        return null;
    };

    public static List<Integer> getStringFromMemory(Interpreter i, Address addr) {
        final List<Integer> nameCh = new ArrayList<>();
        int pointer = addr.offset();
        int ch = 0x80;
        while ((ch & 0x80) > 0) {
            ch = i.memory().read(addr.segment(), pointer, 1);
            nameCh.add(ch);
            pointer++;
        }
        return nameCh;
    }

    public static Instruction decodeOpcode(Interpreter i, int opcode) {
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
            case 0x1d -> new CopyAutomapBuffer();
            case 0x1e -> Instructions.HARD_EXIT; // "kill executable" aka "you lost"
            case 0x1f -> Instructions.NOOP; // "read chunk table", which we don't need to do
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
            case 0x41 -> JumpIf.NOT_CARRY;
            case 0x42 -> JumpIf.CARRY;
            case 0x43 -> JumpIf.ABOVE;
            case 0x44 -> JumpIf.EQUAL;
            case 0x45 -> JumpIf.NOT_EQUAL;
            case 0x46 -> JumpIf.SIGN;
            case 0x47 -> JumpIf.NOT_SIGN;
            case 0x48 -> new TestAndSetHeapSign();
            case 0x49 -> new LoopBX();
            case 0x4a -> new LoopBXLimit();
            case 0x4b -> Instructions.SET_CARRY;
            case 0x4c -> Instructions.CLEAR_CARRY;
            case 0x4d -> new RandomAX();
            case 0x4e -> FlagInstruction.SET_AL_IMM;
            case 0x4f -> FlagInstruction.CLEAR_AL_IMM;
            case 0x50 -> FlagInstruction.TEST_AL_IMM;
            case 0x51 -> new ArrayMax();
            case 0x52 -> JumpIf.ALWAYS;
            case 0x53 -> new Call();
            case 0x54 -> new Return();
            case 0x55 -> new PopAX();
            case 0x56 -> new PushAX();
            case 0x57 -> new LongJump();
            case 0x58 -> new LongCall();
            case 0x59 -> new LongReturn();
            case 0x5a -> Instructions.SOFT_EXIT; // "stop executing instruction stream"
            case 0x5b -> new EraseSquareSpecial();
            case 0x5c -> new RecurseOverParty();
            case 0x5d -> new LoadAXPartyAttribute();
            case 0x5e -> new StoreAXPartyAttribute();
            case 0x5f -> new SetPartyFlag();
            case 0x60 -> new ClearPartyFlag();
            case 0x61 -> new TestPartyFlag();
            case 0x62 -> new SearchPartySkill();
            case 0x63 -> new RecurseOverInventory();
            case 0x64 -> new PickUpItem();
            case 0x65 -> new SearchSpecialItem();
            case 0x66 -> new TestHeap();
            case 0x67 -> new DropItem();
            case 0x68 -> new ReadInventoryWord();
            case 0x69 -> new WriteInventoryWord();
            case 0x6a -> new IsPartyInBox();
            case 0x6b -> new TakeOneStep(true);
            case 0x6c -> new TakeOneStep(false);
            case 0x6d -> new DrawAutomap();
            case 0x6e -> new DrawCompass();
            case 0x6f -> new RotateMapView();
            case 0x70 -> new UnrotateMapView();
            case 0x71 -> new RunBoardEvent();
            case 0x72 -> new FindBoardAction();
            case 0x73 -> Instructions.COPY_HEAP_3F_3E;
            case 0x74 -> new DrawModal();
            case 0x75 -> (j) -> { j.resetUI(); return j.getIP().incr(); };
            case 0x76 -> Instructions.FILL_BBOX;
            case 0x77 -> DecodeStringFrom.CS_WITH_FILL;
            case 0x78 -> DecodeStringFrom.CS;
            case 0x79 -> DecodeStringFrom.DS_WITH_FILL;
            case 0x7a -> DecodeStringFrom.DS;
            case 0x7b -> DecodeStringFrom.CS_TO_TITLE;
            case 0x7c -> DecodeStringFrom.DS_TO_TITLE;
            case 0x7d -> new IndirectCharName();
            case 0x7e -> new IndirectCharItem();
            case 0x7f -> new IndirectString();
            case 0x80 -> new IndentAX();
            case 0x81 -> new Print4DigitNumber();
            case 0x82 -> new Print9DigitNumber();
            case 0x83 -> new IndirectChar();
            case 0x84 -> new AllocateTempSegment();
            case 0x85 -> new FreeSegmentAL();
            case 0x86 -> new LoadChunkAX();
            case 0x87 -> new PersistChunk();
            case 0x88 -> new WaitForEscapeKey();
            case 0x89 -> new ReadKeySwitch();
            case 0x8a -> new ShowMonsterImage();
            case 0x8b -> new DrawCurrentViewport(i);
            case 0x8c -> new RunYesNoModal();
            case 0x8d -> new ReadInputString();
            case 0x8e -> Instructions.NOOP;
            case 0x8f -> new StrToInt();
            case 0x90 -> new PlaySoundEffect();
            case 0x91 -> new DrawPartyInfoArea();
            case 0x92 -> new PauseUntilKeyOrTime(i);
            case 0x93 -> new PushBL();
            case 0x94 -> new PopBL();
            case 0x95 -> new SetCursor();
            case 0x96 -> new IndentToBBox();
            case 0x97 -> new ReadPCField();
            case 0x98 -> new WritePCField();
            case 0x99 -> new TestAX();
            case 0x9a -> new LoadHeapOnes();
            case 0x9b -> FlagInstruction.SET_IMM_IMM;
            case 0x9c -> FlagInstruction.CLEAR_IMM_IMM;
            case 0x9d -> FlagInstruction.TEST_IMM_IMM;
            case 0x9e -> new GetSegmentSize();
            case 0x9f -> new YouWin();
            default -> throw new IllegalArgumentException(String.format("Unimplemented opcode %02x", opcode));
        };
    }
}
