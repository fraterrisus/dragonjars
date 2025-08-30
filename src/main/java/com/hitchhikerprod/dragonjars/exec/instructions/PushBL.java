package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class PushBL implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        i.pushByte(i.getBL());
        return i.getIP().incr(OPCODE);
    }
}
