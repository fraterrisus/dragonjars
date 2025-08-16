package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class FreeSegmentAL implements Instruction {
    // input: AL = segment ID
    @Override
    public Address exec(Interpreter i) {
        final int segmentId = i.getAL();
        if (segmentId > 0x01 && segmentId != 0xff) {
            i.freeSegment(segmentId);
        }
        return i.getIP().incr();
    }
}
