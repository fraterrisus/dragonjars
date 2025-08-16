package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class MoveData implements Instruction {
    // ds[imm:2]:w -> ds[imm:2]:w
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int sourceAddress = i.memory().read(ip.incr(1), 2);
        final int destinationAddress = i.memory().read(ip.incr(3), 2);
        final int segmentId = i.getDS();
        final int value = i.memory().read(segmentId, sourceAddress, 2);
        i.memory().write(segmentId, destinationAddress, i.isWide() ? 2 : 1, value);
        return ip.incr(OPCODE + ADDRESS + ADDRESS);
    }
}
