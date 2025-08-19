package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class StoreAXPartyAttribute implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int charId = i.heap(0x06).read();
        final int attributeOffset = i.memory().read(ip.incr(1), 1);
        final int address = (charId << 8) | attributeOffset;
        i.memory().write(Interpreter.PARTY_SEGMENT, address, (i.isWide()) ? 2 : 1, i.getAX());
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
