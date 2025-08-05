package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class LoadBLHeap implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final int offset = i.getIP().offset();
        final int heapIndex = i.readByte(offset + 1);
        final int heapValue = i.getHeapByte(heapIndex);
        i.setBL(heapValue);
        return i.getIP().incr(OPCODE + IMMEDIATE);
    }
}
