package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Frob;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class LongReturn implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final int segmentId = i.pop();
        final int address = (i.pop() << 8) | (i.pop());
        i.unloadSegment(i.getIP().segment());
        i.memory().setFrob(segmentId, Frob.DIRTY);
        i.setDS(segmentId);
        return new Address(segmentId, address);
    }
}
