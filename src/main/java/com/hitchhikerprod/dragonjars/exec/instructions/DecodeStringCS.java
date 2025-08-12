package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class DecodeStringCS implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        return Instructions.decodeString(i, i.getIP().incr(OPCODE));
    }
}
