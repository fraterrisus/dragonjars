package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class FlagTestImm implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int op = i.memory().read(ip.incr(1), 1);
        final int mask = 0x80 >> (op & 0x7);
        final int heapIndex = (op >> 3) + i.memory().read(ip.incr(2), 1);
        final int value = Heap.get(heapIndex).read(1) & mask;
        i.setCarryFlag(false);
        i.setZeroFlag(value == 0x00);
        i.setSignFlag(value == 0x80);
        return ip.incr(OPCODE + IMMEDIATE + IMMEDIATE);
    }
}
