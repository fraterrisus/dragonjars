package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class DrawPartyInfoArea implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        i.drawPartyInfoArea();
        return i.getIP().incr();
    }
}
