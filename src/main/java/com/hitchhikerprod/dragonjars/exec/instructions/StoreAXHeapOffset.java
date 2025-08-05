package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class StoreAXHeapOffset implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final int offset = i.getIP().offset();
        final int heapIndex = i.readByte(offset + 1) + i.getBL();
        i.setHeap(heapIndex, i.getAX());
        return i.getIP().incr(OPCODE + IMMEDIATE);
    }
}
