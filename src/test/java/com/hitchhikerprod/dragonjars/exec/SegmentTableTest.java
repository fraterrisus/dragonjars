package com.hitchhikerprod.dragonjars.exec;

import com.hitchhikerprod.dragonjars.data.ModifiableChunk;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SegmentTableTest {
    @Test
    public void testRoundTrip() {
        final SegmentTable table = new SegmentTable();
        final ModifiableChunk chunk = new ModifiableChunk(new byte[0x10]);

        // Write a new segment
        final int segmentId = table.newSegment(chunk, 0x4, Frob.IN_USE);
        // Write a second new segment just so there are multiple things in the table
        table.newSegment(new ModifiableChunk(new byte[0x10]), 0x6, Frob.IN_USE);
        // Read back the first segment
        final SegmentTable.Segment segment = table.getSegment(segmentId);

        assertEquals(chunk, segment.chunk());
        assertEquals(0x4, segment.chunkId());
    }

    @Test
    public void testFindFreeSlot() {
        final SegmentTable table = new SegmentTable();
        final int chk4 = table.newSegment(new ModifiableChunk(new byte[0x10]), 0x4, Frob.IN_USE);
        final int chk7 = table.newSegment(new ModifiableChunk(new byte[0x10]), 0x7, Frob.IN_USE);
        final int chk2 = table.newSegment(new ModifiableChunk(new byte[0x10]), 0x2, Frob.IN_USE);

        // If we free a segment and then allocate a new one, it should find the just-freed slot.
        table.free(chk7);
        final int chk9 = table.newSegment(new ModifiableChunk(new byte[0x10]), 0x9, Frob.IN_USE);

        assertEquals(chk7, chk9);
    }

    @Test
    public void testGetSegmentThrows() {
        final SegmentTable table = new SegmentTable();
        assertThrows(IndexOutOfBoundsException.class, () -> table.getSegment(5));
    }

    @Test
    public void testFindSegmentId() {
        final SegmentTable table = new SegmentTable();
        final ModifiableChunk chunk = new ModifiableChunk(new byte[0x10]);
        final int segmentId = table.newSegment(chunk, 0x4, Frob.IN_USE);

        // Pass the same chunk ID, get the same segment ID
        assertEquals(segmentId, table.findSegmentId(0x4));
        // Pass a non-existent chunk ID, get -1
        assertEquals(-1, table.findSegmentId(0x3));
    }

    @Test
    public void testSetFrob() {
        final SegmentTable table = new SegmentTable();
        final ModifiableChunk chunk = new ModifiableChunk(new byte[0x10]);
        final int segmentId = table.newSegment(chunk, 0x4, Frob.IN_USE);
        assertEquals(Frob.IN_USE, table.getSegment(segmentId).frob());

        table.setFrob(segmentId, Frob.FREE);

        final SegmentTable.Segment segment = table.getSegment(segmentId);
        assertEquals(Frob.FREE, segment.frob());
        assertEquals(0x4, segment.chunkId());
        assertEquals(chunk, segment.chunk());
    }

    @Test
    public void testSetFrobWontOverwriteFrozen() {
        final SegmentTable table = new SegmentTable();
        final ModifiableChunk chunk = new ModifiableChunk(new byte[0x10]);
        final int segmentId = table.newSegment(chunk, 0x4, Frob.FROZEN);
        assertEquals(Frob.FROZEN, table.getSegment(segmentId).frob());

        table.setFrob(segmentId, Frob.IN_USE);

        final SegmentTable.Segment segment = table.getSegment(segmentId);
        assertEquals(Frob.FROZEN, segment.frob());
        assertEquals(0x4, segment.chunkId());
        assertEquals(chunk, segment.chunk());
    }

    @Test
    public void testFree() {
        final SegmentTable table = new SegmentTable();
        final ModifiableChunk chunk = new ModifiableChunk(new byte[0x10]);
        final int segmentId = table.newSegment(chunk, 0x4, Frob.IN_USE);
        assertEquals(Frob.IN_USE, table.getSegment(segmentId).frob());

        table.free(segmentId);

        final SegmentTable.Segment segment = table.getSegment(segmentId);
        assertEquals(Frob.FREE, segment.frob());
        assertEquals(0x4, segment.chunkId());
        assertEquals(chunk, segment.chunk());
    }

    @Test
    public void testFreeWontOverwriteFrozen() {
        final SegmentTable table = new SegmentTable();
        final ModifiableChunk chunk = new ModifiableChunk(new byte[0x10]);
        final int segmentId = table.newSegment(chunk, 0x4, Frob.FROZEN);
        assertEquals(Frob.FROZEN, table.getSegment(segmentId).frob());

        table.free(segmentId);

        final SegmentTable.Segment segment = table.getSegment(segmentId);
        assertEquals(Frob.FROZEN, segment.frob());
        assertEquals(0x4, segment.chunkId());
        assertEquals(chunk, segment.chunk());
    }
}
