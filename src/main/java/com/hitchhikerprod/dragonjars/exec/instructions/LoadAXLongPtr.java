package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class LoadAXLongPtr implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int heapIndex = i.readByte(ip.incr(1));
        final int chunkOffset = i.getHeapBytes(heapIndex, 2) + i.getBX(true);
        // FIXME this isn't a chunk ID, it's a segment ID. And segment 0x01 (at least)
        //   was pre-allocated as a large temporary buffer.
        final int chunkId = i.getHeapBytes(heapIndex + 2, 1);
        System.out.format("  ax <- [%02x,%08x]\n", chunkId, chunkOffset);
        final int chunkData = i.readWord(chunkId, chunkOffset);
        i.setAX(chunkData);
        if (!i.isWide()) i.setAH(0x00);
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
