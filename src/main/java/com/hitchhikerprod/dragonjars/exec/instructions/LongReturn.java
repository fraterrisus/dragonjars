package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Frob;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class LongReturn implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        i.popByte(); // frob, unused here but needed for mirroring
        final int segmentId = i.popByte();
        final int address = i.popWord();
        // i.memory().setSegmentFrob(segmentId, Frob.FREE); // [cs/4012]
        i.setDS(segmentId);
        return new Address(segmentId, address);
    }
}
