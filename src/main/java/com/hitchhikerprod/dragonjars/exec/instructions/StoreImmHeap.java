package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class StoreImmHeap implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final int offset = i.getIP().offset();
        final int heapIndex = i.readByte(offset + 1);
        final int value = (i.isWide()) ? i.readWord(offset + 2) : i.readByte(offset + 2);
        i.setHeap(heapIndex, value);
        return i.getIP().incr(OPCODE + IMMEDIATE + wordSize(i));
    }
}
