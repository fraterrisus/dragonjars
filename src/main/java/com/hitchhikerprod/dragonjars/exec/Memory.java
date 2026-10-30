package com.hitchhikerprod.dragonjars.exec;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.ModifiableChunk;

import java.util.List;

public class Memory {
    public static final int PC_STR_CURRENT = 0x0c;
    public static final int PC_DEX_CURRENT = 0x0e;
    public static final int PC_INT_CURRENT = 0x10;
    public static final int PC_SPR_CURRENT = 0x12;
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
    private final List<Chunk> dataChunks;

    private final ModifiableChunk automapChunk; // 0xd1b0

    private final SegmentTable segmentTable;

    public Memory(Chunk codeChunk, List<Chunk> dataChunks) {
        this.codeChunk = codeChunk;
        this.dataChunks = dataChunks;
        this.segmentTable = new SegmentTable();

        // The original engine stores this data in a blank spot in the middle of the in-memory executable. In theory
        // we could do that too (store the data in codeChunk) but this seems nicer.
        this.automapChunk = new ModifiableChunk(new byte[0x700]);

        // Create segment 0 (a garbage segment with size 1) and segment 1 (which stores party data). In the original
        // engine, the same buffer is passed to both setSegment calls, but the size on segment ID 0 is forced to one
        // byte. I don't think segment 0 ever actually gets used — doing so would almost certainly cause a segmentation
        // fault. Get it? That's an x86 joke for everyone out there reading the comments.
        final ModifiableChunk garbageChunk = new ModifiableChunk(new byte[0x1]);
        final ModifiableChunk partyChunk = new ModifiableChunk(new byte[0x0e00]);
        assert segmentTable.newSegment(garbageChunk, 0xffff, Frob.FROZEN) == 0x0;
        assert segmentTable.newSegment(partyChunk, 0xffff, Frob.FROZEN) == 0x1;
    }

    /**
     * Returns a reference to the Chunk of executable data.
     */
    public Chunk getCodeChunk() {
        return codeChunk;
    }

    /**
     * Returns a reference to the privately-allocated Chunk of automap data storage.
     */
    public ModifiableChunk automapChunk() {
        return automapChunk;
    }

    /**
     * Allocates a segment for a chunk ID, writes the frob, and returns the segment ID. If the chunk is already present
     * in the segment table, updates the frob and returns the existing segment ID.
     *
     * @param chunkId The chunk ID we want to load
     * @param frob The new segment's frob
     * @return The segment ID for the chunk, which may or may not be newly allocated
     */
    public synchronized int allocateSegment(int chunkId, Frob frob) {
        int segmentId = segmentTable.findSegmentId(chunkId);
        if (segmentId == -1 || (chunkId & 0x8000) > 0) {
            // Allocate a new segment.
            // Make a copy of the chunk data so the data we read from the file stays pristine.
            final ModifiableChunk newChunk = new ModifiableChunk(dataChunks.get(chunkId));
            return segmentTable.newSegment(newChunk, chunkId, frob);
        } else {
            // Update the frob on the existing segment.
            segmentTable.setFrob(segmentId, frob);
            return segmentId;
        }
    }

    /* There are an awful lot of calls to allocateSegment followed immediately by getChunk. We could introduce a new
     * call that made those atomic, but some callers need both return values. */

    /**
     * Allocates a segment for a temporary buffer of the requested size. The temporary segment will have chunk ID 0xffff
     * which makes it very difficult to look up by chunk ID.
     *
     * @param bufferSize Size of the buffer in bytes
     * @return The segment ID of the new segment
     */
    public synchronized int allocateTemporary(int bufferSize) {
        final ModifiableChunk tempChunk = new ModifiableChunk(new byte[bufferSize]);
        return segmentTable.newSegment(tempChunk, 0xffff, Frob.IN_USE);
    }

