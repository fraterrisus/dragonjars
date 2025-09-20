package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class FlagSetAL implements Instruction {
    // 0x4e (set), 0x4f (clr), 0x50 (tst):
    //   AL <- 0x80 >> AL[2:0]
    //   BL <- AL[7:3] + imm:1
    // 0x9b (set), 0x9c (clr), 0x9d (tst):
    //   t  <- imm:1
    //   AL <- 0x80 >> t[2:0]
    //   BL <- t[7:3] + imm:1
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int op = i.getAL();
        final int mask = 0x80 >> (op & 0x7);
        final int heapIndex = (op >> 3) + i.memory().read(ip.incr(1), 1);
        Heap.get(heapIndex).modify(1, x -> x | mask);
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
