package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class StoreImmHeap implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int heapIndex = i.readByte(ip.incr(1));
        final int value = (i.isWide()) ? i.readWord(ip.incr(2)) : i.readByte(ip.incr(2));
        i.setHeap(heapIndex, value);
        return i.getIP().incr(OPCODE + IMMEDIATE + wordSize(i));
    }
}
