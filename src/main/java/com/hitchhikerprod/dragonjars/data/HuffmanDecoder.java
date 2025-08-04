package com.hitchhikerprod.dragonjars.data;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class HuffmanDecoder {
    private interface HuffmanNode { }

    private record HuffmanTree(HuffmanNode left, HuffmanNode right) implements HuffmanNode {}
    private record HuffmanLeaf(int value) implements HuffmanNode {}

    record Trace(int run, int word) {}

    private final Chunk chunk;
    private int chunkPointer;
    private int chunkSize;

    private List<Trace> traces;
    private Deque<Boolean> bitBuffer;
    private int traceIndex;

    public HuffmanDecoder(Chunk chunk) {
        this.chunk = chunk;
    }

    public List<Byte> decode() {
        this.chunkPointer = 0;
        this.chunkSize = getChunkWord();

        this.traces = new ArrayList<>();
        this.bitBuffer = new ArrayDeque<>();
        this.traceIndex = 0;

        final HuffmanNode root = buildHuffmanTree(0);
        return treeDecode(root);
    }

    public Chunk decodeChunk() {
        return new Chunk(decode());
    }

    private HuffmanNode buildHuffmanTree(int run) {
        if (run == runLength()) {
            final int value = dataWord();
            nextTrace();
            return new HuffmanLeaf(value);
        }

        final HuffmanNode left = buildHuffmanTree(run + 1);
        final HuffmanNode right = buildHuffmanTree(0);
        return new HuffmanTree(left, right);
    }

    private List<Byte> treeDecode(HuffmanNode treeRoot) {
        List<Byte> decoded = new ArrayList<>();
        int count = this.chunkSize;
        HuffmanNode node = treeRoot;

        while (count > 0) {
            if (node instanceof final HuffmanLeaf leaf) {
                decoded.add((byte)(leaf.value() & 0xff));
                node = treeRoot;
                count--;
                continue;
            }

            if (bitBuffer.isEmpty()) unpackByte();
            final Boolean nextBit = bitBuffer.removeFirst();
            if (node instanceof final HuffmanTree tree) {
                node = (nextBit) ? tree.right() : tree.left();
            } else {
                throw new RuntimeException("this has to be a Tree");
            }
        }

        return decoded;
    }

/* You'd have to add support for tracking DI on each node again to make this work
    private void treeToTable(List<Byte> output, HuffmanNode node) {
        final int index = node.di();
        if (node instanceof final HuffmanTree tree) {
            final int leftVal = tree.left().di();
            final int rightVal = tree.right().di();
            output.add(index,   (byte)(leftVal & 0x00ff));
            output.add(index+1, (byte)((leftVal & 0xff00) >> 8));
            output.add(index+2, (byte)(rightVal & 0x00ff));
            output.add(index+3, (byte)((rightVal & 0xff00) >> 8));
            treeToTable(output, tree.left());
            treeToTable(output, tree.right());
        } else if (node instanceof final HuffmanLeaf leaf) {
            output.add(index,   (byte)0);
            output.add(index+1, (byte)0);
            output.add(index+2, (byte)(leaf.value() & 0x00ff));
            output.add(index+3, (byte)((leaf.value() & 0xff00) >> 8));
        }
    }
*/

    private byte getChunkByte() {
        byte b = chunk.getByte(this.chunkPointer);
        this.chunkPointer += 1;
        return b;
    }

    private int getChunkWord() {
        int word = chunk.getWord(this.chunkPointer);
        this.chunkPointer += 2;
        return word;
    }

    private Trace getTrace() {
        while (traceIndex >= traces.size()) {
            readNextTrace();
        }
        return traces.get(traceIndex);
    }

    private void nextTrace() {
        this.traceIndex++;
    }

    private int runLength() {
        return getTrace().run();
    }

    private int dataWord() {
        return getTrace().word();
    }

    private void readNextTrace() {
        int zeroCount = -1;
        Boolean nextBit;
        do {
            zeroCount++;
            if (bitBuffer.isEmpty()) unpackByte();
            nextBit = bitBuffer.removeFirst();
        } while (nextBit == false);

        traces.add(new Trace(zeroCount, parseIntFromBits()));
    }

    private void unpackByte() {
        final byte b = getChunkByte();
        int x = 0x80;
        while (x > 0) {
            bitBuffer.add((b & x) > 0);
            x = x >> 1;
        }
    }

    private int parseIntFromBits() {
        if (bitBuffer.size() < 8) unpackByte();
        int i = 0x0;
        int x = 0x80;
        while (x > 0) {
            if (bitBuffer.removeFirst()) i = i | x;
            x = x >> 1;
        }
        return i;
    }
}
