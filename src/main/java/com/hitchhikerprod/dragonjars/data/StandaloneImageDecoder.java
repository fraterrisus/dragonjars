package com.hitchhikerprod.dragonjars.data;

import com.hitchhikerprod.dragonjars.exec.ALU;

import javax.imageio.ImageIO;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.List;

public class StandaloneImageDecoder {
    public static final int IMAGE_X = 320;
    public static final int IMAGE_Y = 200;

    private static final Rectangle GAMEPLAY = new Rectangle(0x10, 0x08, 0xb0, 0x90);
    private static final Rectangle WHOLE_IMAGE = new Rectangle(0, 0, IMAGE_X, IMAGE_Y);

    private static final int CORNER_LUT_ADDRESS = 0x6428;
    private static final int ROM_IMAGE_LUT_ADDRESS = 0x67c0;

    private final Chunk codeChunk;
    private final byte[] pixels;

    private record ImageData(List<Byte> data, int x0, int y0, int width, int height) {}

    private record Rectangle(int x0, int y0, int x1, int y1) {
        public boolean contains(int x, int y) {
            return x >= x0 && y >= y0 && x < x1 && y < y1;
        }
    }

    public StandaloneImageDecoder(Chunk codeChunk) {
        this.codeChunk = codeChunk;
        this.pixels = new byte[IMAGE_X * IMAGE_Y];
    }

    private int coordToIndex(int x, int y) {
        return x + (IMAGE_X * y);
    }

    public void clearBuffer(byte value) {
        Arrays.fill(pixels, value);
    }

    public void drawGrid() {
        for (int y = 0; y < IMAGE_Y; y += 8) {
            for (int x = 0; x < IMAGE_X; x++) {
                pixels[coordToIndex(x, y)] = 0x06;
            }
        }
        for (int x = 0; x < IMAGE_X; x += 8) {
            for (int y = 0; y < IMAGE_Y; y++) {
                pixels[coordToIndex(x, y)] = 0x06;
            }
        }
    }

    public void exportToPNG(String filename) throws IOException {
        /*
        final WritableImage wimage = new WritableImage(IMAGE_X, IMAGE_Y);
        final PixelWriter writer = wimage.getPixelWriter();
        for (int y = 0; y < IMAGE_X; y++) {
            for (int x = 0; x < IMAGE_Y; x++) {
                final byte pixel = pixels[x + (IMAGE_X * y)];
                final int color = Images.convertColorIndex(pixel);
                writer.setArgb(x, y, color);
            }
        }

        final PixelReader pixelReader = wimage.getPixelReader();
        */
        final BufferedImage image = new BufferedImage(IMAGE_X, IMAGE_Y, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < IMAGE_Y; y++) {
            for (int x = 0; x < IMAGE_X; x++) {
                // image.setRGB(x, y, pixelReader.getArgb(x, y));
                final byte pixel = pixels[coordToIndex(x, y)];
                image.setRGB(x, y, Images.convertColorIndex(pixel));
            }
        }

        ImageIO.write(ImageDecoder.scale(image, 4.0, AffineTransformOp.TYPE_NEAREST_NEIGHBOR), "png", new File(filename));
    }

    public void decodeChunkImage(ModifiableChunk chunk) {
        final List<Byte> imageData = applyRollingXor(chunk);
        final ImageData c = new ImageData(imageData, 0, 0, IMAGE_X, IMAGE_Y);
        drawImageData(c, 0, WHOLE_IMAGE);
    }

    public void decodeCorner(int index) {
        final int lutAddress = CORNER_LUT_ADDRESS + (index * 4);

        final int baseAddress = codeChunk.getWord(lutAddress) - 0x100;
        final int x0 = codeChunk.getUnsignedByte(lutAddress + 2);
        final int y0 = codeChunk.getUnsignedByte(lutAddress + 3);

        final int width = codeChunk.getUnsignedByte(baseAddress);
        final int height = codeChunk.getUnsignedByte(baseAddress + 1);
        // skip over two bytes of 0x00
        final List<Byte> imageData = codeChunk.getBytes(baseAddress, 4 + (width * height));

        // Corners target the gameplay area, so we need to artificially bump this out by (16,8)
        // width is half-size because of two pixels per byte
        final ImageData c = new ImageData(imageData, GAMEPLAY.x0() + x0, GAMEPLAY.y0() + y0, 2 * width, height);

        drawImageData(c, 4, GAMEPLAY);
    }

