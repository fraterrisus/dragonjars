package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class PopDS implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        i.setDS(i.pop());
        return i.getIP().incr(OPCODE);
    }
}
