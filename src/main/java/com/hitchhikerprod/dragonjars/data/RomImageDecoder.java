package com.hitchhikerprod.dragonjars.data;

import javafx.scene.image.PixelWriter;

import java.util.List;

public class RomImageDecoder {
    record Corner(List<Byte> data, int width, int height) {}

    private final Chunk executable;

    public RomImageDecoder(Chunk executable) {
        this.executable = executable;
    }

    // 0x68c0 is the lookup table for most ROM image data
    public void decode68c0(int index, PixelWriter writer) {
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
    public void decode652e(int[] buffer, int index) {
//        final int factor = 0x50;
//        final int val100e = 0x00;
        final Corner c = CORNERS.get(index);

        final int a0 = 0xff & c.data.get(0);
        final int a1 = 0xff & c.data.get(1);
        final int a2 = c.data.get(2);
        final int a3 = c.data.get(3);

        int val1008 = a0;
        int val100d = a1;
//         int val100f = base_address;
//         int val1015;
        int val352e = c.width;
        int val3532 = c.height;

//         if ((val100e & 0x80) == 0) {
            val352e += a2;
//        } else {
//            val352e -= a2;
//        }

//        if (((val100e & 0x80) > 0) && ((val100e & 0x80) > 0)) {
//            val352e--;
//        }
        val1008 = val1008 & 0x7f;

//        if ((val100e & 0x40) == 0) {
        val3532 += a3;
//        } else {
//            val3532 -= a3;
//        }

        int callIndex = 0;
        if ((val352e & 0x0001) > 0) callIndex |= 0x2;
        if ((val352e & 0x8000) > 0) callIndex |= 0x4;
//        if ((val100e & 0x80) > 0) callIndex |= 0x8;

//        if ((val100e & 0x40) > 0) {
//            val1015 = -factor;
//        } else {
//        val1015 = 0x50;
//        }

        switch (callIndex) {
            case 0x0 -> decode_d48(buffer, c.data, val1008, val100d, val352e, val3532);
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

    private void decode_d48(int[] buffer, List<Byte> data, int width, int height,
                            int x0t2, int y0) {
        final int x0 = x0t2 / 2;
        int val100a = width;

        final int ax = width + x0 - 0x50;
        if (ax > 0) {
            val100a -= ax;
            if (val100a <= 0) return;
        }

//        final int rowIncrement = 1;

        int imagePointer = 0x4;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < val100a; i++) {
                final int d = 0xff & data.get(imagePointer);
                final int byteAnd = 0xff & DATA_ada2.get(d);
                final int byteOr = 0xff & DATA_aea2.get(d);
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

    private static final List<Byte> DATA_ada2 = List.of(
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0f,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0f,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0f,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0f,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0f,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0f,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0xf0, (byte) 0xf0, (byte) 0xf0, (byte) 0xf0, (byte) 0xf0, (byte) 0xf0, (byte) 0xff,
            (byte) 0xf0, (byte) 0xf0, (byte) 0xf0, (byte) 0xf0, (byte) 0xf0, (byte) 0xf0, (byte) 0xf0, (byte) 0xf0,
            (byte) 0xf0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0f,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0f,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0f,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0f,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0f,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0f,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0f,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0f,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0f,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00
    );


    private static final List<Byte> DATA_aea2 = List.of(
            (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x05, (byte) 0x00,
            (byte) 0x07, (byte) 0x08, (byte) 0x09, (byte) 0x0a, (byte) 0x0b, (byte) 0x0c, (byte) 0x0d, (byte) 0x0e,
            (byte) 0x0f, (byte) 0x10, (byte) 0x11, (byte) 0x12, (byte) 0x13, (byte) 0x14, (byte) 0x15, (byte) 0x10,
            (byte) 0x17, (byte) 0x18, (byte) 0x19, (byte) 0x1a, (byte) 0x1b, (byte) 0x1c, (byte) 0x1d, (byte) 0x1e,
            (byte) 0x1f, (byte) 0x20, (byte) 0x21, (byte) 0x22, (byte) 0x23, (byte) 0x24, (byte) 0x25, (byte) 0x20,
            (byte) 0x27, (byte) 0x28, (byte) 0x29, (byte) 0x2a, (byte) 0x2b, (byte) 0x2c, (byte) 0x2d, (byte) 0x2e,
            (byte) 0x2f, (byte) 0x30, (byte) 0x31, (byte) 0x32, (byte) 0x33, (byte) 0x34, (byte) 0x35, (byte) 0x30,
            (byte) 0x37, (byte) 0x38, (byte) 0x39, (byte) 0x3a, (byte) 0x3b, (byte) 0x3c, (byte) 0x3d, (byte) 0x3e,
            (byte) 0x3f, (byte) 0x40, (byte) 0x41, (byte) 0x42, (byte) 0x43, (byte) 0x44, (byte) 0x45, (byte) 0x40,
            (byte) 0x47, (byte) 0x48, (byte) 0x49, (byte) 0x4a, (byte) 0x4b, (byte) 0x4c, (byte) 0x4d, (byte) 0x4e,
            (byte) 0x4f, (byte) 0x50, (byte) 0x51, (byte) 0x52, (byte) 0x53, (byte) 0x54, (byte) 0x55, (byte) 0x50,
            (byte) 0x57, (byte) 0x58, (byte) 0x59, (byte) 0x5a, (byte) 0x5b, (byte) 0x5c, (byte) 0x5d, (byte) 0x5e,
            (byte) 0x5f, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x05, (byte) 0x00,
            (byte) 0x07, (byte) 0x08, (byte) 0x09, (byte) 0x0a, (byte) 0x0b, (byte) 0x0c, (byte) 0x0d, (byte) 0x0e,
            (byte) 0x0f, (byte) 0x70, (byte) 0x71, (byte) 0x72, (byte) 0x73, (byte) 0x74, (byte) 0x75, (byte) 0x70,
            (byte) 0x77, (byte) 0x78, (byte) 0x79, (byte) 0x7a, (byte) 0x7b, (byte) 0x7c, (byte) 0x7d, (byte) 0x7e,
            (byte) 0x7f, (byte) 0x80, (byte) 0x81, (byte) 0x82, (byte) 0x83, (byte) 0x84, (byte) 0x85, (byte) 0x80,
            (byte) 0x87, (byte) 0x88, (byte) 0x89, (byte) 0x8a, (byte) 0x8b, (byte) 0x8c, (byte) 0x8d, (byte) 0x8e,
            (byte) 0x8f, (byte) 0x90, (byte) 0x91, (byte) 0x92, (byte) 0x93, (byte) 0x94, (byte) 0x95, (byte) 0x90,
            (byte) 0x97, (byte) 0x98, (byte) 0x99, (byte) 0x9a, (byte) 0x9b, (byte) 0x9c, (byte) 0x9d, (byte) 0x9e,
            (byte) 0x9f, (byte) 0xa0, (byte) 0xa1, (byte) 0xa2, (byte) 0xa3, (byte) 0xa4, (byte) 0xa5, (byte) 0xa0,
            (byte) 0xa7, (byte) 0xa8, (byte) 0xa9, (byte) 0xaa, (byte) 0xab, (byte) 0xac, (byte) 0xad, (byte) 0xae,
            (byte) 0xaf, (byte) 0xb0, (byte) 0xb1, (byte) 0xb2, (byte) 0xb3, (byte) 0xb4, (byte) 0xb5, (byte) 0xb0,
            (byte) 0xb7, (byte) 0xb8, (byte) 0xb9, (byte) 0xba, (byte) 0xbb, (byte) 0xbc, (byte) 0xbd, (byte) 0xbe,
            (byte) 0xbf, (byte) 0xc0, (byte) 0xc1, (byte) 0xc2, (byte) 0xc3, (byte) 0xc4, (byte) 0xc5, (byte) 0xc0,
            (byte) 0xc7, (byte) 0xc8, (byte) 0xc9, (byte) 0xca, (byte) 0xcb, (byte) 0xcc, (byte) 0xcd, (byte) 0xce,
            (byte) 0xcf, (byte) 0xd0, (byte) 0xd1, (byte) 0xd2, (byte) 0xd3, (byte) 0xd4, (byte) 0xd5, (byte) 0xd0,
            (byte) 0xd7, (byte) 0xd8, (byte) 0xd9, (byte) 0xda, (byte) 0xdb, (byte) 0xdc, (byte) 0xdd, (byte) 0xde,
            (byte) 0xdf, (byte) 0xe0, (byte) 0xe1, (byte) 0xe2, (byte) 0xe3, (byte) 0xe4, (byte) 0xe5, (byte) 0xe0,
            (byte) 0xe7, (byte) 0xe8, (byte) 0xe9, (byte) 0xea, (byte) 0xeb, (byte) 0xec, (byte) 0xed, (byte) 0xee,
            (byte) 0xef, (byte) 0xf0, (byte) 0xf1, (byte) 0xf2, (byte) 0xf3, (byte) 0xf4, (byte) 0xf5, (byte) 0xf0,
            (byte) 0xf7, (byte) 0xf8, (byte) 0xf9, (byte) 0xfa, (byte) 0xfb, (byte) 0xfc, (byte) 0xfd, (byte) 0xfe,
            (byte) 0xff
    );

    private static final List<Corner> CORNERS = List.of(
            new Corner(List.of(
                    (byte)0x04, (byte)0x0a, (byte)0x00, (byte)0x00, (byte)0xaa, (byte)0xaa, (byte)0xaa, (byte)0x06,
                    (byte)0xaa, (byte)0xaa, (byte)0xaa, (byte)0x06, (byte)0xaa, (byte)0xae, (byte)0xe2, (byte)0x06,
                    (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x66, (byte)0x22, (byte)0x22, (byte)0x20, (byte)0x66,
                    (byte)0x0e, (byte)0xaa, (byte)0xa0, (byte)0x66, (byte)0x0e, (byte)0xae, (byte)0x06, (byte)0x66,
                    (byte)0x0e, (byte)0xe0, (byte)0x66, (byte)0x66, (byte)0x0a, (byte)0x06, (byte)0x66, (byte)0x66,
                    (byte)0x06, (byte)0x66, (byte)0x66, (byte)0x66
            ), 0x00, 0x00),
            new Corner(List.of(
                    (byte)0x04, (byte)0x0a, (byte)0x00, (byte)0x00, (byte)0x60, (byte)0xaa, (byte)0xae, (byte)0xea,
                    (byte)0x60, (byte)0xae, (byte)0xee, (byte)0xae, (byte)0x60, (byte)0x2e, (byte)0xee, (byte)0xe2,
                    (byte)0x66, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x66, (byte)0x02, (byte)0x22, (byte)0x22,
                    (byte)0x66, (byte)0x02, (byte)0xee, (byte)0xe0, (byte)0x66, (byte)0x60, (byte)0x2e, (byte)0xe0,
                    (byte)0x66, (byte)0x66, (byte)0x62, (byte)0xe0, (byte)0x66, (byte)0x66, (byte)0x66, (byte)0xa0,
                    (byte)0x66, (byte)0x66, (byte)0x66, (byte)0x60
            ), 0x98, 0x00),
            new Corner(List.of(
                    (byte)0x04, (byte)0x0d, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x06, (byte)0x66, (byte)0x66,
                    (byte)0xae, (byte)0xe0, (byte)0x06, (byte)0x66, (byte)0xaa, (byte)0xea, (byte)0xe0, (byte)0x66,
                    (byte)0xae, (byte)0xae, (byte)0x2e, (byte)0x06, (byte)0xae, (byte)0xe2, (byte)0x2a, (byte)0x06,
                    (byte)0x22, (byte)0x22, (byte)0x2a, (byte)0x06, (byte)0x22, (byte)0x22, (byte)0x2a, (byte)0x06,
                    (byte)0x22, (byte)0x22, (byte)0x2a, (byte)0x06, (byte)0x22, (byte)0x22, (byte)0x2a, (byte)0x06,
                    (byte)0x22, (byte)0x22, (byte)0x2a, (byte)0x06, (byte)0x22, (byte)0x22, (byte)0x2a, (byte)0x06,
                    (byte)0x22, (byte)0x22, (byte)0x2a, (byte)0x06, (byte)0x22, (byte)0x22, (byte)0x2a, (byte)0x96
            ), 0x00, 0x7b),
            new Corner(List.of(
                    (byte)0x04, (byte)0x0d, (byte)0x00, (byte)0x00, (byte)0x66, (byte)0x66, (byte)0x60, (byte)0x00,
                    (byte)0x66, (byte)0x60, (byte)0x0e, (byte)0xea, (byte)0x66, (byte)0x0e, (byte)0xae, (byte)0xaa,
                    (byte)0x60, (byte)0xe2, (byte)0xea, (byte)0xea, (byte)0x60, (byte)0xa2, (byte)0x2e, (byte)0xea,
                    (byte)0x60, (byte)0xa2, (byte)0x22, (byte)0x22, (byte)0x60, (byte)0xa2, (byte)0x22, (byte)0x22,
                    (byte)0x60, (byte)0xa2, (byte)0x22, (byte)0x2a, (byte)0x60, (byte)0xa2, (byte)0x22, (byte)0x2a,
                    (byte)0x60, (byte)0xa2, (byte)0x22, (byte)0x2a, (byte)0x60, (byte)0xa2, (byte)0x22, (byte)0x2a,
                    (byte)0x00, (byte)0x02, (byte)0x22, (byte)0x2a, (byte)0x91, (byte)0x00, (byte)0x02, (byte)0x2a
            ), 0x98, 0x7b)
    );
}
