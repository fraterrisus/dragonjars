package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class IncAX implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final int value = i.getAX(true) + 1;
        final boolean width = i.isWide();
        /* A weird one; it runs `and ah, byte[wide]` which seems to mask AH down to 0 in Narrow mode */
        i.setWidth(true);
        i.setAX(width ? value : value & 0x000000ff);
        i.setWidth(width);
        return i.getIP().incr(OPCODE);
    }
}
