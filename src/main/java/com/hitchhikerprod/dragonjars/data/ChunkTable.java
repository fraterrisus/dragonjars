package com.hitchhikerprod.dragonjars.data;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

public class ChunkTable {
    public static final int TITLE_SCREEN = 0x1d;
    public static final int YOU_WIN = 0x18;

    private record FilePointer(int fileNum, int start, int size) { }

    final RandomAccessFile data1;
    final RandomAccessFile data2;

    final List<FilePointer> data1Chunks;
    final List<FilePointer> data2Chunks;

    public ChunkTable(RandomAccessFile data1, RandomAccessFile data2) {
        this.data1 = data1;
        this.data2 = data2;
        this.data1Chunks = readFile(data1, 1);
        this.data2Chunks = readFile(data2, 2);
    }

    public Chunk getModifiableChunk(int chunkId){
        return new ModifiableChunk(readChunkHelper(chunkId, this::readBytes));
    }

    public Chunk readChunk(int chunkId) {
        return new Chunk(readChunkHelper(chunkId, this::readBytes));
    }

    public boolean writeChunk(int chunkId, Chunk chunkData) {
        return writeChunkHelper(chunkId, chunkData, this::writeBytes);
    }

    public int getChunkCount() {
        return data1Chunks.size();
    }

    public void printStartTable() {
        for (int i = 0; i < data1Chunks.size(); i++) {
            final int d1Offset = Optional.ofNullable(data1Chunks.get(i)).map(FilePointer::start).orElse(0);
            final int d2Offset = Optional.ofNullable(data2Chunks.get(i)).map(FilePointer::start).orElse(0);
            System.out.printf("%04x %08x %08x\n", i, d1Offset, d2Offset);
        }
    }

    private Optional<FilePointer> getPointer(int chunkId) {
        final FilePointer fp1 = data1Chunks.get(chunkId);
        final FilePointer fp2 = data2Chunks.get(chunkId);
        if (fp1 == null || fp1.start() == 0x00) {
            if (fp2 == null || fp2.start() == 0x00) {
                // throw new RuntimeException("Chunk ID " + chunkId + " not found");
                return Optional.empty();
            } else {
                return Optional.of(fp2);
            }
        } else {
            return Optional.of(fp1);
        }
    }

    @FunctionalInterface
    private interface ChunkReader {
        List<Byte> apply(RandomAccessFile file, FilePointer ptr);
    }

    private List<Byte> readChunkHelper(int chunkId, ChunkReader reader) {
        final RandomAccessFile dataFile;
        final Optional<FilePointer> filePointer = getPointer(chunkId);
        if (filePointer.isPresent()) {
            final int n = filePointer.get().fileNum();
            switch (n) {
                case 1 -> dataFile = data1;
                case 2 -> dataFile = data2;
                default -> throw new RuntimeException("Unrecognized data file number " + n);
            }
            return reader.apply(dataFile, filePointer.get());
        } else {
            return Collections.emptyList();
        }
    }

    private List<Byte> readBytes(RandomAccessFile dataFile, FilePointer fp) {
        final int len = fp.size();
        final byte[] rawBytes = new byte[len];
        try {
            dataFile.seek(fp.start());
            dataFile.read(rawBytes, 0, len);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return IntStream.range(0, rawBytes.length)
                .mapToObj(i -> rawBytes[i])
                .toList();
    }

    @FunctionalInterface
    private interface ChunkWriter {
        void apply(RandomAccessFile file, FilePointer ptr, Chunk data) throws IOException;
    }

    private boolean writeChunkHelper(int chunkId, Chunk chunkData, ChunkWriter writer) {
        final RandomAccessFile dataFile;
        final Optional<FilePointer> filePointer = getPointer(chunkId);
        if (filePointer.isPresent()) {
            final int n = filePointer.get().fileNum();
            switch (n) {
                case 1 -> dataFile = data1;
                case 2 -> dataFile = data2;
                default -> throw new RuntimeException("Unrecognized data file number " + n);
            }
            if (filePointer.get().size() < chunkData.getSize()) {
                System.err.println("Chunk (" + chunkData.getSize() + "b) is larger than file space (" + filePointer.get().size() + "b)");
            }
            try {
                writer.apply(dataFile, filePointer.get(), chunkData);
                return true;
            } catch (IOException e) {
                System.err.println("Exception writing data: " + e.getMessage());
                return false;
            }
        } else {
            return false;
        }
    }

    private void writeBytes(RandomAccessFile file, FilePointer ptr, Chunk data) throws IOException {
        file.seek(ptr.start());
        final int bufSize = Integer.min(ptr.size(), data.getSize());
        final byte[] buf = new byte[bufSize];
        for (int i = 0 ; i < bufSize; i++) {
            buf[i] = data.getByte(i);
        }
        file.write(buf);
    }

    private static List<FilePointer> readFile(RandomAccessFile dataFile, int fileNum) {
        try {
            List<FilePointer> chunks = new ArrayList<>();
            int next_pointer = 0x300;
            for (int pointer = 0; pointer < 0x300; pointer += 2) {
                final int b0 = dataFile.readUnsignedByte();
                final int b1 = dataFile.readUnsignedByte();
                final int size = (b1 << 8) | (b0);
                if ((size == 0) || ((size & 0x8000) > 0)) {
                    chunks.add(null);
                } else {
                    chunks.add(new FilePointer(fileNum, next_pointer, size));
                    next_pointer += size;
                }
            }
            return Collections.unmodifiableList(chunks);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
