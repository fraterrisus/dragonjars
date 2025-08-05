package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class IncHeap implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int heapIndex = i.readByte(ip.incr(1));
        final int value = i.getHeapWord(heapIndex) + 1;
        final boolean width = i.isWide();
        i.setWidth(true);
        i.setHeap(heapIndex, value);
        i.setWidth(width);
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
