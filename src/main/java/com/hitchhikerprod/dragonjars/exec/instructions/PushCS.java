package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class PushCS implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        i.push(i.getCS());
        return i.getIP().incr(OPCODE);
    }
}
