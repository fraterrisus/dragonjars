package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class IndirectString implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address pointer = new Address(i.getDS(), i.getAX(true));
        Instructions.indirectFunction(i, pointer);
        return i.getIP().incr();
    }
}
