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

        @Override
        public String toString() {
            return String.format("Segment[frob=%6s, chunkId=0x%04x, size=%5d, chunk=0x%08x]",
                    frob.name(), chunkId, size, System.identityHashCode(chunk));
        }
    }

    public static final int PC_STR_CURRENT = 0x0c;
    public static final int PC_DEX_CURRENT = 0x0e;
    public static final int PC_INT_CURRENT = 0x10;
    public static final int PC_SPR_CURRENT = 0x10;
    public static final int PC_HEALTH_CURRENT = 0x14;
    public static final int PC_HEALTH_MAX = 0x16;
    public static final int PC_STUN_CURRENT = 0x18;
    public static final int PC_POWER_CURRENT = 0x1c;
    public static final int PC_SKILL_BANDAGE = 0x25;
    public static final int PC_STATUS = 0x4c;
    public static final int PC_STATUS_DEAD = 0x01; // bitmask
    public static final int PC_AV = 0x59;
    public static final int PC_DV = 0x5a;
    public static final int PC_AC = 0x5b;
    public static final int PC_SUMMONED_LIFESPAN = 0x66;
    public static final int PC_SUMMONED_TICKS = 0x68;
    public static final int PC_INVENTORY = 0xec;

    private final Chunk codeChunk; // contents of DRAGON.COM binary
    private final ModifiableChunk automapChunk; // 0xd1b0
    private final List<Chunk> dataChunks;

    private final List<Segment> segments = new ArrayList<>();

    private int lastOpenSegmentIdx = 0;

    public Memory(Chunk codeChunk, List<Chunk> dataChunks) {
        this.codeChunk = codeChunk;
        this.dataChunks = dataChunks;
        this.automapChunk = new ModifiableChunk(new byte[0x700]);
    }

    public Chunk getCodeChunk() {
        return codeChunk;
    }

    public ModifiableChunk copyDataChunk(int chunkId) {
        return new ModifiableChunk(dataChunks.get(chunkId));
    }

    public ModifiableChunk automapChunk() {
        return automapChunk;
    }

    /**
     * Returns the index of the first free segment. This will either be the index of an existing segment with frob 0,
     * or an index equal to the size of the data structure, which can be used to add a new segment.
     */
    public int getFreeSegmentId() {
        int candidateSegmentIdx = (lastOpenSegmentIdx + 1) % segments.size();
        while (candidateSegmentIdx != lastOpenSegmentIdx) {
            final Segment candidateSegment = segments.get(candidateSegmentIdx);
            if (candidateSegment.frob() == Frob.GONE || candidateSegment.frob() == Frob.FREE) {
                lastOpenSegmentIdx = candidateSegmentIdx;
                return candidateSegmentIdx;
            }
            candidateSegmentIdx = (candidateSegmentIdx + 1) % segments.size();
        }
        return segments.size();
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

    /**
     * Returns the chunk ID associated with the given segment ID.
     */
    public int getSegmentChunk(int segmentId) {
        return segments.get(segmentId).chunkId();
    }

    /**
     * Returns the current frob for the given segment ID.
     */
    public Frob getSegmentFrob(int segmentId) {
        return segments.get(segmentId).frob();
    }

    public void setSegmentFrob(int segmentId, Frob frob) {
        segments.set(segmentId, segments.get(segmentId).withFrob(frob));
    }

    /**
     * Returns the size of the given segment ID.
     */
    public int getSegmentSize(int segmentId) {
        return segments.get(segmentId).size();
    }

    /**
     * Returns the segment ID matching this chunk ID, or -1 if the chunk is not in the segment table.
     */
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

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append("Segments:\n");
        for (Segment segment : segments) {
            sb.append("  ").append(segment).append("\n");
        }
        return sb.toString();
    }
}
