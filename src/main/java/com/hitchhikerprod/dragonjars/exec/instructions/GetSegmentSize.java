package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class GetSegmentSize implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final int segmentID = i.getAL();
        final int segmentSize = i.memory().getSegmentSize(segmentID);
        i.setAX(segmentSize, true);
        return i.getIP().incr();
    }
}
