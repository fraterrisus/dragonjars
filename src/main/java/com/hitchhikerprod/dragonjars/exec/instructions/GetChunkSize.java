package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class GetChunkSize implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final int chunkId = i.getAL();
        final int chunkSize = i.getChunk(chunkId).getSize();
        i.setAX(chunkSize, true);
        return i.getIP().incr();
    }
}
