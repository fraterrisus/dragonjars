package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class MoveHeap implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final int offset = i.getIP().offset();
        final int heapReadIndex = i.readByte(offset + 1);
        final int heapWriteIndex = i.readByte(offset + 2);
        i.setHeap(heapWriteIndex, i.getHeap(heapReadIndex));
        return i.getIP().incr(OPCODE + IMMEDIATE + IMMEDIATE);
    }
}
