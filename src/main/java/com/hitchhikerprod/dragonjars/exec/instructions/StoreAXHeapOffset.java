package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class StoreAXHeapOffset implements Instruction {
    // heap[imm + bx]:w <- ax:w
    // except heap addresses are one byte, so why do we read BX instead of BL?
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int heapIndex = i.readByte(ip.incr(1)) + i.getBL();
        System.out.format("  heap[i=%02x + bl=%02x] <- ax=%04x\n", i.readByte(ip.incr(1)), i.getBL(), i.getAX(true));
        i.setHeap(heapIndex, i.getAX());
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
