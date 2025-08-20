package com.hitchhikerprod.dragonjars.data;

import javafx.scene.image.PixelWriter;

import java.util.List;

public class ImageDecoder {
    private record Corner(List<Byte> data, int x0, int y0) {}

    private final Chunk codeChunk;
    private final int[] buffer;

    public ImageDecoder(Chunk codeChunk, int[] buffer) {
        this.codeChunk = codeChunk;
        this.buffer = buffer;
    }

    public void decodeCorner(int index) {
        final Corner c = getCorner(index);

        final int width = 0x7f & c.data.get(0);
        final int height = 0xff & c.data.get(1);
        final int dx = c.data.get(2);
        final int dy = c.data.get(3);

        final int x0 = c.x0 + dx;
        final int y0 = c.y0 + dy;

        int callIndex = 0;
        if ((x0 & 0x0001) > 0) callIndex |= 0x2;
        if ((x0 & 0x8000) > 0) callIndex |= 0x4;

        final Chunk cornerChunk = new Chunk(c.data);

        switch (callIndex) {
            case 0x0 -> decode_0d48(cornerChunk, 4, width, height, x0, y0, 0x50, 0x50);
            case 0x2 -> throw new UnsupportedOperationException("0x0dab");
            case 0x4 -> throw new UnsupportedOperationException("0x0e2d");
            case 0x6 -> throw new UnsupportedOperationException("0x0e85");
            case 0x8 -> throw new UnsupportedOperationException("0x0efd");
            case 0xa -> throw new UnsupportedOperationException("0x0f72");
        }
    }

    // 0x68c0 is the lookup table for most ROM image data
    public void decodeRomImage(int index, PixelWriter writer) {
        // address LUT: 0x68c0:2 - 0x6914:2
        final List<Byte> lut = codeChunk.getBytes(0x67c0, 0x56);
        final int baseAddress = (lut.get((2 * index) + 1) & 0xff) << 8 |
                (lut.get(2 * index) & 0xff);

        int pointer = baseAddress - 0x0100;

        final int innerCounter = codeChunk.getUnsignedByte(pointer);
        final int outerCounter = codeChunk.getUnsignedByte(pointer + 1);
        int x0 = codeChunk.getUnsignedByte(pointer + 2);
        int baseBit = (x0 % 2 == 1) ? 2 : 6;
        x0 = x0 / 2;
        final int y0 = codeChunk.getUnsignedByte(pointer + 3);

        pointer += 4;

        for (int i = 0; i < outerCounter; i++) {
            final int y = y0 + i;
            int innerBit = baseBit;
            int x = x0 * 8;
            for (int j = 0; j < innerCounter; j++) {
                final int colorIndex = codeChunk.getUnsignedByte(pointer);
                pointer++;
                writer.setArgb(x + (7 - innerBit), y, Images.convertColorIndex(colorIndex));

                innerBit++;
                writer.setArgb(x + (7 - innerBit), y, Images.convertColorIndex(colorIndex >> 4));

                innerBit -= 2;
                if (innerBit < 0) {
                    innerBit = 7;
                    x += 8;
                }
                innerBit--;
            }
        }
    }
    
    public void decodeTexture(Chunk chunk, int pointer, int offset, int x0_352e, int y0_3532, int unk_100e) { // 0x0ca7
        // chunk: stand-in for [1011/seg]
        // pointer: stand-in for [100f/adr]
        // offset: stand-in for bx
        final int value = chunk.getUnsignedByte(pointer + offset);
        if (value == 0) return;
        entrypoint0cb8(chunk, pointer + value, x0_352e, y0_3532, unk_100e);
    }

