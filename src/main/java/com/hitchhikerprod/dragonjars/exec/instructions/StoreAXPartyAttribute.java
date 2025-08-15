package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class StoreAXPartyAttribute implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int charId = i.getHeapBytes(0x06, 1);
        final int attributeOffset = i.readByte(ip.incr(1));
        final int address = (charId << 8) | attributeOffset;
        if (i.isWide()) {
            i.getSegment(1).setWord(address, i.getAX());
        } else {
            i.getSegment(1).setByte(address, i.getAL());
        }
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
