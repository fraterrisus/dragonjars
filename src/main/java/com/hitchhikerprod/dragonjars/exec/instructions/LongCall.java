package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Frob;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class LongCall implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int chunkId = i.memory().read(ip.incr(1), 1);
        final int address = i.memory().read(ip.incr(2), 2);
        final Address returnAddress = ip.incr(OPCODE + IMMEDIATE + ADDRESS);
        i.push(returnAddress.offset());
        i.push(returnAddress.offset() >> 8);
        i.push(returnAddress.segment());
        final int segmentId = i.getSegmentForChunk(chunkId, Frob.CLEAN);
        i.setDS(segmentId);
        return new Address(segmentId, address);
    }

    /* Assembly operation:
     * - lookup the destination chunk ID in the frobs table
     * - if the chunk ID is in the table and frob is not 0x02, we don't have to do anything (dl <- 0x00)
     * - otherwise, load the chunk into a free segment and set its frob to 0x01 (dl <- 0xff)
     * - push dl so we know how to reverse that step
     * - set seg1_idx _and_ seg2_idx to the new code segment (this was returned from the unpack() call)
     * - update the segment pointers (load seg1 from seg1_idx, etc.)
     * - si <- destination address, es <- new code segment
     */
}
