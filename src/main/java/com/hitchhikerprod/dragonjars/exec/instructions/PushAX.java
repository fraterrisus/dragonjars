package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class PushAX implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final int value = i.getAX();
        if (i.isWide()) {
            i.pushWord(value);
        } else {
            i.pushByte(value);
        }
        return i.getIP().incr(OPCODE);
    }
}
