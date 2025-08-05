package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class SetNarrow implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        i.setWidth(false);
        return i.getIP().incr(OPCODE);
    }
}
