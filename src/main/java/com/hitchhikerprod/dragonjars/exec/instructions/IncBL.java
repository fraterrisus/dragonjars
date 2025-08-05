package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class IncBL implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        i.setBL(i.getBL() + 1);
        return i.getIP().incr(OPCODE);
    }
}
