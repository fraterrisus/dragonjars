package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class ClearPartyFlag implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int charId = i.getHeapBytes(0x06, 1);
        final int charBaseAddress = i.getHeapBytes(0x0a + charId, 1) << 8;
        final int offset = (i.getAL() >> 3) + i.readByte(ip.incr(1));
        final int value = i.readData(Interpreter.PARTY_SEGMENT, charBaseAddress + offset, 1);
        final int bitmask = 0x80 >> (i.getAL() & 0x07);

        final int result = value & ~bitmask;
        i.writeData(Interpreter.PARTY_SEGMENT, charBaseAddress + offset, 1, result);

        return ip.incr(OPCODE + IMMEDIATE);
    }
}
