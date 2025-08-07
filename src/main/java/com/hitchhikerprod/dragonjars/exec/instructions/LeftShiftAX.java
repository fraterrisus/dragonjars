package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class LeftShiftAX implements Instruction {
    // Uses the weird "overwrite AH with 0x00 in narrow mode" semantics.
    @Override
    public Address exec(Interpreter i) {
        final int value = i.getAX(true) << 1;
        i.setAX(value);
        if (!i.isWide()) i.setAH(0x00);
        return i.getIP().incr();
    }
}
