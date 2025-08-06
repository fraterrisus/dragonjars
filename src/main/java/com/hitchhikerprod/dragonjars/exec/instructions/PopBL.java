package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class PopBL implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        i.setBL(i.pop());
        return i.getIP().incr(OPCODE);
    }
}
