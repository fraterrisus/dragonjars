package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class NoOp implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        return i.getIP().incr(OPCODE);
    }
}
