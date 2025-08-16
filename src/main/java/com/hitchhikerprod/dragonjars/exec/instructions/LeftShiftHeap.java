package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class LeftShiftHeap implements Instruction {
    // Reads the entire word, performs SHL (always shifts in zeroes), writes one or two bytes
    // Technically this shifts the high bit into real CF, but doesn't write meta CF.
    // This instruction never gets called in DragonWars' code, and it kinda shows.
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int heapIndex = i.memory().read(ip.incr(1), 1);
        final int value = i.heap().read(heapIndex, 2) << 1;
        i.writeHeap(heapIndex, value);
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
