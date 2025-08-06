package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class AddAXHeap implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int carryIn = (i.getCarryFlag()) ? 1 : 0;
        final int heapIndex = i.readByte(ip.incr(1));
        final int value = i.getHeapWord(heapIndex);
        if (i.isWide()) {
            final int newValue = i.getAX() + value + carryIn;
            i.setAX(newValue);
            i.setCarryFlag((newValue & 0xffff0000) > 0);
        } else {
            final int newValue = i.getAL() + (value & 0x000000ff) + carryIn;
            i.setAL(newValue);
            i.setCarryFlag((newValue & 0xffffff00) > 0);
        }
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
