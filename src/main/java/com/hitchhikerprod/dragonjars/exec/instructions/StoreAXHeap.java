package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class StoreAXHeap implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int heapIndex = i.memory().read(ip.incr(1), 1);
        int val = i.getAX();
        i.heap(heapIndex).write(val, i.isWide() ? 2 : 1);
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
