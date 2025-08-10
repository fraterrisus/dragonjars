package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class AndAXHeap implements Instruction {
    // ax:2 <- ax:2 & heap[imm]:2 (ignores width)
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int heapIndex = i.readByte(ip.incr(1));
        final int op1 = i.getHeapBytes(heapIndex, 2);
        final int op2 = i.getAX(true);
        final int result = op1 & op2;
        i.setAX(result, true);
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
