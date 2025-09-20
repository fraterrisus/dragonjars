package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class OrAXHeap implements Instruction {
    // ax:w <- ax:w | heap[imm]:w
    // ah <- 0 if narrow
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int heapIndex = i.memory().read(ip.incr(1), 1);
        final int op1 = Heap.get(heapIndex).read(2);
        final int op2 = i.getAX(true);
        final int result = op1 | op2;
        i.setAX(result);
        if (!i.isWide()) i.setAH(0x00);
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
