package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class LoadAXPartyAttribute implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int charId = i.heap().read(0x06, 1);
        final int attributeOffset = i.readByte(ip.incr(1));
        final int address = (charId << 8) | attributeOffset;
        final int value = i.readData(Interpreter.PARTY_SEGMENT, address, 2);
        i.setAX(value);
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
