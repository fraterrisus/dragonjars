package com.hitchhikerprod.dragonjars.exec;

import com.hitchhikerprod.dragonjars.data.ModifiableChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

class SegmentTable {
    private static final boolean DEBUG = false;

    private final List<Segment> segments = new ArrayList<>();
    private int lastOpenSegmentIdx = 0;

    // The original engine tracks the segment size in a separate column. For us, the segment size and the chunk size
    // are always the same (see the special case handling of segment 0 in the Memory constructor) so in the *one* case
    // where we need the segment size we can just ask the Chunk for it.
    record Segment(ModifiableChunk chunk, int chunkId, Frob frob) {
        Segment withFrob(Frob newFrob) {
            return new Segment(chunk, chunkId, newFrob);
        }

        @Override
        public String toString() {
            return String.format("Segment[frob=%6s, chunkId=0x%04x, size=0x%04x, chunk=0x%08x]",
                    frob.name(), chunkId, chunk.getSize(), System.identityHashCode(chunk));
        }
    }

    /**
     * Look up the Segment ID for a given Chunk ID.
     * @param chunkId ID of the desired Chunk
     * @return ID of the segment containing that chunk, or -1 if the requested Chunk is not in the table
     */
    int findSegmentId(int chunkId) {
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
    Segment getSegment(int segmentId) {
        return Objects.requireNonNull(segments.get(segmentId));
    }

    private int findFreeSlot() {
        if (segments.isEmpty()) return 0;
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

    private void writeSegment(int segmentId, ModifiableChunk chunk, int chunkId, Frob frob) {
        final int c = Integer.compare(segmentId, segments.size());
        if (c > 0) {
            throw new IllegalArgumentException("Segment ID " + segmentId + " is too large");
        } else if (c == 0) {
            if (DEBUG) {
                System.out.format("Segment %02d (chunk 0x%02x) -> %s\n",
                        segments.size(), chunkId, frob);
            }
            segments.add(new Segment(chunk, chunkId, frob));
        } else {
            if (DEBUG) {
                System.out.format("Segment %02d (chunk 0x%02x) -> %s\n",
                        segmentId, chunkId, frob);
            }
            segments.set(segmentId, new Segment(chunk, chunkId, frob));
        }
    }

    /**
     * Add a new segment to the table. The table will be searched for an appropriate slot in which to put the new
     * segment, which may either be a new slot or a slot containing a segment marked for unloading.
     *
     * @param chunk The (writeable) data for the segment
     * @param chunkId The Chunk ID to associate with the segment
     * @param frob The new frob value for the segment
     * @return The ID of the assigned segment
     */
    int newSegment(ModifiableChunk chunk, int chunkId, Frob frob) {
        final int segmentId = findFreeSlot();
        writeSegment(segmentId, chunk, chunkId, frob);
        return segmentId;
    }

    /**
     * Update the Frob on a segment without changing anything else. You shouldn't call this on a FROZEN segment,
     * but if you do it will work (and spew a console message saying you shouldn't do that).
     *
     * @param segmentId ID of the segment to update
     * @param frob new Frob value
     */
    void setFrob(int segmentId, Frob frob) {
        final SegmentTable.Segment segment = segments.get(segmentId);
        if (segment.frob() != frob) {
            if (DEBUG || segment.frob() == Frob.FROZEN) {
                System.out.format("Segment %02d (chunk 0x%02x) %s -> %s\n",
                        segmentId, segment.chunkId(), segment.frob(), frob);
            }
            segments.set(segmentId, segment.withFrob(frob));
        }
    }

    /**
     * Mark a segment as FREE (able to be overwritten). If you call this on a FROZEN segment, no update occurs.
     *
     * @param segmentId ID of the segment to update
     */
    void free(int segmentId) {
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
