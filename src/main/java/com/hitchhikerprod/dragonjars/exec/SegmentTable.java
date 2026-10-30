package com.hitchhikerprod.dragonjars.exec;

import com.hitchhikerprod.dragonjars.data.ModifiableChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

public class SegmentTable {
    private static final boolean DEBUG = false;

    private final List<Segment> segments = new ArrayList<>();
    private int lastOpenSegmentIdx = 0;

    protected record Segment(ModifiableChunk chunk, int chunkId, int size, Frob frob) {
        protected Segment withFrob(Frob newFrob) {
            return new Segment(chunk, chunkId, size, newFrob);
        }

        @Override
        public String toString() {
            return String.format("Segment[frob=%6s, chunkId=0x%04x, size=%5d, chunk=0x%08x]",
                    frob.name(), chunkId, size, System.identityHashCode(chunk));
        }
    }

    protected int getFreeSegmentId() {
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

    /**
     * Lookup the Segment ID for a given Chunk ID.
     * @param chunkId ID of the desired Chunk
     * @return ID of the segment containing that chunk, or -1 if the requested Chunk is not in the table
     */
    protected int findSegmentId(int chunkId) {
        return IntStream.range(0, segments.size())
                .filter(i -> segments.get(i).chunkId() == chunkId)
                .findFirst()
                .orElse(-1);
    }

    /**
     * Fetch a Segment from the table. Throws if the segment ID doesn't exist.
     * @param segmentId ID of the Segment
     * @return Segment
     */
    protected Segment getSegment(int segmentId) {
        return Objects.requireNonNull(segments.get(segmentId));
    }

    /**
     * Add a new Segment to the table.
     * @param chunk A ModifiableChunk containing the new segment data.
     * @param chunkId The ID of the new chunk.
     * @param size
     * @param frob
     */
    protected void addSegment(ModifiableChunk chunk, int chunkId, int size, Frob frob) {
        if (DEBUG) {
            System.out.format("Segment %02d (chunk 0x%02x) -> %s\n",
                    segments.size(), chunkId, frob);
        }
        segments.add(new Segment(chunk, chunkId, size, frob));
    }

    protected void setSegment(int segmentId, ModifiableChunk chunk, int chunkId, int size, Frob frob) {
        if (segmentId > segments.size()) {
            throw new IllegalArgumentException("Segment ID " + segmentId + " is too large");
        } else if (segmentId == segments.size()) {
            addSegment(chunk, chunkId, size, frob);
        } else {
            if (DEBUG) {
                System.out.format("Segment %02d (chunk 0x%02x) -> %s\n",
                        segmentId, chunkId, frob);
            }
            segments.set(segmentId, new Segment(chunk, chunkId, size, frob));
        }
    }

    protected void setSegmentFrob(int segmentId, Frob frob) {
        final SegmentTable.Segment segment = segments.get(segmentId);
        if (segment.frob() != frob) {
            if (DEBUG) {
                System.out.format("Segment %02d (chunk 0x%02x) -> %s\n",
                        segmentId, segment.chunkId(), frob);
            }
            segments.set(segmentId, segment.withFrob(frob));
        }
    }

    protected void freeSegment(int segmentId) {
        Segment segment = getSegment(segmentId);
        if (segment.frob() != Frob.FROZEN) {
            segments.set(segmentId, segment.withFrob(Frob.FREE));
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Segments:\n");
        for (Segment segment : segments) {
            sb.append("  ").append(segment).append("\n");
        }
        return sb.toString();
    }
}
