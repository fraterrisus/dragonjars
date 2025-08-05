package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class LoadBLImm implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final int offset = i.getIP().offset();
        final int value = i.readByte(offset + 1);
        i.setBL(value);
        return i.getIP().incr(OPCODE + IMMEDIATE);
    }
}
