package com.hitchhikerprod.dragonjars.tasks;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.ChunkTable;
import com.hitchhikerprod.dragonjars.data.HuffmanDecoder;
import com.hitchhikerprod.dragonjars.data.ModifiableChunk;
import javafx.concurrent.Task;

import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public class LoadDataTask extends Task<List<Chunk>> {
    public static final String MAGIC_STRING = "Created on an Apple ][ GS. Apple ][ Forever!";

    private final String executablePath;
    private final String data1Path;
    private final String data2Path;

    public LoadDataTask(String executablePath, String data1Path, String data2Path) {
        this.executablePath = executablePath;
        this.data1Path = data1Path;
        this.data2Path = data2Path;
    }

    @Override
    protected List<Chunk> call() throws Exception {
        final List<Chunk> chunks = new ArrayList<>();
        try (
            final RandomAccessFile exec = new RandomAccessFile(executablePath, "r");
            final RandomAccessFile data1 = new RandomAccessFile(data1Path, "r");
            final RandomAccessFile data2 = new RandomAccessFile(data2Path, "r")
        ) {
            final ChunkTable table = new ChunkTable(data1, data2);
            final int count = table.getChunkCount();

            for (int chunkId = 0; chunkId < count; chunkId++) {
                if (isCancelled()) return null;
                updateMessage("Loading segment " + (chunkId + 1) + " of " + count + 1);
                ModifiableChunk newChunk = new ModifiableChunk(table.readChunk(chunkId));

                if (newChunk.getSize() != 0) {
                    if (chunkId >= 0x18) {
                        // compressed chunks
                        updateMessage("Decompressing segment " + (chunkId + 1) + " of " + count + 1);
                        final HuffmanDecoder huffman = new HuffmanDecoder(newChunk);
                        newChunk = new ModifiableChunk(huffman.decode());
                        if (chunkId <= 0x1d) {
                            // full-screen image chunks
                            updateMessage("Decoding segment " + (chunkId + 1) + " of " + count + 1);
                            applyRollingXor(newChunk);
                        }
                        if (chunkId >= 0x100) {
                            // sound files
                            updateMessage("Decoding segment " + (chunkId + 1) + " of " + count + 1);
                            if (chunkId % 2 != 0) applyRollingAddition(newChunk, 0x0000);
                            applyRollingAddition(newChunk, 0x0004);
                        }
                    }
                }

                for (Patch p : PATCHES) {
                    if (p.chunkId() == chunkId) {
                        final byte oldValue = newChunk.getByte(p.address());
                        if (p.oldValue() == oldValue) {
                            newChunk.write(p.address(), 1, p.newValue());
                        } else {
                            System.err.println("Patch failed to apply " + p);
                        }
                    }
                }

                chunks.add(newChunk);
                updateProgress(chunkId + 1, count + 1);
            }

            updateMessage("Loading code segment " + (count + 1) + " of " + (count + 1));
            final int codeSize = (int) (exec.length());
            if ((long) codeSize != exec.length()) {
                throw new RuntimeException("Executable is too big for one byte array");
            }
            final byte[] codeSegment = new byte[codeSize];
            exec.readFully(codeSegment);

            final byte[] checkString = MAGIC_STRING.getBytes();
            for (int i = 0; i < checkString.length; i++) {
                if (codeSegment[0x08 + i] != checkString[i]) {
                    throw new RuntimeException("DRAGON.COM does not appear to be valid");
                }
            }

            final Chunk codeChunk = new Chunk(codeSegment);
            chunks.add(codeChunk);
            updateProgress(count + 1, count + 1);

            updateMessage("Finished.");
        }
        return chunks;
    }

    private static void applyRollingAddition(ModifiableChunk chunk, int baseIndex) {
        int pointer = baseIndex;
        int running = 0;
        while (pointer < chunk.getSize()) {
            running = running + chunk.getUnsignedByte(pointer);
            chunk.write(pointer, 1, running);
            pointer++;
        }
    }

    private static void applyRollingXor(ModifiableChunk chunk) {
        int readAddress = 0x00;
        int writeAddress = 0xa0;
        while (writeAddress < chunk.getSize()) {
            final int b0 = chunk.getQuadWord(readAddress);
            final int b1 = chunk.getQuadWord(writeAddress);
//            System.out.format("%04x\n", writeAddress);
            chunk.write(writeAddress, 4, b0 ^ b1);
            readAddress += 4;
            writeAddress += 4;
        }
    }

    private record Patch(int chunkId, int address, byte oldValue, byte newValue) {}

    // BUGFIX
    private static final List<Patch> PATCHES = List.of(
            // Purgatory: if you leave to the north, it drops you 2E of where you should be
            new Patch(0x047, 0x1696, (byte)0x0f, (byte)0x0d),
            // Dwarf Ruins: move the Dwarven Hammer chest flag from [9e:80] to [9a:08]
            new Patch(0x055, 0x02d7, (byte)0x28, (byte)0x0c),
            // Pilgrim dock exits are _all_ messed up
            new Patch(0x060, 0x02bf, (byte)0x13, (byte)0x12),
            new Patch(0x060, 0x02c1, (byte)0x13, (byte)0x12),
            new Patch(0x060, 0x02c2, (byte)0x14, (byte)0x15),
            new Patch(0x060, 0x02c5, (byte)0x11, (byte)0x12),
            // Slave Estate: move the Statue of Mog flag from [9f:10] to [a5:80]
            new Patch(0x06b, 0x041b, (byte)0x33, (byte)0x60)
    );
}
