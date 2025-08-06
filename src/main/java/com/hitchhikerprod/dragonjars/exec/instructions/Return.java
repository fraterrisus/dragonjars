package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class Return implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final int address = (i.pop() << 8) | (i.pop());
        return new Address(i.getIP().chunk(), address);
    }
}
