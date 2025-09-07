package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Frob;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class LoadChunkAX implements Instruction {
    // input: AX = chunk ID to load
    // output: AX = segment ID
    @Override
    public Address exec(Interpreter i) {
        final int chunkId = i.getAL();
        final int segmentId = i.getSegmentForChunk(chunkId, Frob.IN_USE);
        i.setAX(segmentId, true);
        return i.getIP().incr();
    }
}
