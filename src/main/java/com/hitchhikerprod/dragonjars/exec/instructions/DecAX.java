package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class DecAX implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        i.setAX(i.getAX() - 1);
        if (!i.isWide()) i.setAH(0x00);
        return i.getIP().incr();
    }
}
