package com.hitchhikerprod.dragonjars.tasks;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.ChunkTable;
import javafx.concurrent.Task;

import java.io.RandomAccessFile;

public class SaveChunkTask extends Task<Void> {
    private final int chunkId;
    private final Chunk chunkData;

    public SaveChunkTask(int chunkId, Chunk chunkData) {
        this.chunkId = chunkId;
        this.chunkData = chunkData;
    }

    @Override
    protected Void call() throws Exception {
        try (
            final RandomAccessFile data1 = new RandomAccessFile("DATA1", "rw");
            final RandomAccessFile data2 = new RandomAccessFile("DATA2", "rw")
        ) {
            if (!new ChunkTable(data1, data2).writeChunk(chunkId, chunkData)) failed();
        }
        return null;
    }
}
