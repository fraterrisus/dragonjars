package com.hitchhikerprod.dragonjars.exec;

import com.hitchhikerprod.dragonjars.data.ModifiableChunk;

public record Segment(int id, int chunkId, ModifiableChunk chunk, Frob frob) {
    @Override
    public String toString() {
        return String.format("Segment[segmentId=%d, chunkId=0x%02x, frob=%s]", id, chunkId, frob);
    }
}
