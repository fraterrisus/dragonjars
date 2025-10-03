package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class LoadAXHeapOffset implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        // I *think* this can't go over 256, so reading BL is okay
        final int heapIndex = i.memory().read(ip.incr(1), 1) + i.getBL();
        final int value = Heap.get(heapIndex).read(i.width());
        i.setAX(value);
        if (!i.isWide()) i.setAH(0x00);
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
