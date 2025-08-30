package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class ShowMonsterImage implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        // TODO 0x4a80
        return i.getIP().incr(OPCODE);
    }
}
