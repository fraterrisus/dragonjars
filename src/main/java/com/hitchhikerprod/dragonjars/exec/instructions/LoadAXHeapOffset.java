package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class LoadAXHeapOffset implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        // I *think* this can't go over 256, so reading BL is okay
        final Address ip = i.getIP();
        final int heapIndex = i.readByte(ip.incr(1)) + i.getBL();
        final int heapValue = i.getHeapWord(heapIndex);
        i.setAX(heapValue);
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
