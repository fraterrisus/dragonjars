package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class PushAX implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final int value = i.getAX();
        i.push(value & 0x000000ff);
        if (i.isWide()) {
            i.push((value & 0x0000ff00) >> 8);
        }
        return i.getIP().incr(OPCODE);
    }
}
