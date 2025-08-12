package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class StoreAXLongPtr implements Instruction {
    // longptr[heap[imm:1]:3 + bx:2]:w <- ax:w
    // The assembly doesn't check whether or not the segment's already been loaded, so good luck?
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int index = i.readByte(ip.incr(1));
        final int addr = i.getHeapBytes(index, 2) + i.getBX(true);
        final int segmentId = i.getHeapBytes(index + 2, 1);
        final int value = i.getAX(true);
        System.out.format("  [s:%02x,a:%08x] <- %04x\n", segmentId, addr, value);
        i.writeWidth(segmentId, addr, value);
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
