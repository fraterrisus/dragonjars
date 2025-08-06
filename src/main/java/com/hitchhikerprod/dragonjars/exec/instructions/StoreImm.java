package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class StoreImm implements Instruction {
    // ds[imm:2]:w <- imm:w
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int address = i.readWord(ip.incr(1));
        final int value = i.readWord(ip.incr(3));
        i.writeWidth(i.getDS(), address, value);
        return ip.incr(OPCODE + ADDRESS + wordSize(i));
    }
}
