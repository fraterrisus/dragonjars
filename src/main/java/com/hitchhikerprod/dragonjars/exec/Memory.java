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
        segmentTable.setSegment(0, garbageChunk, 0xffff, 0x1, Frob.FROZEN);
        segmentTable.setSegment(Interpreter.PARTY_SEGMENT, partyChunk, 0xffff, 0x0e00, Frob.FROZEN);
    }

    public Chunk getCodeChunk() {
        return codeChunk;
    }

    public ModifiableChunk automapChunk() {
        return automapChunk;
    }

    /**
     * Allocates a segment for a chunk ID, writes the frob, and returns the segment ID. If the chunk is already present
     * in the segment table, updates the frob and returns the existing segment ID.
     *
     * @param chunkId     The desired chunk ID
     * @param frob        The desired new value of the segment's frob
     * @return The segment ID for the chunk, which may or may not be newly allocated
     */
    public synchronized int allocateSegment(int chunkId, Frob frob) {
        int segmentId = segmentTable.findSegmentId(chunkId);
        if (segmentId == -1 || (chunkId & 0x8000) > 0) {
            segmentId = segmentTable.getFreeSegmentId();
            // Make a copy of the chunk data for the new segment, so the data we read from the file stays pristine.
            // This is a closer emulation to how the engine runs.
            final ModifiableChunk newChunk = new ModifiableChunk(dataChunks.get(chunkId));
            segmentTable.setSegment(segmentId, newChunk, chunkId, newChunk.getSize(), frob);
        }
        if (segmentTable.getSegment(segmentId).frob() == Frob.FROZEN && frob != Frob.FROZEN)
            System.err.println("Warning: overwriting Frob.FROZEN for segment " + segmentId + "\n");
        segmentTable.setSegmentFrob(segmentId, frob);
        return segmentId;
    }

    /* There are an awful lot of calls to getSegmentForChunk followed immediately by getChunk. We could introduce a new
     * call that made those atomic, but some callers need both return values. */

    /**
     * Allocates a segment for a temporary buffer of the requested size. The temporary segment will have chunk ID 0xffff
     * which makes it very difficult to look up by chunk ID.
     *
     * @param bufferSize Size of the buffer in bytes
     * @return The segment ID of the new segment
     */
    public synchronized int allocateTemporary(int bufferSize) {
        final int segmentId = segmentTable.getFreeSegmentId();
        final ModifiableChunk tempChunk = new ModifiableChunk(new byte[bufferSize]);
        segmentTable.setSegment(segmentId, tempChunk, 0xffff, tempChunk.getSize(), Frob.IN_USE);
        return segmentId;
    }

    /**
     * Marks the segment related to a chunk as available to be unloaded.
     * This method is much safer than #freeSegment, because it looks up the chunk-to-segment relationship first.
     *
     * @param chunkId The Chunk ID to mark for unloading.
     */
    public synchronized void freeChunk(int chunkId) {
        final int segmentId = segmentTable.findSegmentId(chunkId);
        if (segmentId != -1) segmentTable.freeSegment(segmentId);
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
        segmentTable.freeSegment(segmentId);
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
        return segmentTable.getSegment(segmentId).size();
    }

    /**
     * Returns the segment ID matching this chunk ID, or -1 if the chunk is not in the segment table.
     */
    public synchronized int lookupChunkId(int chunkId) {
        return segmentTable.findSegmentId(chunkId);
    }

    public synchronized int read(int segmentId, int offset, int length) {
        return getChunk(segmentId).read(offset, length);
    }

    public synchronized int read(Address address, int length) {
        return read(address.segment(), address.offset(), length);
    }

    public synchronized List<Byte> readBytes(Address address, int length) {
        return getChunk(address.segment()).getBytes(address.offset(), length);
    }

    public synchronized void write(int segmentId, int offset, int length, int data) {
        getChunk(segmentId).write(offset, length, data);
    }

    public synchronized void write(Address address, int length, int data) {
        write(address.segment(), address.offset(), length, data);
    }

    public synchronized void writeBytes(Address address, List<Byte> data) {
        getChunk(address.segment()).setBytes(address.offset(), data);
    }
}
