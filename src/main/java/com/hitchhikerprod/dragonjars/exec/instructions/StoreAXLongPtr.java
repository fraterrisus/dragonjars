package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class StoreAXLongPtr implements Instruction {
    // longptr[heap[imm:1]:3 + bx:2]:w <- ax:w
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int index = i.readByte(ip.incr(1));
        final int addr = i.getHeapWord(index) + i.getBX(true);
        final int chunk = i.getHeapByte(index + 2);
        final int value = i.getAX(true);
        i.writeWidth(chunk, addr, value);
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
