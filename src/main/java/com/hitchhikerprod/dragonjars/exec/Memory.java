package com.hitchhikerprod.dragonjars.exec;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.ModifiableChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

public class Memory {
    private record Segment(ModifiableChunk chunk, int chunkId, int size, Frob frob) {
        private Segment withFrob(Frob newFrob) {
            return new Segment(chunk, chunkId, size, newFrob);
        }
    }

    private final Chunk codeChunk; // contents of DRAGON.COM binary
    private final List<Chunk> dataChunks;
    private final List<Segment> segments = new ArrayList<>();

    public Memory(Chunk codeChunk, List<Chunk> dataChunks) {
        this.codeChunk = codeChunk;
        this.dataChunks = dataChunks;
    }

    public Chunk getCodeChunk() {
        return codeChunk;
    }

    public ModifiableChunk copyDataChunk(int chunkId) {
        return new ModifiableChunk(dataChunks.get(chunkId));
    }

    /**
     * Returns the index of the first free segment. This will either be the index of an existing segment with frob 0,
     * or an index equal to the size of the data structure, which can be used to add a new segment.
     */
    public int getFreeSegmentId() {
        return IntStream.range(0, segments.size())
                .filter(i -> segments.get(i).frob() == Frob.EMPTY)
                .findFirst()
                .orElse(segments.size());
    }

    public ModifiableChunk getSegment(int segmentId) {
        return Objects.requireNonNull(segments.get(segmentId)).chunk();
    }

    public void addSegment(ModifiableChunk chunk, int chunkId, int size, Frob frob) {
        segments.add(new Segment(chunk, chunkId, size, frob));
    }

    public void setSegment(int segmentId, ModifiableChunk chunk, int chunkId, int size, Frob frob) {
        if (segmentId > segments.size()) {
            throw new IllegalArgumentException("Segment ID " + segmentId + " is too large");
        } else if (segmentId == segments.size()) {
            addSegment(chunk, chunkId, size, frob);
        } else {
            segments.set(segmentId, new Segment(chunk, chunkId, size, frob));
        }
    }

    public int getSegmentChunk(int segmentId) {
        return segments.get(segmentId).chunkId();
    }

    public Frob getSegmentFrob(int segmentId) {
        return segments.get(segmentId).frob();
    }

    public void setSegmentFrob(int segmentId, Frob frob) {
        segments.set(segmentId, segments.get(segmentId).withFrob(frob));
    }

    public int getSegmentSize(int segmentId) {
        return segments.get(segmentId).size();
    }

    public int lookupChunkId(int chunkId) {
        return IntStream.range(0, segments.size())
                .filter(i -> segments.get(i).chunkId() == chunkId)
                .findFirst()
                .orElse(-1);
    }

    public int read(int segmentId, int offset, int length) {
        return getSegment(segmentId).read(offset, length);
    }

    public int read(Address address, int length) {
        return read(address.segment(), address.offset(), length);
    }

    public List<Byte> readList(Address address, int length) {
        return getSegment(address.segment()).getBytes(address.offset(), length);
    }

    public void write(int segmentId, int offset, int length, int data) {
        getSegment(segmentId).write(offset, length, data);
    }

    public void write(Address address, int length, int data) {
        write(address.segment(), address.offset(), length, data);
    }

    public void writeList(Address address, List<Byte> data) {
        getSegment(address.segment()).setBytes(address.offset(), data);
    }
}
