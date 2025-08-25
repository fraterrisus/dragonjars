package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class Print4DigitNumber implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final int value = i.getAX(true);
        Instructions.printNumber(i, value);
        return i.getIP().incr(OPCODE);
    }
}
