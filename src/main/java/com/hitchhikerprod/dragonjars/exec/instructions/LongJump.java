package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class LongJump implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int chunkId = i.readByte(ip.incr(1));
        final int address = i.readWord(ip.incr(2));
        i.unloadChunk(ip.chunk());
        i.loadChunk(chunkId);
        return new Address(chunkId, address);
    }
}
