package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class PopAX implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final int value = (i.isWide()) ? i.popWord() : i.popByte();
        i.setAX(value);
        return i.getIP().incr(OPCODE);
    }
}
