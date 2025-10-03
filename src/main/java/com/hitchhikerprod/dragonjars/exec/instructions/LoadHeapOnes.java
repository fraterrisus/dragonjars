package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class LoadHeapOnes implements Instruction {
    // heap[imm]:w <- 0xff(ff)
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int heapIndex = i.memory().read(ip.incr(1), 1);
        Heap.get(heapIndex).write(0xffff, i.width());
//        System.out.format("  heap[%02x] <- %s\n", heapIndex, (i.isWide()) ? "0xffff" : "0xff");
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
