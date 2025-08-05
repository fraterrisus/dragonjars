package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class LoadAXLongPtr implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final int offset = i.getIP().offset();
        final int heapIndex = i.readByte(offset + 1);
        final int chunkId = i.getHeapByte(heapIndex);
        final int chunkOffset = i.getHeapWord(heapIndex + 1) + i.getBX(true);
        final int chunkData = i.getChunk(chunkId).getWord(chunkOffset);
        i.setAX(chunkData);
        return i.getIP().incr(OPCODE + IMMEDIATE);
    }
}
