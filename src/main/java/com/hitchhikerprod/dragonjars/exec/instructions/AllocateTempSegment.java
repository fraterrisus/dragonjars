package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class AllocateTempSegment implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final int bufferSize = i.getAX(true);
        final int segmentId = i.memory().allocateTemporary(bufferSize);
        i.setAX(segmentId, true);
        return i.getIP().incr(OPCODE);
    }
}
