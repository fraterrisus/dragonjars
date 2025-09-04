package com.hitchhikerprod.dragonjars.data;

import com.hitchhikerprod.dragonjars.exec.ALU;
import com.hitchhikerprod.dragonjars.exec.VideoBuffer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static com.hitchhikerprod.dragonjars.exec.VideoBuffer.CHROMA_KEY;
import static com.hitchhikerprod.dragonjars.exec.VideoBuffer.GAMEPLAY;
import static com.hitchhikerprod.dragonjars.exec.VideoBuffer.WHOLE_IMAGE;

public class ImageDecoder {
    public static final int IMAGE_X = 320;
    public static final int IMAGE_Y = 200;

    private static final int FONT_ADDRESS = 0xb8a2;
    private static final int CORNER_LUT_ADDRESS = 0x6428;
    private static final int ROM_IMAGE_LUT_ADDRESS = 0x67c0;

    // parameters for decodeRomImage
    public static final int HUD_REGION_0 = 0;
    public static final int HUD_REGION_1 = 1;
    public static final int HUD_REGION_2 = 2;
    public static final int HUD_REGION_3 = 3;
    public static final int HUD_REGION_4 = 4;
    public static final int HUD_REGION_5 = 5;
    public static final int HUD_REGION_6 = 6;
    public static final int HUD_REGION_7 = 7;
    public static final int HUD_REGION_8 = 8;
    public static final int HUD_REGION_9 = 9;
    public static final int COMPASS_N = 10;
    public static final int COMPASS_E = 11;
    public static final int COMPASS_S = 12;
    public static final int COMPASS_W = 13;
    public static final int EYE_CLOSED = 14;
    public static final int EYE_MOSTLY_CLOSED = 15;
    public static final int EYE_PARTLY_CLOSED = 16;
    public static final int EYE_OPEN = 17;
    public static final int EYE_CENTER = 18;
    public static final int EYE_RIGHT = 19;
    public static final int SHIELD = 20;
    public static final int TORCH_UNLIT = 21;
    public static final int TORCH_1 = 22;
    public static final int TORCH_2 = 23;
    public static final int TORCH_3 = 24;
    public static final int TORCH_4 = 25;
    public static final int TORCH_5 = 26;
    public static final int HUD_TITLE_BAR = 27; // through 27+15

    private final Chunk codeChunk;
    private VideoBuffer vb;

    private record ImageData(List<Byte> data, int x0, int y0, int width, int height) {}

    public ImageDecoder(Chunk codeChunk) {
        this.codeChunk = codeChunk;
    }

    public void setVideoBuffer(VideoBuffer vb) {
        System.out.println("setVideoBuffer(" + vb + ")");
        this.vb = vb;
    }

    public void withVideoBuffer(VideoBuffer vb, Consumer<ImageDecoder> callback) {
        final VideoBuffer oldVb = this.vb;
        setVideoBuffer(vb);
        callback.accept(this);
        setVideoBuffer(oldVb);
    }

    public void clearBuffer(byte value) {
        Objects.requireNonNull(vb).reset(value);
    }

    public void drawGrid() {
        Objects.requireNonNull(vb);
        for (int y = 0; y < IMAGE_Y; y += 8) {
            for (int x = 0; x < IMAGE_X; x++) {
                vb.set(x, y, CHROMA_KEY);
            }
        }
        for (int x = 0; x < IMAGE_X; x += 8) {
            for (int y = 0; y < IMAGE_Y; y++) {
                vb.set(x, y, CHROMA_KEY);
            }
        }
    }

    public void drawPixel(int x, int y, byte value) {
        vb.set(x, y, value);
    }

    public void drawRectangle(PixelRectangle r, byte value) {
        Objects.requireNonNull(vb);
        for (int y = 0; y < IMAGE_Y; y++) {
            for (int x = 0; x < IMAGE_X; x++) {
                if (r.contains(x, y)) vb.set(x, y, value);
            }
        }
    }

