package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class RandomAX implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final int maximum = i.getAX(true);
        final int value = (int)(Math.random() * maximum);
        i.setAX(value);
        return i.getIP().incr();
    }
}
