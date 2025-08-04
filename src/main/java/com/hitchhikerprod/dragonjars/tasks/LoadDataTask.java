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
            final RandomAccessFile data1 = new RandomAccessFile("DATA1", "r");
            final RandomAccessFile data2 = new RandomAccessFile("DATA2", "r")
        ) {
            final ChunkTable table = new ChunkTable(data1, data2);
            final int count = table.getChunkCount();
            for (int i = 0; i < count; i++) {
                if (isCancelled()) return null;
                updateMessage("Loading chunk " + (i+1) + " of " + count);
                chunks.add(table.getChunk(i));
                updateProgress(i+1, count);
            }
            updateMessage("Finished.");
            updateProgress(count, count);
        }
        return chunks;
    }
}