    public void exportToPNG(String filename) {
        Objects.requireNonNull(vb).writeTo(filename);
    }

    public void decodeChar(int ch, int x0, int y0, boolean invert) {
        Objects.requireNonNull(vb);
        final int offset = FONT_ADDRESS + ((ch & 0x7f) * 8);
        List<Byte> bitmask = codeChunk.getBytes(offset, 8);
        for (int dy = 0; dy < 8; dy++) {
            final int b = bitmask.get(dy);
            final int mask = 0x80;
            for (int dx = 0; dx < 8; dx++) {
                final boolean draw = (b & (mask >> dx)) > 0;
                vb.set(x0 + dx, y0 + dy, (byte)((draw ^ invert) ? 0x0 : 0xf));
            }
        }
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

    public void eraseRomImage(int index) {
        drawRectangle(decodeRomImageArea(index), CHROMA_KEY);
    }

    public void decodeRomImage(int index) {
        final int lutAddress = ROM_IMAGE_LUT_ADDRESS + (index * 2);
        final int baseAddress = codeChunk.getWord(lutAddress) - 0x0100;

        final int width = 2 * codeChunk.getUnsignedByte(baseAddress);
        final int height = codeChunk.getUnsignedByte(baseAddress + 1);
        final int x0 = 4 * codeChunk.getUnsignedByte(baseAddress + 2);
        final int y0 = codeChunk.getUnsignedByte(baseAddress + 3);

        final int size = 4 + (codeChunk.getUnsignedByte(baseAddress) * codeChunk.getUnsignedByte(baseAddress + 1));
        final List<Byte> imageData = codeChunk.getBytes(baseAddress, size);

        // width is half-size because of two pixels per byte
        // x1 is too small by a factor of 8 (div 2 for same reason)
        final ImageData c = new ImageData(imageData, x0, y0, width, height);

        drawImageDataWithoutChroma(c, 4, WHOLE_IMAGE);
    }

    public PixelRectangle decodeRomImageArea(int index) {
        final int lutAddress = ROM_IMAGE_LUT_ADDRESS + (index * 2);
        final int baseAddress = codeChunk.getWord(lutAddress) - 0x0100;

        final int width = 2 * codeChunk.getUnsignedByte(baseAddress);
        final int height = codeChunk.getUnsignedByte(baseAddress + 1);
        final int x0 = 4 * codeChunk.getUnsignedByte(baseAddress + 2);
        final int y0 = codeChunk.getUnsignedByte(baseAddress + 3);

        return new PixelRectangle(x0, y0, x0 + width, y0 + height);
    }

    // invertbyte is only ever 0x00, 0x01 (doesn't do anything???), or 0x80 (flip x)
    public void decodeTexture(Chunk chunk, int index, int x0i, int y0i, int invert, PixelRectangle mask) {
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

//        System.out.format("[%02x] size:(%3d,%3d) offset:(%+4d,%+4d) pos:(%+4d,%+4d) inv:%02x call:%d\n",
//                index, width, height, offsetX, offsetY, x0, y0, invert, callIndex);

        final List<Byte> imageData = chunk.getBytes(baseAddress + 4, width * height);
        final ImageData c = new ImageData(imageData, GAMEPLAY.x0() + x0, GAMEPLAY.y0() + y0, 2 * width, height);

        if (callIndex == 0 || callIndex == 4) drawImageData(c, 0, mask);
        if (callIndex == 2 || callIndex == 6)
            drawImageData(c, 0, mask); // x0 is odd so there's only a one-byte draw?
        if (callIndex == 8) {
            drawImageFlip(c, 0, mask);
        }
    }

    private void drawImageData(ImageData c, int basePointer, PixelRectangle mask) {
        // Corner data is JUST EGA COLOR INDICES packed two to a byte with color 6 treated as a chroma key
        // i.e. if the new value is 6, don't overwrite whatever's there
        Objects.requireNonNull(vb);
        int pointer = basePointer;
        for (int y = c.y0; y < c.y0 + c.height; y++) {
            boolean inc = true;
            for (int x = c.x0; x < c.x0 + c.width; x++) {
                inc = !inc;
                final byte chunkByte = c.data().get(pointer);
                final byte newPixel = (byte) (0xf & chunkByte >> (inc ? 0 : 4));
                if (newPixel != CHROMA_KEY && mask.contains(x, y)) vb.set(x, y, newPixel);
                if (inc) pointer++;
            }
        }
    }

    private void drawImageDataWithoutChroma(ImageData c, int basePointer, PixelRectangle mask) {
        // Corner data is JUST EGA COLOR INDICES packed two to a byte with color 6 treated as a chroma key
        // i.e. if the new value is 6, don't overwrite whatever's there
        Objects.requireNonNull(vb);
        int pointer = basePointer;
        for (int y = c.y0; y < c.y0 + c.height; y++) {
            boolean inc = true;
            for (int x = c.x0; x < c.x0 + c.width; x++) {
                inc = !inc;
                final byte chunkByte = c.data().get(pointer);
                final byte newPixel = (byte) (0xf & chunkByte >> (inc ? 0 : 4));
                if (mask.contains(x, y)) vb.set(x, y, newPixel);
                if (inc) pointer++;
            }
        }
    }

    private void drawImageFlip(ImageData c, int basePointer, PixelRectangle mask) {
        Objects.requireNonNull(vb);
        int pointer = basePointer;
        for (int y = c.y0; y < c.y0 + c.height; y++) {
            boolean inc = true;
            // X coordinate runs backwards, but Pointer fetches data in the same order
            for (int x = (c.x0 + c.width) - 1; x >= c.x0; x--) {
                inc = !inc;
                final byte chunkByte = c.data().get(pointer);
                final byte newPixel = (byte) (0xf & chunkByte >> (inc ? 0 : 4));
                if (newPixel != CHROMA_KEY && mask.contains(x, y)) vb.set(x, y, newPixel);
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
//            System.out.format("%04x\n", writeAddress);
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

            final ImageDecoder decoder = new ImageDecoder(codeChunk);
            decoder.setVideoBuffer(new VideoBuffer());

            printChunk(decoder, chunkTable, 0x18);
            printPillar(decoder);
            printHUD(decoder);
            buildHUDWireframe(decoder);

            final List<Integer> walls = List.of(0x6e, 0x73, 0x7a, 0x7d, 0x7e);
            final List<Integer> floors = List.of(0x70, 0x75, 0x7c, 0x85);
            final List<Integer> ceilings = List.of(0x6f);
            final List<Integer> decos = List.of(0x71, 0x72, 0x74, 0x77, 0x78, 0x79, 0x7f, 0x80, 0x81);

            for (int chunkId : walls) {
//                System.out.format("[%02x] [%02x] ", chunkId, 0);
                printTexture(decoder, chunkTable, chunkId, 0, GAMEPLAY.x0(), GAMEPLAY.y0(), 0, WHOLE_IMAGE, String.format("texture-%02x-00.png", chunkId));
//                System.out.format("[%02x] [%02x] ", chunkId, 2);
                printTexture(decoder, chunkTable, chunkId, 2, GAMEPLAY.x0(), GAMEPLAY.y0(), 0, WHOLE_IMAGE, String.format("texture-%02x-02.png", chunkId));

                for (int i = 0; i < WALL_TEXTURE_OFFSET.size(); i++) {
                    final int index = WALL_TEXTURE_OFFSET.get(i);
                    final int x0 = ALU.signExtend(WALL_X_OFFSET.get(i), 2);
                    final int y0 = WALL_Y_OFFSET.get(i);
                    final int invert = WALL_INVERT.get(i);
                    final String filename = String.format("texture-%02x-sq%02x.png", chunkId, i);
//                    System.out.format("[%02x] [%02x] ", chunkId, i);
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
//                    System.out.format("[%02x] [%02x] ", chunkId, squareId);
                    printTexture(decoder, chunkTable, chunkId, index, x0, y0, 0, GAMEPLAY, filename);
                }
            }

            for (int chunkId : ceilings) {
                final String filename = String.format("texture-%02x.png", chunkId);
//                System.out.format("[%02x] [%02x] ", chunkId, 04);
                printTexture(decoder, chunkTable, chunkId, 4, 0, 0, 0, GAMEPLAY, filename);
            }

            for (int chunkId : decos) {
                for (int i = 0; i < 0x0a; i += 2) {
                    if (i == 2) continue;
//                    System.out.format("[%02x] [%02x] ", chunkId, i);
                    final String filename = String.format("texture-%02x-%02x.png", chunkId, i);
                    printTexture(decoder, chunkTable, chunkId, i, GAMEPLAY.x0(), GAMEPLAY.y0(), 0, WHOLE_IMAGE, filename);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void printHUD(ImageDecoder decoder) throws IOException {
        decoder.clearBuffer((byte)0x06);
        for (int i = 0; i < 10; i++) decoder.decodeRomImage(i); // most HUD sections
        for (int i = 0; i < 16; i++) decoder.decodeRomImage(27 + i); // HUD title bar
        decoder.exportToPNG("hud.png");
        for (int i = 0; i < 4; i++) decoder.decodeCorner(i);
        decoder.exportToPNG("hud-with-corners.png");
    }

    private static void buildHUDWireframe(ImageDecoder decoder) throws IOException {
        final BufferedImage image = new BufferedImage(4 * IMAGE_X, 4 * IMAGE_Y, BufferedImage.TYPE_INT_RGB);
        final int bgColor = Images.convertColorIndex(1);
        for (int y = 0; y < 4 * IMAGE_Y; y++) {
            for (int x = 0; x < 4 * IMAGE_X; x++) {
                image.setRGB(x, y, bgColor);
            }
        }

        final Graphics g = image.getGraphics();
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        g.setColor(Color.GREEN);

        final List<Integer> regionIds = new ArrayList<>();
        for (int i = 0; i < 10; i++) regionIds.add(i);
        regionIds.add(ImageDecoder.COMPASS_N);
        regionIds.add(ImageDecoder.EYE_CLOSED);
        regionIds.add(ImageDecoder.SHIELD);
        regionIds.add(ImageDecoder.TORCH_1);
        for (int i = 0; i < 16; i++) regionIds.add(27 + i);

        for (int i : regionIds) {
            final PixelRectangle region = decoder.decodeRomImageArea(i);
            System.out.println(region);
            g.drawString(String.format("%x", i), (4 * region.x0()) + 4, (4 * region.y0()) + 22);
            for (int w = 0; w < 2; w++) {
                g.drawRect(
                        (4 * region.x0()) + w,
                        (4 * region.y0()) + w,
                        (4 * (region.x1() - region.x0())) - (2 * w),
                        (4 * (region.y1() - region.y0())) - (2 * w)
                );
            }
        }

        ImageIO.write(image, "png", new File("hud-wireframe.png"));
    }

    private static void printPillar(ImageDecoder decoder) throws IOException {
        decoder.clearBuffer((byte)0x06);
        decoder.decodeRomImage(9); // hud pillar
        decoder.decodeRomImage(10); // compass (N)
        decoder.exportToPNG("pillar-with-compass.png");
    }

    private static void printChunk(ImageDecoder decoder, ChunkTable table, int chunkId) throws IOException {
        final Chunk chunk = table.getModifiableChunk(chunkId);
        if (chunk.getSize() == 0) return;

        final HuffmanDecoder huffman = new HuffmanDecoder(chunk);
        final ModifiableChunk decoded = new ModifiableChunk(huffman.decode());

        decoder.clearBuffer((byte)0x00);
        decoder.decodeChunkImage(decoded);
        decoder.exportToPNG(String.format("image-%02x.png", chunkId));
    }

    private static void printTexture(ImageDecoder decoder, ChunkTable table, int chunkId,
                                     int index, int x0, int y0, int invert, PixelRectangle mask, String filename) throws IOException {
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
