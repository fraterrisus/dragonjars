package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class TestAndSetHeapSign implements Instruction {
    // Test heap[imm] bit 0x80
    // if 1, ZF=0
    // if 0, ZF=1 and set the bit

    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int heapIndex = i.memory().read(ip.incr(1), 1);
        final int value = Heap.get(heapIndex).read(1);
        if ((value & 0x80) == 0) {
            Heap.get(heapIndex).write(value | 0x80);
            i.setZeroFlag(true);
        } else {
            i.setZeroFlag(false);
        }
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
