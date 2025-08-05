package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class MoveHeap implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int heapReadIndex = i.readByte(ip.incr(1));
        final int heapWriteIndex = i.readByte(ip.incr(2));
        i.setHeap(heapWriteIndex, i.getHeap(heapReadIndex));
        return ip.incr(OPCODE + IMMEDIATE + IMMEDIATE);
    }
}
