package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class LoadAXLongPtr implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int heapIndex = i.readByte(ip.incr(1));
        final int chunkOffset = i.getHeapWord(heapIndex) + i.getBX(true);
        final int chunkId = i.getHeapByte(heapIndex + 2);
        final int chunkData = i.readWord(chunkId, chunkOffset);
        i.setAX(chunkData);
        if (!i.isWide()) i.setAH(0x00);
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
