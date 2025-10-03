package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class MoveHeap implements Instruction {
    // heap[imm:1]:w -> heap[imm:1]:w
    // note ordering; first IMM is read address, second is write address
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int heapReadIndex = i.memory().read(ip.incr(1), 1);
        final int heapWriteIndex = i.memory().read(ip.incr(2), 1);
        final int value = Heap.get(heapReadIndex).read(i.width());
        Heap.get(heapWriteIndex).write(value, i.width());
        return ip.incr(OPCODE + IMMEDIATE + IMMEDIATE);
    }
}
