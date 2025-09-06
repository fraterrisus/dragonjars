package com.hitchhikerprod.dragonjars.tasks;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.ChunkTable;
import javafx.concurrent.Task;

import java.io.RandomAccessFile;

public class SaveChunkTask extends Task<Void> {
    private final String data1Path, data2Path;
    private final int chunkId;
    private final Chunk chunkData;

    public SaveChunkTask(String data1Path, String data2Path, int chunkId, Chunk chunkData) {
        this.data1Path = data1Path;
        this.data2Path = data2Path;
        this.chunkId = chunkId;
        this.chunkData = chunkData;
    }

    @Override
    protected Void call() throws Exception {
        try (
            final RandomAccessFile data1 = new RandomAccessFile(data1Path, "rw");
            final RandomAccessFile data2 = new RandomAccessFile(data2Path, "rw")
        ) {
            if (!new ChunkTable(data1, data2).writeChunk(chunkId, chunkData)) failed();
        }
        return null;
    }
}
