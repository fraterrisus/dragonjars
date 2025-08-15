package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class IndentAX implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int x = i.readByte(ip.incr(1));
        i.indentFromBbox(x);
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
