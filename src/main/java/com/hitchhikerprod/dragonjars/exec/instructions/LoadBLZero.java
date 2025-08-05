package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class LoadBLZero implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        i.setBL(0);
        return i.getIP().incr(OPCODE);
    }
}
