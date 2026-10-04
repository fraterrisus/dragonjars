package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Frob;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class LongReturn implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final boolean unload = (0x00 != i.popByte());
        final int currentSegmentId = i.getIP().segment();
        final int targetSegmentId = i.popByte();
        final int targetAddress = i.popWord();
        if (unload) i.freeSegment(currentSegmentId); // [cs/4012]
        i.setDS(-1); // set to same as CS
        return new Address(targetSegmentId, targetAddress);
    }
}
