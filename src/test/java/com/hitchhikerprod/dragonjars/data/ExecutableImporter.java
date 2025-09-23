package com.hitchhikerprod.dragonjars.data;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class ExecutableImporter {
    public Chunk getChunk() {
        try (final RandomAccessFile exec = new RandomAccessFile("/home/bcordes/pc-games/dragonwars/DRAGON.COM", "r")) {
            final int codeSize = (int)(exec.length());
            if ((long)codeSize != exec.length()) {
                throw new RuntimeException("Executable is too big for one byte array");
            }
            final byte[] codeSegment = new byte[codeSize];
            exec.readFully(codeSegment);
            return new Chunk(codeSegment);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("DRAGON.COM not found", e);
        } catch (IOException e) {
            throw new RuntimeException("DRAGON.COM could not be read", e);
        }
    }
}
