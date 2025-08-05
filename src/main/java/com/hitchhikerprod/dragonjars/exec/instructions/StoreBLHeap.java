package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class StoreBLHeap implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final int offset = i.getIP().offset();
        final int index = i.readByte(offset + 1);
        final int value = i.getBL();
        i.setHeap(index, value);
        return i.getIP().incr(OPCODE + IMMEDIATE);
    }
}
