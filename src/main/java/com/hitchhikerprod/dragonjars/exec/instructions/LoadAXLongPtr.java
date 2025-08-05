package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class LoadAXLongPtr implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int heapIndex = i.readByte(ip.incr(1));
        final int chunkId = i.getHeapByte(heapIndex);
        final int chunkOffset = i.getHeapWord(heapIndex + 1) + i.getBX(true);
        final int chunkData = i.readWord(chunkId, chunkOffset);
        i.setAX(chunkData);
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