    /**
     * Marks the segment related to a chunk as available to be unloaded.
     * This method is much safer than #freeSegment, because it looks up the chunk-to-segment relationship first.
     *
     * @param chunkId The Chunk ID to mark for unloading.
     */
    public synchronized void freeChunk(int chunkId) {
        final int segmentId = segmentTable.findSegmentId(chunkId);
        if (segmentId != -1) segmentTable.free(segmentId);
    }

    /**
     * Marks a segment as available to be unloaded, but doesn't actually remove it. If a subsequent call to
     * #allocateSegment occurs for the same chunk ID, there's a chance that the existing segment will be revived
     * (rather than unloading and reloading it).
     * Note that this method is somewhat dangerous, in that it requires the caller to pass a segment ID while hopefully
     * knowing that it refers to the correct chunk.
     *
     * @param segmentId The Segment ID to mark for unloading.
     */
    public synchronized void freeSegment(int segmentId) {
        if (segmentId == 0xff) return;
        segmentTable.free(segmentId);
    }

    /**
     * Retrieve a Chunk from the segments table.
     * @param segmentId ID of the segment
     * @return a ModifiableChunk
     */
    public synchronized ModifiableChunk getChunk(int segmentId) {
        return segmentTable.getSegment(segmentId).chunk();
    }

    /**
     * Retrieve the Chunk ID for a given Segment ID.
     * @param segmentId ID of the segment
     * @return ID of the chunk
     */
    public synchronized int getChunkId(int segmentId) {
        return segmentTable.getSegment(segmentId).chunkId();
    }

    // This method only ever gets called for debugging purposes.
    public synchronized Frob getFrob(int segmentId) {
        return segmentTable.getSegment(segmentId).frob();
    }

    /**
     * Returns the size of the given segment ID.
     */
    public synchronized int getSize(int segmentId) {
        return segmentTable.getSegment(segmentId).chunk().getSize();
    }

    /**
     * Looks up the segment ID matching a given chunk ID.
     *
     * @param chunkId The chunk ID to locate
     * @return The segment ID, or -1 if not found.
     */
    public synchronized int lookupChunkId(int chunkId) {
        return segmentTable.findSegmentId(chunkId);
    }

    /**
     * Read some bytes from a memory location and translate them from little-endian into a Java integer.
     *
     * @param segmentId Segment ID from which to read.
     * @param offset Address from which to read.
     * @param length Number of bytes (1-4) of data to read.
     * @return int
     */
    public synchronized int read(int segmentId, int offset, int length) {
        return getChunk(segmentId).read(offset, length);
    }

    /**
     * Read some bytes from a memory location and translate them from little-endian into a Java integer.
     *
     * @param address Memory location from which to read.
     * @param length Number of bytes (1-4) of data to read.
     * @return int
     */
    public synchronized int read(Address address, int length) {
        return read(address.segment(), address.offset(), length);
    }

    /**
     * Read some bytes from a memory location.
     *
     * @param address Memory location from which to read.
     * @param length Number of bytes of data to read.
     * @return A list of Bytes, in the same order they are in the file.
     */
    public synchronized List<Byte> readBytes(Address address, int length) {
        return getChunk(address.segment()).getBytes(address.offset(), length);
    }

    /**
     * Write some data to a memory location.
     *
     * @param segmentId Segment ID to write.
     * @param offset Address to write.
     * @param length Number of bytes of data to write.
     * @param data A Java int containing the data to write; will be written in little-endian order.
     */
    public synchronized void write(int segmentId, int offset, int length, int data) {
        getChunk(segmentId).write(offset, length, data);
    }

    /**
     * Write some data to a memory location.
     *
     * @param address Memory location to write.
     * @param length Number of bytes (1-4) of data to write.
     * @param data A Java int containing the data to write; will be written in little-endian order.
     */
    public synchronized void write(Address address, int length, int data) {
        write(address.segment(), address.offset(), length, data);
    }

    /**
     * Write some data to a memory location.
     *
     * @param address Memory location to write.
     * @param data A list of Bytes to write; will be written in list order.
     */
    public synchronized void writeBytes(Address address, List<Byte> data) {
        getChunk(address.segment()).setBytes(address.offset(), data);
    }
}
