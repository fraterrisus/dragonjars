package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class TestPartyFlag implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int charId = i.heap(0x06).read();
        final int charBaseAddress = i.heap(0x0a + charId).read() << 8;
        final int offset = (i.getAL() >> 3) + i.memory().read(ip.incr(1), 1);
        final int value = i.memory().read(Interpreter.PARTY_SEGMENT, charBaseAddress + offset, 1);
        final int bitmask = 0x80 >> (i.getAL() & 0x07);

        final int result = value & bitmask;
        i.setZeroFlag(result == 0x00);
        i.setSignFlag((value & 0x80) > 0);

        return ip.incr(OPCODE + IMMEDIATE);
    }
}
