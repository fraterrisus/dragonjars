package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class StoreAX implements Instruction {
    // ds[imm:2]:w <- ax:w
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int addr = i.readWord(ip.incr(1));
        final int value = i.getAX(true);
        i.writeWidth(i.getDS(), addr, value);
        return ip.incr(OPCODE + ADDRESS);
    }
}
