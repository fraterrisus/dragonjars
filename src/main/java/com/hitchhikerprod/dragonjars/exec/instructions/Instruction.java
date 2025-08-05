package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public interface Instruction {
    int OPCODE = 1;
    int IMMEDIATE = 1;
    int ADDRESS = 2;
    int RECTANGLE = 4;

    Instruction SET_WIDE = (i) -> {
        i.setWidth(true);
        return i.getIP().incr(OPCODE);
    };

    Instruction SET_NARROW = (i) -> {
        i.setWidth(false);
        return i.getIP().incr(OPCODE);
    };

    Instruction NOOP = (i) -> i.getIP().incr(OPCODE);

    Address exec(Interpreter i);

    default int wordSize(Interpreter i) {
        return (i.isWide()) ? 2 : 1;
    }
}
