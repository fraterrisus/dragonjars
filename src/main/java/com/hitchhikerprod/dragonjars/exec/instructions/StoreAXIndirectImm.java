package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class StoreAXIndirectImm implements Instruction {
    // ds[heap[imm:1]:2 + imm:1]:w <- ax:w
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int index = i.readByte(ip.incr(1));
        final int addr = i.getHeapWord(index) + i.readByte(ip.incr(2));
        final int value = i.getAX(true);
        i.writeWidth(i.getDS(), addr, value);
        return ip.incr(OPCODE + IMMEDIATE + IMMEDIATE);
    }
}
