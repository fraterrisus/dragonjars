package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class IndentToBBox implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        i.drawStringBuffer();
        i.indentToBBox();
        return i.getIP().incr();
    }
}
