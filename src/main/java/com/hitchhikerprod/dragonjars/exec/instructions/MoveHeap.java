package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class MoveHeap implements Instruction {
    // heap[imm:1]:w -> heap[imm:1]:w
    // note ordering; first IMM is read address, second is write address
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int heapReadIndex = i.readByte(ip.incr(1));
        final int heapWriteIndex = i.readByte(ip.incr(2));
        final int value = i.readHeap(heapReadIndex);
        i.writeHeap(heapWriteIndex, value);
        return ip.incr(OPCODE + IMMEDIATE + IMMEDIATE);
    }
}
