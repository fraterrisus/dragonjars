package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class StoreAXOffset implements Instruction {
    // ds[imm:2 + bx:2]:w <- ax:w
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int addr = i.memory().read(ip.incr(1), 2) + i.getBX(true);
        final int value = i.getAX(true);
        i.memory().write(i.getDS(), addr, i.width(), value);
        return ip.incr(OPCODE + ADDRESS);
    }
}
