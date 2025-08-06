package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class LongCall implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int chunkId = i.readByte(ip.incr(1));
        final int address = i.readWord(ip.incr(2));
        final Address returnAddress = ip.incr(OPCODE + IMMEDIATE + ADDRESS);
        i.push(returnAddress.offset());
        i.push(returnAddress.offset() >> 8);
        i.push(returnAddress.chunk());
        return new Address(chunkId, address);
    }
}
