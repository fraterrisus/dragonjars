package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class RightShiftHeap implements Instruction {
    // Loads the entire word, performs SHR, then only writes the first byte in Narrow mode.
    // This instruction never gets called in DragonWars' code, and it kinda shows.
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int heapIndex = i.memory().read(ip.incr(1), 1);
        i.heap(heapIndex).modify(i.isWide() ? 2 : 1, (val) -> val >> 1);
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
