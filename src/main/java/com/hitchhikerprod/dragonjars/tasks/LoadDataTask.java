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
    @Override
    protected List<Chunk> call() throws Exception {
        final List<Chunk> chunks = new ArrayList<>();
        try (
            final RandomAccessFile exec = new RandomAccessFile("DRAGON.COM", "r");
            final RandomAccessFile data1 = new RandomAccessFile("DATA1", "r");
            final RandomAccessFile data2 = new RandomAccessFile("DATA2", "r")
        ) {
            final ChunkTable table = new ChunkTable(data1, data2);
            final int count = table.getChunkCount();

            for (int chunkId = 0; chunkId < count; chunkId++) {
                if (isCancelled()) return null;
                updateMessage("Loading segment " + (chunkId+1) + " of " + count+1);
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
                            // monster animation chunks
                            updateMessage("Decoding segment " + (chunkId + 1) + " of " + count + 1);
                            if (chunkId % 2 != 0) applyRollingAddition(newChunk, 0x0000);
                            applyRollingAddition(newChunk, 0x0004);
                        }
                    }
                }

                chunks.add(newChunk);
                updateProgress(chunkId+1, count+1);
            }

            updateMessage("Loading code segment " + count+1 + " of " + count+1);
            final int codeSize = (int)(exec.length());
            if ((long)codeSize != exec.length()) {
                throw new RuntimeException("Executable is too big for one byte array");
            }
            final byte[] codeSegment = new byte[codeSize];
            exec.readFully(codeSegment);
            chunks.add(new Chunk(codeSegment));
            updateProgress(count+1, count+1);

            updateMessage("Finished.");
        }
        return chunks;
    }

    private void applyRollingAddition(ModifiableChunk chunk, int baseIndex) {
        int pointer = baseIndex;
        int running = 0;
        while (pointer < chunk.getSize()) {
            running = running + chunk.getUnsignedByte(pointer);
            chunk.write(pointer, 1, running);
            pointer++;
        }
    }

    private void applyRollingXor(ModifiableChunk chunk) {
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
}
