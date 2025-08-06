package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class MoveData implements Instruction {
    // ds[imm:2]:w -> ds[imm:2]:w
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int sourceAddress = i.readWord(ip.incr(1));
        final int destinationAddress = i.readWord(ip.incr(3));
        final int value = i.readWord(i.getDS(), sourceAddress);
        i.writeWidth(i.getDS(), destinationAddress, value);
        return ip.incr(OPCODE + ADDRESS + ADDRESS);
    }
}
