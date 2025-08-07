package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class LoadAXHeapOffset implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        // I *think* this can't go over 256, so reading BL is okay
        final int heapIndex = i.readByte(ip.incr(1)) + i.getBL();
        final int value = i.getHeapBytes(heapIndex, 2);
        i.setAX(value);
        if (!i.isWide()) i.setAH(0x00);
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
