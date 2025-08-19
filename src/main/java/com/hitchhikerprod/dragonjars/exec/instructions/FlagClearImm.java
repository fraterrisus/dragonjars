package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class FlagClearImm implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int op = i.memory().read(ip.incr(1), 1);
        final int mask = 0x80 >> (op & 0x7);
        final int heapIndex = (op >> 3) + i.memory().read(ip.incr(2), 1);
        final int value = i.heap(heapIndex).read(1) & ~mask;
        i.heap(heapIndex).write(value);
        return ip.incr(OPCODE + IMMEDIATE + IMMEDIATE);
    }
}
