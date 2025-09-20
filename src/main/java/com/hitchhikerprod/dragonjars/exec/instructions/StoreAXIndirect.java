package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class StoreAXIndirect implements Instruction {
    // ds[heap[imm:1]:2 + bx:2]:w <- ax:w
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int index = i.memory().read(ip.incr(1), 1);
        final int addr = Heap.get(index).read(2) + i.getBX(true);
        final int value = i.getAX(true);
        i.memory().write(i.getDS(), addr, i.isWide() ? 2 : 1, value);
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
