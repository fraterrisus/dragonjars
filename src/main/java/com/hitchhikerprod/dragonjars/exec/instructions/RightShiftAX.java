package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class RightShiftAX implements Instruction {
    // ALWAYS operates on the AX word, regardless of Width setting
    // SHR always shifts in zeroes from the left
    //   0x3ca7  shr word[mp.ax], 1
    @Override
    public Address exec(Interpreter i) {
        final boolean width = i.isWide();
        i.setWidth(true);
        final int value = i.getAX() >> 1;
        i.setAX(value);
        i.setWidth(width);
        return i.getIP().incr();
    }
}
