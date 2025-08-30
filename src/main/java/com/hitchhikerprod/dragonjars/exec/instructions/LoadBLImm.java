package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class LoadBLImm implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int value = i.memory().read(ip.incr(1), 1);
        i.setBL(value);
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
