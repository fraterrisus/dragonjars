package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class PopAX implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        int value = i.pop();
        if (i.isWide()) {
            value = value | (i.pop() << 8);
        }
        i.setAX(value);
        return i.getIP().incr(OPCODE);
    }
}
