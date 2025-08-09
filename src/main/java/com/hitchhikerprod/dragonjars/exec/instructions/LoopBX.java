package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class LoopBX implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int address = i.readWord(ip.incr(1));
        final int value = (i.getBL() - 1) & 0xff;
        i.setBL(value);
        if (value == 0xff) {
            return ip.incr(OPCODE + ADDRESS);
        } else {
            return new Address(ip.chunk(), address);
        }
    }
}
