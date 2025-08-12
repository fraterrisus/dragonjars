package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class DecodeTitleStringDS implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address pointer = new Address(i.getDS(), i.getAX(true));
        Instructions.decodeTitleString(i, pointer);
        return i.getIP().incr();
    }
}
