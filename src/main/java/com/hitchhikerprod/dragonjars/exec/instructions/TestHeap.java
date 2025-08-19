package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class TestHeap implements Instruction {
    // Sets ZF and SF, leaves CF as it was
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int heapIndex = i.memory().read(ip.incr(1), 1);
        if (i.isWide()) {
            final int value = i.heap(heapIndex).read(2);
            i.setZeroFlag(value == 0x0000);
            i.setSignFlag((value & 0x8000) > 0);
        } else {
            final int value = i.heap(heapIndex).read(1);
            i.setZeroFlag(value == 0x00);
            i.setSignFlag((value & 0x80) > 0);
        }
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
