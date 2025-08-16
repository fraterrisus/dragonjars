package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class DecHeap implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int heapIndex = i.readByte(ip.incr(1));
        final int value = i.heap().read(heapIndex, 2) - 1;
        i.writeHeap(heapIndex, value);
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
