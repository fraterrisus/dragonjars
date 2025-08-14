package com.hitchhikerprod.dragonjars.data;

import javafx.scene.image.PixelWriter;

import java.util.List;

public class RomImageDecoder {
    record Corner(List<Byte> data, int x0, int y0) {}

    private final Chunk executable;

    public RomImageDecoder(Chunk executable) {
        this.executable = executable;
    }

    // 0x68c0 is the lookup table for most ROM image data
    public void decodeRomImage(int index, PixelWriter writer) {
        // address LUT: 0x68c0:2 - 0x6914:2
        final List<Byte> lut = executable.getBytes(0x67c0, 0x56);
        final int baseAddress = (lut.get((2 * index) + 1) & 0xff) << 8 |
                (lut.get(2 * index) & 0xff);

        int pointer = baseAddress - 0x0100;

        final int innerCounter = executable.getUnsignedByte(pointer);
        final int outerCounter = executable.getUnsignedByte(pointer + 1);
        int x0 = executable.getUnsignedByte(pointer + 2);
        int baseBit = (x0 % 2 == 1) ? 2 : 6;
        x0 = x0 / 2;
        final int y0 = executable.getUnsignedByte(pointer + 3);

        pointer += 4;

        for (int i = 0; i < outerCounter; i++) {
            final int y = y0 + i;
            int innerBit = baseBit;
            int x = x0 * 8;
            for (int j = 0; j < innerCounter; j++) {
                final int colorIndex = executable.getUnsignedByte(pointer);
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

    // 0x652e is the lookup table for the little viewport corner decorations
    public void decodeCorner(int[] buffer, int index) {
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

        switch (callIndex) {
            case 0x0 -> decode_0d48(buffer, c.data, width, height, x0, y0);
            case 0x2 -> throw new UnsupportedOperationException("0x0dab");
            case 0x4 -> throw new UnsupportedOperationException("0x0e2d");
            case 0x6 -> throw new UnsupportedOperationException("0x0e85");
            case 0x8 -> throw new UnsupportedOperationException("0x0efd");
            case 0xa -> throw new UnsupportedOperationException("0x0f72");
        }
    }

    public void reorg(int[] buffer, PixelWriter writer, int y0, int height, int widthx4) {
        // TODO: I wonder about writing transparency instead of black, here
        int inputCounter = 0;
        final int x0 = 2;
        final int width = widthx4 / 4;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                Images.convertEgaData(writer, (x) -> buffer[x] & 0xff, inputCounter, 8 * (x0 + i), y0 + j);
                inputCounter += 4;
            }
        }
    }

    private Corner getCorner(int index) {
        final int lutAddress = 0x6428 + (index * 4);
        final List<Byte> metadata = executable.getBytes(lutAddress, 4);

        final int baseAddress = (((metadata.get(1) & 0xff) << 8) | (metadata.get(0) & 0xff)) - 0x0100;
        final int x0 = metadata.get(2) & 0xff;
        final int y0 = metadata.get(3) & 0xff;
        final int width = executable.getUnsignedByte(baseAddress);
        final int height = executable.getUnsignedByte(baseAddress + 1);
        final List<Byte> imageData = executable.getBytes(baseAddress, 4 + (width * height));
        return new Corner(imageData, x0, y0);
    }

    private void decode_0d48(int[] buffer, List<Byte> data, int width, int height,
                             int x0t2, int y0) {
        final int x0 = x0t2 / 2;
        int val100a = width;

        final int ax = width + x0 - 0x50;
        if (ax > 0) {
            val100a -= ax;
            if (val100a <= 0) return;
        }

        int imagePointer = 0x4;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < val100a; i++) {
                final int d = 0xff & data.get(imagePointer);
                final int byteAnd = executable.getUnsignedByte(0xada2 + d);
                final int byteOr = executable.getUnsignedByte(0xaea2 + d);
                imagePointer++;

                final int x = x0 + i;
                final int y = y0 + j; // (j * rowIncrement), but rowIncrement is always 1
                final int adr = x + (y * 0x50);
                int oldValue = buffer[adr];
                int newValue = (oldValue & byteAnd) | byteOr;
                // System.out.printf("(%03d,%03d:%05x)  %02x <- %02x\n", x, y, adr, oldValue, newValue);
                buffer[adr] = newValue;
            }
        }
    }
}
