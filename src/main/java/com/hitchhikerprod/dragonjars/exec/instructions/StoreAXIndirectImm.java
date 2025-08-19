package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class StoreAXIndirectImm implements Instruction {
    // ds[heap[imm:1]:2 + imm:1]:w <- ax:w
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int index = i.memory().read(ip.incr(1), 1);
        final int addr = i.heap(index).read(2) + i.memory().read(ip.incr(2), 1);
        final int value = i.getAX(true);
        i.memory().write(i.getDS(), addr, i.isWide() ? 2 : 1, value);
        return ip.incr(OPCODE + IMMEDIATE + IMMEDIATE);
    }
}
