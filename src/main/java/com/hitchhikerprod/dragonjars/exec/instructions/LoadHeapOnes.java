package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class LoadHeapOnes implements Instruction {
    // heap[imm]:w <- 0xff(ff)
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int heapIndex = i.readByte(ip.incr(1));
        i.setHeap(heapIndex, 0xffff);
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
