package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class DecodeTitleStringCS implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        return Instructions.decodeTitleString(i, i.getIP().incr(OPCODE));
    }
}
