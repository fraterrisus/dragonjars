package com.hitchhikerprod.dragonjars.data;

import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

public class ImageDecoder {
    private record Corner(List<Byte> data, int x0, int y0) {}

    private final Chunk codeChunk;
    private final int[] buffer;

    public static class NoImageException extends RuntimeException {
        public NoImageException(String message) { super(message); }
    }

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

    public void decodeTexture(Chunk chunk, int pointer, int offset, int x0_352e, int y0_3532, int invert_100e) { // 0x0ca7
        // chunk: stand-in for [1011/seg]
        // pointer: stand-in for [100f/adr]
        // offset: stand-in for bx
        final int value = chunk.getWord(pointer + offset);
        if (value == 0) throw new NoImageException("Null pointer");
        if (pointer + value > chunk.getSize()) throw new IllegalArgumentException("Offset too large");
        entrypoint0cb8(chunk, pointer + value, x0_352e, y0_3532, invert_100e);
    }

    // this gets used by the automapper (see 0x19b3)
    public void entrypoint0cb8(Chunk chunk, int pointer, int x0_352e, int y0_3532, int invert_100e) { // 0cb8
        // chunk: stand-in for [1011/seg]
        // pointer: stand-in for [100f/adr]
        int width_1008 = chunk.getUnsignedByte(pointer++);
        int height_100d = chunk.getUnsignedByte(pointer++);

        // 0x0ccb
        int offsetX = chunk.getByte(pointer++); // sign extended
        if ((invert_100e & 0x80) > 0) offsetX = offsetX * -1;
        int x0 = x0_352e + offsetX; // enough of this four-byte int nonsense
        if ((width_1008 & 0x80) > 0 && (invert_100e & 0x80) > 0) x0--;

        // 0x0cea
        width_1008 = width_1008 & 0x7f;
        int offsetY = chunk.getByte(pointer++); // sign extended
        if ((invert_100e & 0x40) > 0) offsetY = offsetY * -1;
        final int y0 = y0_3532 + offsetY;

        int callIndex = 0;
        if ((x0 & 0x0001) > 0) callIndex = callIndex | 0x02;
        if ((x0 & 0x8000) > 0) callIndex = callIndex | 0x04;
        if ((invert_100e & 0x0080) > 0) callIndex = callIndex | 0x08;

        final int factor_1013 = 0x50; // the automap uses 0x90 but everything else uses 0x50
        final int factorCopy_1015 = ((invert_100e & 0x40) > 0) ? -1 * factor_1013 : factor_1013;

        switch (callIndex) {
            case 0x0 -> decode_0d48(chunk, pointer, width_1008, height_100d, x0, y0, factor_1013, factorCopy_1015);
            case 0x2 -> decode_0dab(chunk, pointer, width_1008, height_100d, x0, y0, factor_1013, factorCopy_1015);
            case 0x4 -> decode_0e2d(chunk, pointer, width_1008, height_100d, x0, y0, factor_1013, factorCopy_1015);
            case 0x6 -> decode_0e85(chunk, pointer, width_1008, height_100d, x0, y0, factor_1013, factorCopy_1015);
            case 0x8 -> decode_0efd(chunk, pointer, width_1008, height_100d, x0, y0, factor_1013, factorCopy_1015);
            case 0xa -> decode_0f72();
            case 0xc, 0xe -> throw new NoImageException("Call index " + callIndex);
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

    private void decode_0d48(Chunk chunk, final int pointer, int width_1008, int height_100d, int x0t2, int y0,
                             int factor_1013, int factorCopy_1015) {
        final boolean x0Sign = (x0t2 & 0x8000) > 0;
        final int x0 = (x0t2 >> 1) | (x0Sign ? 0x8000 : 0x0000);

        int width_100a = width_1008; // was 1008
        int temp = width_1008 + x0 - factor_1013;
        if (temp > 0) {
            width_100a -= temp;
            if (width_100a <= 0) throw new NoImageException("Image width less than 0");
        }

        int y = height_100d;

        // 0x0d68
        int dx = x0 + (y0 * factor_1013); // via 0xac92 multiplication table
        int si = pointer;
        while (y > 0) {
            int x = width_100a;
            int di = dx;
            int save_si = si;
            while (x > 0) {
                final int bx = chunk.getUnsignedByte(si++);
                int pixel = buffer[di] & 0xff;
                final int byteAnd = getAEA2(bx); // codeChunk.getUnsignedByte(0xada2 + bx);
                final int byteOr = getAFA2(bx); // codeChunk.getUnsignedByte(0xaea2 + bx);
                pixel = pixel & byteAnd;
                pixel = pixel | byteOr;
                buffer[di++] = pixel & 0xff;
                x--;
            }
            si = save_si + width_1008;
            dx += factorCopy_1015;
            y--;
        }
    }

    private void decode_0dab(Chunk chunk, final int pointer, int width_1008, int height_100d, int x0t2, int y0,
                             int factor_1013, int factorCopy_1015) {
        final boolean x0Sign = (x0t2 & 0x8000) > 0;
        final int x0 = (x0t2 >> 1) | (x0Sign ? 0x8000 : 0x0000);
        int tmp_100c = 0x00;

        int width_100a = width_1008; // was 1008
        int temp = width_1008 + x0 - factor_1013;
        if ((temp & 0x8000) == 0) { // temp >= 0
            width_100a = (width_100a - temp) & 0xffff;
            if (width_100a <= 0) throw new NoImageException("Image width less than 0");
            tmp_100c = 0xff;
        }

        int y = height_100d;

        // 0x0dd4
        int dx = x0 + (y0 * factor_1013); // via 0xac92 multiplication table
        int si = pointer;
        while (y > 0) {
            int x = width_100a;
            int di = dx;
            int save_si = si;
            int pixel;
            while (true) {
                final int bx = chunk.getUnsignedByte(si++); // 0df5-f6
                pixel = (buffer[di+1] & 0xff) << 8 | (buffer[di] & 0xff); // 0dfa
                final int wordAnd = getB0A2(bx); // codeChunk.getWord(0xafa2 + (bx * 2));
                final int wordOr = getB2A2(bx); // codeChunk.getWord(0xb1a2 + (bx * 2));
                pixel = pixel & wordAnd;
                pixel = pixel | wordOr;
                x--;
                if (x <= 0) break;
                buffer[di++] = pixel & 0xff; // 0df1
                buffer[di] = (pixel & 0xff00) >> 8;
            }
            buffer[di] = pixel & 0xff;
            if ((tmp_100c & 0x80) == 0) {
                di++;
                buffer[di] = (pixel & 0xff00) >> 8;
            }
            si = save_si + width_1008;
            dx += factorCopy_1015;
            y--;
        }
    }

    private void decode_0e2d(Chunk chunk, final int pointer, int width_1008, int height_100d, int x0t2, int y0,
                             int factor_1013, int factorCopy_1015) {
        final int x0t2n = -1 * x0t2;
        final boolean x0Sign = (x0t2n & 0x8000) > 0;
        final int x0 = (x0t2n >> 1) | (x0Sign ? 0x8000 : 0x0000);

        // 0x0e35
        final int width_100a = width_1008 - x0;
        if (width_100a <= 0) throw new NoImageException("Image width less than 0");

        int y = height_100d;

        // 0x0e43
        int si = pointer + (x0 & 0xff);
        int dx = y0 * factor_1013; // via 0xac92 multiplication table
        while (y > 0) {
            int x = width_100a;
            int di = dx;
            int save_si = si;
            while (x > 0) {
                final int bx = chunk.getUnsignedByte(si++);
                int pixel = buffer[di] & 0xff;
                final int byteAnd = getAEA2(bx); // codeChunk.getUnsignedByte(0xada2 + bx);
                final int byteOr = getAFA2(bx); // codeChunk.getUnsignedByte(0xaea2 + bx);
                pixel = pixel & byteAnd;
                pixel = pixel | byteOr;
                buffer[di++] = pixel & 0xff;
                x--;
            }
            si = save_si + width_1008;
            dx += factorCopy_1015;
            y--;
        }
    }

    private void decode_0e85(Chunk chunk, final int pointer, int width_1008, int height_100d, int x0t2, int y0,
                             int factor_1013, int factorCopy_1015) {
        final int x0t2n = -1 * x0t2;
        final boolean x0Sign = (x0t2n & 0x8000) > 0;
        final int x0 = (x0t2n >> 1) | (x0Sign ? 0x8000 : 0x0000);

        // 0x0e8d
        final int width_100a = width_1008 - x0;
        if (width_100a <= 0) throw new NoImageException("Image width less than 0");

        int y = height_100d;

        // 0x0e9b
        int si = pointer + (x0 & 0xff);
        int dx = (y0 * factor_1013) - 1; // via 0xac92 multiplication table
        while (y > 0) {
            int x = width_100a;
            int di = dx;
            int save_si = si;

            // do this once
            int bx = chunk.getUnsignedByte(si++); // 0eb6
            int pixel = (buffer[di+1] & 0xff) << 8 | (buffer[di] & 0xff); // 0ebb
            int wordAnd = getB0A2(bx); // codeChunk.getWord(0xafa2 + (bx * 2));
            int wordOr = getB2A2(bx); // codeChunk.getWord(0xb1a2 + (bx * 2));
            pixel = pixel & wordAnd;
            pixel = pixel | wordOr;
            di++; // 0ec8
            buffer[di] = (pixel >> 8) & 0xff; // 0ec9 stores just dh
            x--;
            while (x > 0) { // loop point at 0ed0
                bx = chunk.getUnsignedByte(si++); // 0ed0
                pixel = (buffer[di+1] & 0xff) << 8 | (buffer[di] & 0xff); // 0ed5
                wordAnd = getB0A2(bx); // codeChunk.getWord(0xafa2 + (bx * 2));
                wordOr = getB2A2(bx); // codeChunk.getWord(0xb1a2 + (bx * 2));
                pixel = pixel & wordAnd;
                pixel = pixel | wordOr;
                buffer[di++] = pixel & 0xff; // 0ee2 stores 1 word
                buffer[di] = (pixel >> 8) & 0xff;
                x--;
            }
            si = save_si + width_1008;
            dx += factorCopy_1015;
            y--;
        }
    }

    // should basically be 0d48 but flipped X
    private void decode_0efd(Chunk chunk, final int pointer, int width_1008, int height_100d, int x0t2, int y0,
                             int factor_1013, int factorCopy_1015) {
        final boolean x0Sign = (x0t2 & 0x8000) > 0;
        final int x0 = (x0t2 >> 1) | (x0Sign ? 0x8000 : 0x0000);

        int width_100a = width_1008; // was 1008
        int temp = width_1008 + x0 - factor_1013;
        if (temp > 0) {
            width_100a -= temp;
            if (width_100a <= 0) throw new NoImageException("Image width less than 0");
        }

        int y = height_100d;

        int dx = x0 + (y0 * factor_1013); // via 0xac92 multiplication table
        int si = pointer + width_1008 - 1; // see 0x0f35
        while (y > 0) {
            int x = width_100a;
            int di = dx;
            int save_si = si;
            while (x > 0) {
                // System.out.format("(%03d,%03d) [ds:%04x]", x, y, si);
                final int bx = chunk.getUnsignedByte(si--);
                // reverse the nibbles; assembly does this with a LUT at 0xada2, see 0x0fa9
                final int xb = ((bx & 0xf0) >> 4) | ((bx & 0x0f) << 4);
                // System.out.format("=%02x", xb);
                int pixel = buffer[di] & 0xff;
                // System.out.format(" [vb:%04x]=%02x", di, pixel);
                final int byteAnd = getAEA2(xb); // codeChunk.getUnsignedByte(0xada2 + xb);
                final int byteOr = getAFA2(xb); // codeChunk.getUnsignedByte(0xaea2 + xb);
                pixel = pixel & byteAnd;
                pixel = pixel | byteOr;
                // System.out.format(" & %02x | %02x -> %02x", byteAnd, byteOr, pixel);
                buffer[di++] = pixel & 0xff;
                x--;
                // System.out.println();
            }
            si = save_si + width_1008;
            dx += factorCopy_1015;
            y--;
        }
    }

    private void decode_0f72() {
        throw new UnsupportedOperationException("0x0f72");
    }

    public static void main(String[] args) {
        try (
            final RandomAccessFile exec = new RandomAccessFile("DRAGON.COM", "r");
            final RandomAccessFile data1 = new RandomAccessFile("DATA1", "r");
            final RandomAccessFile data2 = new RandomAccessFile("DATA2", "r");
        ) {
            final int chunkId = Integer.parseInt(args[0].substring(2), 16);

            final int codeSize = (int) (exec.length());
            if ((long) codeSize != exec.length()) {
                throw new RuntimeException("Executable is too big for one byte array");
            }
            final byte[] codeSegment = new byte[codeSize];
            exec.readFully(codeSegment);
            final Chunk codeChunk = new Chunk(codeSegment);

            final ChunkTable chunkTable = new ChunkTable(data1, data2);
            final Chunk rawChunk = chunkTable.readChunk(chunkId);
            final HuffmanDecoder huffman = new HuffmanDecoder(rawChunk);
            final Chunk decodedChunk = huffman.decodeChunk();
            decodedChunk.display();

            final int[] videoMemory = new int[0x3e80];
            for (int i = 0; i < 0x3e80; i++) videoMemory[i] = 0x00;
            final ImageDecoder decoder = new ImageDecoder(codeChunk, videoMemory);

            for (int offset = 0; offset < 0x20; offset += 2) {
                try {
                    decoder.decodeTexture(decodedChunk, 0, offset, 0, 0, 0);
                    System.out.println("Decoded image at offset " + offset);
                    final WritableImage buf = Images.blankImage(320, 200);
                    final PixelWriter pixelWriter = buf.getPixelWriter();
                    int ptr = 0;
                    for (int y = 0x08; y < 0x90; y++) {
                        for (int x = 0x02; x < 0x16; x++) {
                            Images.convertEgaData(pixelWriter, (z) -> videoMemory[z] & 0xff, ptr, x * 8, y);
                            ptr += 4;
                        }
                    }

                    final String filename = String.format("texture-%02x-%02x.png", chunkId, offset);
                    final BufferedImage image = new BufferedImage(320, 200, BufferedImage.TYPE_INT_RGB);
                    final PixelReader pixelReader = buf.getPixelReader();
                    for (int y = 0x08; y < 0x90; y++) {
                        for (int x = 0x02 * 8; x < 0x16 * 8; x++) {
                            image.setRGB(x, y, pixelReader.getArgb(x, y));
                        }
                    }
                    ImageIO.write(scale(image, 4.0, AffineTransformOp.TYPE_NEAREST_NEIGHBOR), "png", new File(filename));
                    for (int i = 0; i < 0x3e80; i++) videoMemory[i] = 0x00;
                } catch (IllegalArgumentException e) {
                    System.out.println("No image found at offset " + offset + ":" + e.getMessage());
                    offset = 0x100;
                } catch (NoImageException e) {
                    System.out.println("No image found at offset " + offset + ":" + e.getMessage());
                } catch (UnsupportedOperationException e) {
                    System.out.println("Unsupported decode operation at offset " + offset + ":" + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static BufferedImage scale(final BufferedImage before, final double scale, final int type) {
        int w = before.getWidth();
        int h = before.getHeight();
        int w2 = (int) (w * scale);
        int h2 = (int) (h * scale);
        BufferedImage after = new BufferedImage(w2, h2, before.getType());
        AffineTransform scaleInstance = AffineTransform.getScaleInstance(scale, scale);
        AffineTransformOp scaleOp = new AffineTransformOp(scaleInstance, type);
        scaleOp.filter(before, after);
        return after;
    }

    /** Emulates the lookup table at 0xaea2 in the binary. */
    public static int getAEA2(int index) {
        int val = 0x00;
        if ((index & 0x0f) == 0x06) val |= 0x0f;
        if ((index & 0xf0) == 0x60) val |= 0xf0;
        return val;
    }

    /** Emulates the lookup table at 0xafa2 in the binary. */
    public static int getAFA2(int index) {
        int val = index;
        if ((index & 0x0f) == 0x06) val = val - 0x06;
        if ((index & 0xf0) == 0x60) val = val - 0x60;
        return val;
    }

    public static int getB0A2(int index) {
        int val = 0x0ff0;
        if ((index & 0x0f) == 0x06) val = val | 0xf000;
        if ((index & 0xf0) == 0x60) val = val | 0x000f;
        return val;
    }

    public static int getB2A2(int index) {
        int lo = index & 0x0f;
        int hi = index & 0xf0;
        int val = 0;
        if (lo != 0x06) val |= lo << 12;
        if (hi != 0x60) val |= hi >> 4;
        return val;
    }
}
