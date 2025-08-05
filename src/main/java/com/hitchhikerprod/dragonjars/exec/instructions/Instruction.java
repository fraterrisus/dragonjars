package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public interface Instruction {
    int OPCODE = 1;
    int IMMEDIATE = 1;
    int ADDRESS = 2;
    int RECTANGLE = 4;

    Address exec(Interpreter i);

    default int wordSize(Interpreter i) {
        return (i.isWide()) ? 2 : 1;
    }
}
