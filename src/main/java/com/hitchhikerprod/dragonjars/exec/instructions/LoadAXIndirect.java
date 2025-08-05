package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class LoadAXIndirect implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int heapIndex = i.readWord(ip.incr(1));
        final int addr = i.getHeap(heapIndex) + i.getBX(true);
        final int value = i.readWord(i.getDS(), addr);
        i.setAX(value);
        if (!i.isWide()) i.setAH(0x00);
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
