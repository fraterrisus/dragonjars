package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.ALU;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class DecBL implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        i.setBL(ALU.decByte(i.getBL()));
        return i.getIP().incr();
    }
}
