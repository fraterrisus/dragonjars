package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class PopAX implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final int value;
        if (i.isWide()) {
            value = (i.pop() << 8) | (i.pop());
        } else {
            value = i.pop();
        }
        i.setAX(value);
        return i.getIP().incr(OPCODE);
    }
}
