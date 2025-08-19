package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class StoreZeroHeap implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int heapIndex = i.memory().read(ip.incr(1), 1);
        i.heap(heapIndex).write(0x0000, i.isWide() ? 2 : 1);
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
