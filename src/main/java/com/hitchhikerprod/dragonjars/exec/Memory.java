package com.hitchhikerprod.dragonjars.exec;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.ModifiableChunk;

import java.util.ArrayList;
import java.util.List;

public class Memory {
    private final Chunk codeChunk; // contents of DRAGON.COM binary
    private final List<Chunk> dataChunks;

    private final List<ModifiableChunk> segments = new ArrayList<>();
    private final List<Integer> chunkSizes = new ArrayList<>();
    private final List<Integer> chunkFrobs = new ArrayList<>();
    private final List<Integer> chunkIds = new ArrayList<>();


    public Memory(Chunk codeChunk, List<Chunk> dataChunks) {
        this.codeChunk = codeChunk;
        this.dataChunks = dataChunks;
    }

    public Chunk getCodeChunk() {
        return codeChunk;
    }

    public ModifiableChunk copyChunk(int chunkId) {
        return new ModifiableChunk(dataChunks.get(chunkId));
    }

    public void addSegment(ModifiableChunk chunk, int chunkId, int size, Frob frob) {
        segments.add(chunk);
        chunkIds.add(chunkId);
        chunkSizes.add(size);
        chunkFrobs.add(frob.value());
    }

    public void setSegment(int segmentId, ModifiableChunk chunk, int chunkId, int size, Frob frob) {
        if (segmentId == segments.size()) {
            addSegment(chunk, chunkId, size, frob);
        } else {
            segments.set(segmentId, chunk);
            chunkIds.set(segmentId, chunkId);
            chunkSizes.set(segmentId, size);
            chunkFrobs.set(segmentId, frob.value());
        }
    }

    public Frob getFrob(int segmentId) {
        return Frob.of(chunkFrobs.get(segmentId));
    }

    public void setFrob(int segmentId, Frob frob) {
        chunkFrobs.set(segmentId, frob.value());
    }

    public int lookupChunkId(int chunkId) {
        return chunkIds.indexOf(chunkId);
    }

    /**
     * Returns the index of the first free segment. This will either be the index of an existing segment with frob 0,
     * or an index equal to the size of the data structure, which can be used to add a new segment.
     */
    public int getFreeSegmentId() {
        final int firstFreeSegmentId = chunkFrobs.indexOf(0x00);
        if (firstFreeSegmentId == -1) return chunkFrobs.size();
        return firstFreeSegmentId;
    }

    public ModifiableChunk getSegment(int segmentId) {
        return segments.get(segmentId);
    }
}
