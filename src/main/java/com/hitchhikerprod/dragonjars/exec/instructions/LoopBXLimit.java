package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class LoopBXLimit implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int limit = i.readByte(ip.incr(1));
        final int address = i.readWord(ip.incr(2));
        final int value = (i.getBL() + 1) & 0xff;
        i.setBL(value);
        if (value == limit) {
            return ip.incr(OPCODE + IMMEDIATE + ADDRESS);
        } else {
            return new Address(ip.segment(), address);
        }
    }
}
