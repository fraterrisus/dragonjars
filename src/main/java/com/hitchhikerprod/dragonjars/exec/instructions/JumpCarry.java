package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class JumpCarry implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int address = i.readWord(ip.incr(1));
        final boolean jump = i.getCarry();
        if (jump) {
            return new Address(ip.chunk(), address);
        } else {
            return ip.incr(OPCODE + ADDRESS);
        }
    }
}
