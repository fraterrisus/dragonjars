package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import com.hitchhikerprod.dragonjars.exec.Segment;

public class AllocateTempSegment implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final int bufferSize = i.getAX(true);
        final Segment segment = i.memory().allocateTemporary(bufferSize);
        i.setAX(segment.id(), true);
        return i.getIP().incr(OPCODE);
    }
}
