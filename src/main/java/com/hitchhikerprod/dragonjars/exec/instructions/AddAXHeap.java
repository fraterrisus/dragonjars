package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class AddAXHeap implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final int offset = i.getIP().offset();
        final int carryIn = (i.getCarry()) ? 1 : 0;
        final int heapIndex = i.readByte(offset + 1);
        final int value = i.getHeapWord(heapIndex);
        if (i.isWide()) {
            final int newValue = i.getAX() + value + carryIn;
            i.setAX(newValue);
            i.setCarry((newValue & 0xffff0000) > 0);
        } else {
            final int newValue = i.getAL() + (value & 0x000000ff) + carryIn;
            i.setAL(newValue);
            i.setCarry((newValue & 0xffffff00) > 0);
        }
        return i.getIP().incr(OPCODE + IMMEDIATE);
    }
}
