package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class StoreImmHeap implements Instruction {
    // heap[imm:1]:w <- imm:w
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int heapIndex = i.memory().read(ip.incr(1), 1);
        final int value = i.memory().read(ip.incr(2), 2);
        Heap.get(heapIndex).write(value, i.isWide() ? 2 : 1);
        return i.getIP().incr(OPCODE + IMMEDIATE + wordSize(i));
    }
}