    public void entrypoint0cb8(Chunk chunk, int pointer, int x0_352e, int y0_3532, int unk_100e) { // 0cb8
        // chunk: stand-in for [1011/seg]
        // pointer: stand-in for [100f/adr]
        int width_1008 = chunk.getUnsignedByte(pointer++);
        int height_100d = chunk.getUnsignedByte(pointer++);

        // 0x0ccb
        int offsetX = chunk.getByte(pointer++); // sign extended
        if ((unk_100e & 0x80) > 0) offsetX = offsetX * -1;
        int x0 = x0_352e + (offsetX & 0xffff); // enough of this four-byte int nonsense
        if ((width_1008 & 0x80) > 0 && (unk_100e & 0x80) > 0) x0--;

        // 0x0cea
        width_1008 = width_1008 & 0x7f;
        int offsetY = chunk.getByte(pointer++); // sign extended
        if ((unk_100e & 0x40) > 0) offsetY = offsetY * -1;
        final int y0 = y0_3532 + offsetY;

        // 0x0cfe; index divided by two because we aren't using a word-based lookup table
        int callIndex = 0;
        if ((x0 & 0x0001) > 0) callIndex = callIndex | 0x02;
        if ((x0 & 0x8000) > 0) callIndex = callIndex | 0x04;
        if ((unk_100e & 0x0080) > 0) callIndex = callIndex | 0x08;

        final int factor_1013 = 0x50; // the automap uses 0x90 but everything else uses 0x50
        final int factorCopy_1015 = ((unk_100e & 0x40) > 0) ? -1 * factor_1013 : factor_1013;

        switch (callIndex) {
            case 0x0 -> decode_0d48(chunk, pointer, width_1008, height_100d, x0, y0, factor_1013, factorCopy_1015);
            case 0x2 -> decode_0dab();
            case 0x4 -> decode_0e2d();
            case 0x6 -> decode_0e85();
            case 0x8 -> decode_0efd();
            case 0xa -> decode_0f72();
            case 0xc, 0xe -> {}
            default -> throw new RuntimeException("Unrecognized call index " + callIndex + "; this shouldn't be possible");
        }
    }

    // 0x652e is the lookup table for the little viewport corner decorations
    private Corner getCorner(int index) {
        final int lutAddress = 0x6428 + (index * 4);
        final List<Byte> metadata = codeChunk.getBytes(lutAddress, 4);

        final int baseAddress = (((metadata.get(1) & 0xff) << 8) | (metadata.get(0) & 0xff)) - 0x0100;
        final int x0 = metadata.get(2) & 0xff;
        final int y0 = metadata.get(3) & 0xff;
        final int width = codeChunk.getUnsignedByte(baseAddress);
        final int height = codeChunk.getUnsignedByte(baseAddress + 1);
        final List<Byte> imageData = codeChunk.getBytes(baseAddress, 4 + (width * height));
        return new Corner(imageData, x0, y0);
    }

    private void decode_0d48(Chunk chunk, final int pointer, int width, int height, int x0t2, int y0,
                             int factor, int factorCopy) {
        final boolean x0Sign = (x0t2 & 0x8000) > 0;
        final int x0 = (x0t2 >> 1) | (x0Sign ? 0x8000 : 0x0000);

        int width_100a = width; // was 1008
        int temp = width + x0 - factor;
        if (temp > 0) {
            width_100a -= temp;
            if (width_100a <= 0) return;
        }

        int y = height;

        // 0x0d68
        int dx = x0 + (y0 * factor); // via 0xac92 multiplication table
        int si = pointer;
        while (y > 0) {
            int x = width_100a;
            int di = dx;
            int save_si = si;
            while (x > 0) {
                final int bx = chunk.getUnsignedByte(si++);
                int pixel = buffer[di];
                final int byteAnd = codeChunk.getByte(0xada2 + bx);
                final int byteOr = codeChunk.getByte(0xaea2 + bx);
                pixel = pixel & byteAnd;
                pixel = pixel | byteOr;
                buffer[di++] = pixel;
                x--;
            }
            si = save_si + width;
            dx += factorCopy;
            y--;
        }
    }

    private void decode_0dab() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    private void decode_0e2d() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    private void decode_0e85() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    private void decode_0efd() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    private void decode_0f72() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
