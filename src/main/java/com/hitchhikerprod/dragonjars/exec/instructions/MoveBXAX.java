package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class MoveBXAX implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final int value = i.getBX(true);
        final boolean width = i.isWide();
        i.setWidth(true);
        i.setAX(value);
        i.setWidth(width);
        return i.getIP().incr(OPCODE);
    }
}
