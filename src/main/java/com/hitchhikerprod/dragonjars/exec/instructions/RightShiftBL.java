package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class RightShiftBL implements Instruction {
    // Always operates on the BL byte, regardless of width setting
    @Override
    public Address exec(Interpreter i) {
        i.setBL(i.getBL() >> 1);
        return i.getIP().incr();
    }
}
