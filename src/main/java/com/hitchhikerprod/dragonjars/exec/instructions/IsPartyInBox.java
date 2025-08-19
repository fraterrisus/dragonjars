package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class IsPartyInBox implements Instruction {
    // ZF <- 0 if party's (x,y) is outside boundary, ZF <- 1 if on or inside
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int yParty = i.heap(0x00).read();
        final int xParty = i.heap(0x01).read();
        final int yMin = i.memory().read(ip.incr(1), 1);
        final int xMin = i.memory().read(ip.incr(2), 1);
        final int yMax = i.memory().read(ip.incr(3), 1);
        final int xMax = i.memory().read(ip.incr(4), 1);
        i.setZeroFlag(yParty >= yMin && yParty <= yMax && xParty >= xMin && xParty <= xMax);
        return ip.incr(OPCODE + RECTANGLE);
    }
}

/* cmp 0x08, 0x03 -> JB is NOT taken (i.e. 8 is not below 3) */