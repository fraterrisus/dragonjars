package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class ClearPartyFlag implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int charId = i.heap(Heap.SELECTED_PC).read();
        final int charBaseAddress = i.heap(Heap.MARCHING_ORDER + charId).read() << 8;
        final int offset = (i.getAL() >> 3) + i.memory().read(ip.incr(1), 1);
        final int value = i.memory().read(Interpreter.PARTY_SEGMENT, charBaseAddress + offset, 1);
        final int bitmask = 0x80 >> (i.getAL() & 0x07);

        final int result = value & ~bitmask;
        i.memory().write(Interpreter.PARTY_SEGMENT, charBaseAddress + offset, 1, result);

        return ip.incr(OPCODE + IMMEDIATE);
    }
}