    public void decodeRomImage(int index) {
        final int lutAddress = ROM_IMAGE_LUT_ADDRESS + (index * 2);
        final int baseAddress = codeChunk.getWord(lutAddress) - 0x0100;

        final int width = codeChunk.getUnsignedByte(baseAddress);
        final int height = codeChunk.getUnsignedByte(baseAddress + 1);
        final int x0 = codeChunk.getUnsignedByte(baseAddress + 2);
        final int y0 = codeChunk.getUnsignedByte(baseAddress + 3);
        final List<Byte> imageData = codeChunk.getBytes(baseAddress, 4 + (width * height));

        // width is half-size because of two pixels per byte
        // x1 is too small by a factor of 8 (div 2 for same reason)
        final ImageData c = new ImageData(imageData, 4 * x0, y0, 2 * width, height);

        drawImageData(c, 4, WHOLE_IMAGE);
    }

    // invertbyte is only ever 0x00, 0x01 (doesn't do anything???), or 0x80 (flip x)
    public void decodeTexture(Chunk chunk, int index, int x0i, int y0i, int invert, Rectangle mask) {
        final int baseAddress = chunk.getWord(index);
        if (baseAddress == 0 || baseAddress > chunk.getSize()) return;

        final int w = chunk.getUnsignedByte(baseAddress);
        final boolean widthSign = (w & 0x80) > 0;
        final int width = w & 0x7f;

        final int height = chunk.getUnsignedByte(baseAddress + 1);

        final boolean invertX = (invert & 0x80) != 0;
        final int offsetX = chunk.getByte(baseAddress + 2);
        final int x0;
        if (invertX) {
            x0 = x0i - offsetX + ((widthSign) ? -1 : 0);
        } else {
            x0 = x0i + offsetX;
        }

        // invertY = invertbyte & 0x40, which never happens
        final int offsetY = chunk.getUnsignedByte(baseAddress + 3);
        final int y0 = y0i + offsetY;

        int callIndex = 0;
        if ((x0 & 0x01) != 0) callIndex |= 0x02;
        if (x0 < 0) callIndex |= 0x04;
        if (invertX) callIndex |= 0x08;

        System.out.format("[%02x] size:(%3d,%3d) offset:(%+4d,%+4d) pos:(%+4d,%+4d) inv:%02x call:%d\n",
                index, width, height, offsetX, offsetY, x0, y0, invert, callIndex);

        final List<Byte> imageData = chunk.getBytes(baseAddress + 4, width * height);
        final ImageData c = new ImageData(imageData, GAMEPLAY.x0() + x0, GAMEPLAY.y0() + y0, 2 * width, height);

        if (callIndex == 0 || callIndex == 4) drawImageData(c, 0, mask);
        if (callIndex == 2 || callIndex == 6)
            drawImageData(c, 0, mask); // x0 is odd so there's only a one-byte draw?
        if (callIndex == 8) {
            drawImageFlip(c, 0, mask);
        }
    }

    private void drawImageData(ImageData c, int basePointer, Rectangle mask) {
        // Corner data is JUST EGA COLOR INDICES packed two to a byte with color 6 treated as a chroma key
        // i.e. if the new value is 6, don't overwrite whatever's there
        int pointer = basePointer;
        for (int y = c.y0; y < c.y0 + c.height; y++) {
            boolean inc = true;
            for (int x = c.x0; x < c.x0 + c.width; x++) {
                inc = !inc;
                final byte chunkByte = c.data().get(pointer);
                final byte newPixel = (byte) (0xf & chunkByte >> (inc ? 0 : 4));
                final int pixelOffset = coordToIndex(x, y);
                if (newPixel != 6 && mask.contains(x, y)) pixels[pixelOffset] = newPixel;
                if (inc) pointer++;
            }
        }
    }

    private void drawImageFlip(ImageData c, int basePointer, Rectangle mask) {
        int pointer = basePointer;
        for (int y = c.y0; y < c.y0 + c.height; y++) {
            boolean inc = true;
            // X coordinate runs backwards, but Pointer fetches data in the same order
            for (int x = (c.x0 + c.width) - 1; x >= c.x0; x--) {
                inc = !inc;
                final byte chunkByte = c.data().get(pointer);
                final byte newPixel = (byte) (0xf & chunkByte >> (inc ? 0 : 4));
                final int pixelOffset = coordToIndex(x, y);
                if (newPixel != 6 && mask.contains(x, y)) pixels[pixelOffset] = newPixel;
                if (inc) pointer++;
            }
        }
    }

