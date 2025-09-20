package com.hitchhikerprod.dragonjars.exec;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.ChunkTable;
import com.hitchhikerprod.dragonjars.data.HuffmanDecoder;
import com.hitchhikerprod.dragonjars.data.Images;
import com.hitchhikerprod.dragonjars.data.ModifiableChunk;
import com.hitchhikerprod.dragonjars.data.PixelRectangle;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import static com.hitchhikerprod.dragonjars.DragonWarsApp.IMAGE_X;
import static com.hitchhikerprod.dragonjars.DragonWarsApp.IMAGE_Y;
import static com.hitchhikerprod.dragonjars.exec.VideoBuffer.WHOLE_IMAGE;

public class VideoHelperRunner {

    public static void main(String[] args) {
        try (
                final RandomAccessFile exec = new RandomAccessFile("/home/bcordes/pc-games/dragonwars/DRAGON.COM", "r");
                final RandomAccessFile data1 = new RandomAccessFile("/home/bcordes/pc-games/dragonwars/DATA1", "r");
                final RandomAccessFile data2 = new RandomAccessFile("/home/bcordes/pc-games/dragonwars/DATA2", "r");
        ) {
            final int codeSize = (int) (exec.length());
            if ((long) codeSize != exec.length()) {
                throw new RuntimeException("Executable is too big for one byte array");
            }
            final byte[] codeSegment = new byte[codeSize];
            exec.readFully(codeSegment);
            final Chunk codeChunk = new Chunk(codeSegment);

            final ChunkTable chunkTable = new ChunkTable(data1, data2);

            final VideoHelper decoder = new VideoHelper(codeChunk);
            decoder.setVideoBuffer(new VideoBuffer());

            printChunk(decoder, chunkTable, 0x18);
            printPillar(decoder);
            printHUD(decoder);
            buildHudWireframe(decoder);
            buildRomWireframe(decoder);

            final List<Integer> walls = List.of(0x6e, 0x73, 0x7a, 0x7d, 0x7e);
            final List<Integer> floors = List.of(0x70, 0x75, 0x7c, 0x85);
            final List<Integer> ceilings = List.of(0x6f);
            final List<Integer> decos = List.of(0x71, 0x72, 0x74, 0x77, 0x78, 0x79, 0x7f, 0x80, 0x81);

            final PixelRectangle gameplayArea = decoder.getHudRegionArea(VideoHelper.HUD_GAMEPLAY).toPixel();

            decoder.clearBuffer((byte)0x66);
            decoder.drawTextureData(codeChunk, VideoHelper.LITTLE_MAN_TEXTURE_ADDRESS, gameplayArea.x0(), gameplayArea.y0(), 0, WHOLE_IMAGE);
            for (int i = 0; i < 4; i++) decoder.drawCorner(i);
            decoder.writeTo("textures-new/little-man.png", 4.0);

            for (int chunkId : walls) {
//                System.out.format("[%02x] [%02x] ", chunkId, 0);
                printTexture(decoder, chunkTable, chunkId, 0, gameplayArea.x0(), gameplayArea.y0(), 0, WHOLE_IMAGE, String.format("texture-%02x-00.png", chunkId));
//                System.out.format("[%02x] [%02x] ", chunkId, 2);
                printTexture(decoder, chunkTable, chunkId, 2, gameplayArea.x0(), gameplayArea.y0(), 0, WHOLE_IMAGE, String.format("texture-%02x-02.png", chunkId));

                for (int i = 0; i < WALL_TEXTURE_OFFSET.size(); i++) {
                    final int index = WALL_TEXTURE_OFFSET.get(i);
                    final int x0 = ALU.signExtend(WALL_X_OFFSET.get(i), 2);
                    final int y0 = WALL_Y_OFFSET.get(i);
                    final int invert = WALL_INVERT.get(i);
                    final String filename = String.format("texture-%02x-sq%02x.png", chunkId, i);
//                    System.out.format("[%02x] [%02x] ", chunkId, i);
                    printTexture(decoder, chunkTable, chunkId, index, x0, y0, invert, gameplayArea, filename);
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
                    printTexture(decoder, chunkTable, chunkId, index, x0, y0, 0, gameplayArea, filename);
                }
            }

            for (int chunkId : ceilings) {
                final String filename = String.format("texture-%02x.png", chunkId);
//                System.out.format("[%02x] [%02x] ", chunkId, 04);
                printTexture(decoder, chunkTable, chunkId, 4, 0, 0, 0, gameplayArea, filename);
            }

            for (int chunkId : decos) {
                for (int i = 0; i < 0x0a; i += 2) {
                    if (i == 2) continue;
//                    System.out.format("[%02x] [%02x] ", chunkId, i);
                    final String filename = String.format("texture-%02x-%02x.png", chunkId, i);
                    printTexture(decoder, chunkTable, chunkId, i, gameplayArea.x0(), gameplayArea.y0(), 0, WHOLE_IMAGE, filename);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void printHUD(VideoHelper decoder) throws IOException {
        decoder.clearBuffer((byte)0x06);
        for (int i = 0; i < 10; i++) decoder.drawRomImage(i); // most HUD sections
        for (int i = 0; i < 16; i++) decoder.drawRomImage(27 + i); // HUD title bar
        decoder.writeTo("hud.png", 4.0);
        for (int i = 0; i < 4; i++) decoder.drawCorner(i);
        decoder.writeTo("hud-with-corners.png", 4.0);
    }

    private static void buildHudWireframe(VideoHelper decoder) throws IOException {
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

        for (int regionId = 0; regionId < 14; regionId++) {
            final PixelRectangle region = decoder.getHudRegionArea(regionId).toPixel();
            System.out.println(region);
            g.drawString(String.format("%x", regionId), (4 * region.x0()) + 4, (4 * region.y0()) + 22);
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

    private static void buildRomWireframe(VideoHelper decoder) throws IOException {
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
        regionIds.add(VideoHelper.COMPASS_N);
        regionIds.add(VideoHelper.EYE_CLOSED);
        regionIds.add(VideoHelper.SHIELD);
        regionIds.add(VideoHelper.TORCH_1);
        for (int i = 0; i < 16; i++) regionIds.add(27 + i);

        for (int i : regionIds) {
            final PixelRectangle region = decoder.getRomImageArea(i);
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

        ImageIO.write(image, "png", new File("rom-wireframe.png"));
    }

    private static void printPillar(VideoHelper decoder) throws IOException {
        decoder.clearBuffer((byte)0x06);
        decoder.drawRomImage(9); // hud pillar
        decoder.drawRomImage(10); // compass (N)
        decoder.writeTo("pillar-with-compass.png", 4.0);
    }

    private static void printChunk(VideoHelper decoder, ChunkTable table, int chunkId) throws IOException {
        final Chunk chunk = table.getModifiableChunk(chunkId);
        if (chunk.getSize() == 0) return;

        final HuffmanDecoder huffman = new HuffmanDecoder(chunk);
        final ModifiableChunk decoded = new ModifiableChunk(huffman.decode());
        applyRollingXor(decoded);

        decoder.clearBuffer((byte)0x00);
        decoder.drawChunkImage(decoded);
        decoder.writeTo(String.format("image-%02x.png", chunkId), 4.0);
    }

    private static void applyRollingXor(ModifiableChunk chunk) {
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
    }

    private static void printTexture(VideoHelper decoder, ChunkTable table, int chunkId,
                                     int index, int x0, int y0, int invert,
                                     PixelRectangle mask, String filename) throws IOException {
        final Chunk chunk = table.getModifiableChunk(chunkId);
        if (chunk.getSize() == 0) return;

        final HuffmanDecoder huffman = new HuffmanDecoder(chunk);
        final ModifiableChunk decoded = new ModifiableChunk(huffman.decode());
        final Chunk decodedChunk = new Chunk(decoded);

        decoder.clearBuffer((byte)0x66);
        decoder.drawTexture(decodedChunk, index, x0, y0, invert, mask);
        for (int i = 0; i < 4; i++) decoder.drawCorner(i);
        decoder.writeTo("textures-new/" + filename, 4.0);
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
    );}
