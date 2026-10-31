package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class FreeSegmentAL implements Instruction {
    // input: AL = segment ID
    @Override
    public Address exec(Interpreter i) {
        final int segmentId = i.getAL();
        // Technically these guards shouldn't be needed, because segments 0 and 1 have Frob.FROZEN and freeSegment
        // has its own guard for segment ID 0xff.
        if (segmentId > 0x01 && segmentId != 0xff) {
            i.memory().freeSegment(segmentId);
        }
        return i.getIP().incr();
    }
}
