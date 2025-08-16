package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class StoreBLHeap implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int index = i.readByte(ip.incr(1));
        final int value = i.getBL();
        i.writeHeap(index, value);
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