    private List<Byte> applyRollingXor(ModifiableChunk chunk) {
        int readAddress = 0x00;
        int writeAddress = 0xa0;
        while (writeAddress < chunk.getSize()) {
            final int b0 = chunk.getQuadWord(readAddress);
            final int b1 = chunk.getQuadWord(writeAddress);
            System.out.format("%04x\n", writeAddress);
            chunk.write(writeAddress, 4, b0 ^ b1);
            readAddress += 4;
            writeAddress += 4;
        }
        return chunk.getBytes(0, chunk.getSize());
    }

    public static void main(String[] args) {
        try (
            final RandomAccessFile exec = new RandomAccessFile("DRAGON.COM", "r");
            final RandomAccessFile data1 = new RandomAccessFile("DATA1", "r");
            final RandomAccessFile data2 = new RandomAccessFile("DATA2", "r");
        ) {
            final int codeSize = (int) (exec.length());
            if ((long) codeSize != exec.length()) {
                throw new RuntimeException("Executable is too big for one byte array");
            }
            final byte[] codeSegment = new byte[codeSize];
            exec.readFully(codeSegment);
            final Chunk codeChunk = new Chunk(codeSegment);

            final ChunkTable chunkTable = new ChunkTable(data1, data2);

            final StandaloneImageDecoder decoder = new StandaloneImageDecoder(codeChunk);

            printChunk(decoder, chunkTable, 0x18);
            printPillar(decoder);
            printHUD(decoder);

            final List<Integer> walls = List.of(0x6e, 0x73, 0x7a, 0x7d, 0x7e);
            final List<Integer> floors = List.of(0x70, 0x75, 0x7c, 0x85);
            final List<Integer> ceilings = List.of(0x6f);
            final List<Integer> decos = List.of(0x71, 0x72, 0x74, 0x77, 0x78, 0x79, 0x7f, 0x80, 0x81);

            for (int chunkId : walls) {
                System.out.format("[%02x] [%02x] ", chunkId, 0);
                printTexture(decoder, chunkTable, chunkId, 0, GAMEPLAY.x0(), GAMEPLAY.y0(), 0, WHOLE_IMAGE, String.format("texture-%02x-00.png", chunkId));
                System.out.format("[%02x] [%02x] ", chunkId, 2);
                printTexture(decoder, chunkTable, chunkId, 2, GAMEPLAY.x0(), GAMEPLAY.y0(), 0, WHOLE_IMAGE, String.format("texture-%02x-02.png", chunkId));

                for (int i = 0; i < WALL_TEXTURE_OFFSET.size(); i++) {
                    final int index = WALL_TEXTURE_OFFSET.get(i);
                    final int x0 = ALU.signExtend(WALL_X_OFFSET.get(i), 2);
                    final int y0 = WALL_Y_OFFSET.get(i);
                    final int invert = WALL_INVERT.get(i);
                    final String filename = String.format("texture-%02x-sq%02x.png", chunkId, i);
                    System.out.format("[%02x] [%02x] ", chunkId, i);
                    printTexture(decoder, chunkTable, chunkId, index, x0, y0, invert, GAMEPLAY, filename);
                }
            }

            for (int chunkId : floors) {
                for (int i = 0; i < FLOOR_TEXTURE_OFFSET.size(); i++) {
                    final int index = FLOOR_TEXTURE_OFFSET.get(i);
                    final int x0 = FLOOR_X_OFFSET.get(i);
                    final int y0 = FLOOR_Y_OFFSET.get(i);
                    final int squareId = FLOOR_SQUARE_ORDER.get(i);
                    final String filename = String.format("texture-%02x-sq%02x.png", chunkId, squareId);
                    System.out.format("[%02x] [%02x] ", chunkId, squareId);
                    printTexture(decoder, chunkTable, chunkId, index, x0, y0, 0, GAMEPLAY, filename);
                }
            }

            for (int chunkId : ceilings) {
                final String filename = String.format("texture-%02x.png", chunkId);
                System.out.format("[%02x] [%02x] ", chunkId, 04);
                printTexture(decoder, chunkTable, chunkId, 4, 0, 0, 0, GAMEPLAY, filename);
            }

            for (int chunkId : decos) {
                for (int i = 0; i < 0x0a; i += 2) {
                    if (i == 2) continue;
                    System.out.format("[%02x] [%02x] ", chunkId, i);
                    final String filename = String.format("texture-%02x-%02x.png", chunkId, i);
                    printTexture(decoder, chunkTable, chunkId, i, GAMEPLAY.x0(), GAMEPLAY.y0(), 0, WHOLE_IMAGE, filename);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void printHUD(StandaloneImageDecoder decoder) throws IOException {
        decoder.clearBuffer((byte)0x06);
        for (int i = 0; i < 10; i++) decoder.decodeRomImage(i); // most HUD sections
        for (int i = 0; i < 16; i++) decoder.decodeRomImage(27 + i); // HUD title bar
        decoder.exportToPNG("hud.png");
        for (int i = 0; i < 4; i++) decoder.decodeCorner(i);
        decoder.exportToPNG("hud-with-corners.png");
    }

    private static void printPillar(StandaloneImageDecoder decoder) throws IOException {
        decoder.clearBuffer((byte)0x06);
        decoder.decodeRomImage(9); // hud pillar
        decoder.decodeRomImage(10); // compass (N)
        decoder.exportToPNG("pillar-with-compass.png");
    }

    private static void printChunk(StandaloneImageDecoder decoder, ChunkTable table, int chunkId) throws IOException {
        final Chunk chunk = table.getModifiableChunk(chunkId);
        if (chunk.getSize() == 0) return;

        final HuffmanDecoder huffman = new HuffmanDecoder(chunk);
        final ModifiableChunk decoded = new ModifiableChunk(huffman.decode());

        decoder.clearBuffer((byte)0x00);
        decoder.decodeChunkImage(decoded);
        decoder.exportToPNG(String.format("image-%02x.png", chunkId));
    }

    private static void printTexture(StandaloneImageDecoder decoder, ChunkTable table, int chunkId,
                                     int index, int x0, int y0, int invert, Rectangle mask, String filename) throws IOException {
        final Chunk chunk = table.getModifiableChunk(chunkId);
        if (chunk.getSize() == 0) return;

        final HuffmanDecoder huffman = new HuffmanDecoder(chunk);
        final ModifiableChunk decoded = new ModifiableChunk(huffman.decode());
        final Chunk decodedChunk = new Chunk(decoded);

        decoder.clearBuffer((byte)0x66);
        decoder.decodeTexture(decodedChunk, index, x0, y0, invert, mask);
        for (int i = 0; i < 4; i++) decoder.decodeCorner(i);
        decoder.exportToPNG("textures-new/" + filename);
    }

    private static final List<Integer> WALL_X_OFFSET = List.of( // 0x536f
            0x0020, 0x0000, 0x0080, 0xffc0, 0x0080, 0x0020, 0xffc0, 0x0080,
            0x0030, 0x0020, 0x0070, 0xfff0, 0x0070, 0x0030, 0xfff0, 0x0070,
            0x0040, 0x0030, 0x0060, 0x0020, 0x0060, 0x0040, 0x0020, 0x0060
    );

    private static final List<Integer> WALL_Y_OFFSET = List.of( // 0x539f
            0x10, 0x00, 0x00, 0x10, 0x10, 0x10, 0x10, 0x10,
            0x20, 0x10, 0x10, 0x20, 0x20, 0x20, 0x20, 0x20,
            0x30, 0x20, 0x20, 0x30, 0x30, 0x30, 0x30, 0x30
    );

    private static final List<Integer> WALL_TEXTURE_OFFSET = List.of( // 0x53ff
            0x04, 0x0c, 0x0c, 0x04, 0x04, 0x04, 0x04, 0x04,
            0x06, 0x0e, 0x0e, 0x06, 0x06, 0x06, 0x06, 0x06,
            0x08, 0x10, 0x10, 0x08, 0x08, 0x08, 0x08, 0x08
    );

    private static final List<Integer> WALL_INVERT = List.of( // 0x542f
            0x01, 0x00, 0x80, 0x01, 0x01, 0x00, 0x00, 0x00,
            0x01, 0x00, 0x80, 0x01, 0x01, 0x00, 0x00, 0x00,
            0x01, 0x00, 0x80, 0x01, 0x01, 0x00, 0x00, 0x00
    );

    private static final List<Integer> FLOOR_X_OFFSET = List.of(
            0x10, 0x00, 0x80, 0x20, 0x00, 0x70, 0x30, 0x00, 0x60
    );

    private static final List<Integer> FLOOR_Y_OFFSET = List.of(
            0x78, 0x78, 0x78, 0x68, 0x68, 0x68, 0x58, 0x58, 0x58
    );

    private static final List<Integer> FLOOR_TEXTURE_OFFSET = List.of(
            0x12, 0x10, 0x14, 0x0c, 0x0a, 0x0e, 0x06, 0x04, 0x08
    );

    private static final List<Integer> FLOOR_SQUARE_ORDER = List.of(
            0xa, 0x9, 0xb, 0x7, 0x6, 0x8, 0x4, 0x3, 0x5
    );
}
