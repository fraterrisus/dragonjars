package com.hitchhikerprod.dragonjars.data;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class ExecutableImporter {
    public Chunk getChunk() {
        return getChunk(LocalProperties.getBasePath() + "/DRAGON.COM");
    }

    public Chunk getChunk(String filename) {
        try (final RandomAccessFile exec = new RandomAccessFile(filename, "r")) {
            final int codeSize = (int)(exec.length());
            if ((long)codeSize != exec.length()) {
                throw new RuntimeException("Executable is too big for one byte array");
            }
            final byte[] codeSegment = new byte[codeSize];
            exec.readFully(codeSegment);
            final Chunk codeChunk = new Chunk(codeSegment);
            ExecutableLayout.detect(codeChunk);
            return codeChunk;
        } catch (FileNotFoundException e) {
            throw new RuntimeException("DRAGON.COM not found", e);
        } catch (IOException e) {
            throw new RuntimeException("DRAGON.COM could not be read", e);
        }
    }

    public static void main(String[] args) {
        final ExecutableImporter importer = new ExecutableImporter();
        final Chunk executable = importer.getChunk(args[0]);
        ExecutableLayout.detect(executable);
        System.out.println(ExecutableLayout.getInstance());
    }
}
