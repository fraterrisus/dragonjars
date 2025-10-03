package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class StoreAXHeapOffset implements Instruction {
    // heap[imm + bx]:w <- ax:w
    // except heap addresses are one byte, so why do we read BX instead of BL?
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int heapIndex = i.memory().read(ip.incr(1), 1) + i.getBL();
        int val = i.getAX();
        Heap.get(heapIndex).write(val, i.width());
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
