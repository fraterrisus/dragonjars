package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class StoreImm implements Instruction {
    // ds[imm:2]:w <- imm:w
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int address = i.memory().read(ip.incr(1), 2);
        final int value = i.memory().read(ip.incr(3), 2);
        i.memory().write(i.getDS(), address, i.isWide() ? 2 : 1, value);
        return ip.incr(OPCODE + ADDRESS + wordSize(i));
    }
}
