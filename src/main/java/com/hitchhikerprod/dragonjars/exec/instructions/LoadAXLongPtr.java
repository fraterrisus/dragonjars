package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class LoadAXLongPtr implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int heapIndex = i.readByte(ip.incr(1));
        final int offset = i.getHeapBytes(heapIndex, 2) + i.getBX(true);
        final int segmentId = i.getHeapBytes(heapIndex + 2, 1);
        System.out.format("  ax <- [s:%02x,a:%08x]\n", segmentId, offset);
        final int value = i.readWord(segmentId, offset);
        i.setAX(value);
        if (!i.isWide()) i.setAH(0x00);
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
