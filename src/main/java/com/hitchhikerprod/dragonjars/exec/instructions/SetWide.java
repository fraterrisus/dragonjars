package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class SetWide implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        i.setWidth(true);
        return i.getIP().incr(OPCODE);
    }
}
