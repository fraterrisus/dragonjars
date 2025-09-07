package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.ModifiableChunk;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Frob;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class AllocateTempSegment implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final int bufferSize = i.getAX(true);
        final ModifiableChunk tempChunk = new ModifiableChunk(new byte[bufferSize]);
        final int segmentId = i.memory().getFreeSegmentId();
        i.memory().setSegment(segmentId, tempChunk, 0xffff, bufferSize, Frob.IN_USE);
        i.setAX(segmentId, true);
        return i.getIP().incr(OPCODE);
    }
}
