package com.hitchhikerprod.dragonjars.data;


import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import java.util.ArrayList;
import java.util.List;

public class ChunkImageDecoder {
    private final Chunk chunk;

    public ChunkImageDecoder(Chunk chunk) {
        this.chunk = chunk;
    }

    public Image parse() {
        final HuffmanDecoder decoder = new HuffmanDecoder(chunk);
        final List<Byte> decoded = decoder.decode();
        final List<Byte> rolled = applyRollingXor(decoded, 0x00);
        return convert(rolled);
    }

    private int getWord(List<Byte> table, int index) {
        byte b0 = table.get(index);
        byte b1 = table.get(index + 1);
        return (b1 << 8) | (b0 & 0xff);
    }

    private void setWord(List<Byte> table, int index, int value) {
        while (table.size() <= index + 1) table.add((byte) 0);
        table.set(index, (byte) (value & 0x00ff));
        table.set(index + 1, (byte) ((value & 0xff00) >> 8));
    }

    private List<Byte> applyRollingXor(List<Byte> input, int startAddress) {
        int readAddress = startAddress;
        int writeAddress = startAddress + 0xa0;
        final List<Byte> output = new ArrayList<>(input);
        for (int i = 0; i < 0x3e30; i++) {
            final int b0 = getWord(output, readAddress);
            final int b1 = getWord(output, writeAddress);
            final int result = (b0 ^ b1) & 0xffff;
            setWord(output, writeAddress, result);
            readAddress += 2;
            writeAddress += 2;
        }
        return output;
    }

    private WritableImage convert(List<Byte> words) {
        final WritableImage output = new WritableImage(320, 200);
        final PixelWriter writer = output.getPixelWriter();

        int inputCounter = 0;
        for (int y = 0; y < 200; y++) {
            for (int x = 0; x < 320; x += 8) {
                Images.convertEgaData(writer, (i) -> words.get(i) & 0xff, inputCounter, x, y);
                inputCounter += 4;
            }
        }
        return output;
    }
}
