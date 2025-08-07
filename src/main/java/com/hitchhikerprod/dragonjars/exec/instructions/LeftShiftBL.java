package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class LeftShiftBL implements Instruction {
    // Straightforward: just runs SHL on the BL byte.
    @Override
    public Address exec(Interpreter i) {
        i.setBL((i.getBL() << 1) & 0xff);
        return i.getIP().incr();
    }
}
