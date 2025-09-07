package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Frob;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class LongJump implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int chunkId = i.memory().read(ip.incr(1), 1);
        final int address = i.memory().read(ip.incr(2), 2);
        final int segmentId = i.getSegmentForChunk(chunkId, Frob.IN_USE);
        return new Address(segmentId, address);
    }
}
