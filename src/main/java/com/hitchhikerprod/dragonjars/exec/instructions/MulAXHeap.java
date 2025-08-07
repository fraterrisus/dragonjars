package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class MulAXHeap implements Instruction {
    // heap[37]:4 <- heap[imm:1]:4 * ax:2
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int heapIndex = i.readByte(ip.incr(1));
        final int op1 = i.getHeapBytes(heapIndex, 4);
        final int op2 = i.getAX(true);
        i.setHeapBytes(0x37, 4, op1 * op2);
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
