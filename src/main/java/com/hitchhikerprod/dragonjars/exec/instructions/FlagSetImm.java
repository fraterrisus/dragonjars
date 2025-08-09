package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class FlagSetImm implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int op = i.readByte(ip.incr(1));
        final int mask = 0x80 >> (op & 0x7);
        final int heapIndex = (op >> 3) + i.readByte(ip.incr(2));
        final int value = i.getHeapBytes(heapIndex, 1) | mask;
        i.setHeapBytes(heapIndex, 1, value);
        return ip.incr(OPCODE + IMMEDIATE + IMMEDIATE);
    }
}
