package com.hitchhikerprod.dragonjars.tasks;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.ChunkTable;
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
            for (int i = 0; i < count; i++) {
                if (isCancelled()) return null;
                updateMessage("Loading segment " + (i+1) + " of " + count+1);
                chunks.add(table.getChunk(i));
                updateProgress(i+1, count+1);
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
}
