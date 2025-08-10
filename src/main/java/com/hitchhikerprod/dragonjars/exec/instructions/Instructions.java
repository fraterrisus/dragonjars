package com.hitchhikerprod.dragonjars.exec.instructions;

import static com.hitchhikerprod.dragonjars.exec.instructions.Instruction.OPCODE;

public class Instructions {
    public static final Instruction SET_WIDE = (i) -> {
        i.setWidth(true);
        return i.getIP().incr(OPCODE);
    };

    public static final Instruction SET_NARROW = (i) -> {
        i.setWidth(false);
        i.setAL(0x00); // ???????
        return i.getIP().incr(OPCODE);
    };

    public static final Instruction PUSH_CS = (i) -> {
        i.push(i.getCS());
        return i.getIP().incr(OPCODE);
    };

    public static final Instruction PUSH_DS = (i) -> {
        i.push(i.getDS());
        return i.getIP().incr(OPCODE);
    };

    public static final Instruction POP_DS = (i) -> {
        i.setDS(i.pop());
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

    public static final Instruction COPY_HEAP_3E_3F = (i) -> {
        i.setHeapBytes(0x3e, 1, i.getHeapBytes(0x3f, 1));
        return i.getIP().incr(OPCODE);
    };

    public static final Instruction NOOP = (i) -> i.getIP().incr(OPCODE);

    public static final Instruction EXIT = (i) -> null;
}