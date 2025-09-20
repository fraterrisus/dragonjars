package com.hitchhikerprod.dragonjars.exec;

import com.hitchhikerprod.dragonjars.data.CharRectangle;
import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.Images;
import com.hitchhikerprod.dragonjars.data.PixelRectangle;
import javafx.scene.image.PixelWriter;

import java.util.List;
import java.util.Objects;

import static com.hitchhikerprod.dragonjars.DragonWarsApp.IMAGE_X;
import static com.hitchhikerprod.dragonjars.DragonWarsApp.IMAGE_Y;
import static com.hitchhikerprod.dragonjars.exec.VideoBuffer.CHROMA_KEY;
import static com.hitchhikerprod.dragonjars.exec.VideoBuffer.WHOLE_IMAGE;

public class VideoHelper {
    // these addresses have already had the -0x0100 offset applied
    public static final int PC_STATUS_BITMASKS = 0x1a61;
    public static final int PC_STATUS_OFFSETS = 0x1a65;
    public static final int PC_STATUS_STRINGS = 0x1a69;

    public static final int LITTLE_MAN_TEXTURE_ADDRESS = 0x6500;
    private static final int HUD_REGION_LUT_ADDRESS = 0x2544;
    private static final int CORNER_LUT_ADDRESS = 0x6428;
    private static final int ROM_IMAGE_LUT_ADDRESS = 0x67c0;
    private static final int FONT_ADDRESS = 0xb8a2;

    // parameters for decodeHudRegion
    public static final int HUD_BOTTOM = 0;
    public static final int HUD_LEFT_BOTTOM = 1;
    public static final int HUD_RIGHT_BOTTOM = 2;
    public static final int HUD_DIVIDER = 3;
    public static final int HUD_RIGHT_TOP = 4;
    public static final int HUD_DW_LOGO = 5;
    public static final int HUD_LEFT_TOP = 6;
    public static final int HUD_TITLE_BAR_0 = 7;
    public static final int HUD_TITLE_BAR_17 = 8;
    public static final int HUD_PILLAR = 9;
    public static final int HUD_GAMEPLAY = 10;
    public static final int HUD_PARTY_AREA = 11;
    public static final int HUD_TITLE_BAR = 12;
    public static final int HUD_MESSAGE_AREA = 13;

    // parameters for decodeRomImage (see also 0-9 above)
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
    public static final int TORCH_1 = 22; // 0x16
    public static final int TORCH_2 = 23;
    public static final int TORCH_3 = 24;
    public static final int TORCH_4 = 25;
    public static final int TORCH_5 = 26;
    public static final int HUD_TITLE_BAR_1 = 27; // through 27+15

    private final Chunk codeChunk;
    private VideoBuffer vb;

    private record ImageData(List<Byte> data, int x0, int y0, int width, int height) {}

    public VideoHelper(Chunk codeChunk) {
        this.codeChunk = codeChunk;
    }

    public VideoBuffer getSnapshot() {
        return new VideoBuffer(this.vb);
    }

    public void setVideoBuffer(VideoBuffer vb) {
        this.vb = vb;
    }

    public void clearBuffer(byte value) {
        Objects.requireNonNull(vb).reset(value);
    }

    public void drawCharacter(int ch, int x0, int y0, boolean invert) {
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

    public void drawCharacter(int ch, int x0, int y0, boolean invert, PixelWriter writer) {
        final int offset = FONT_ADDRESS + ((ch & 0x7f) * 8);
        List<Byte> bitmask = codeChunk.getBytes(offset, 8);
        for (int dy = 0; dy < 8; dy++) {
            final int b = bitmask.get(dy);
            final int mask = 0x80;
            for (int dx = 0; dx < 8; dx++) {
                final boolean draw = (b & (mask >> dx)) > 0;
                final int color = Images.convertColorIndex((draw ^ invert) ? 0x0 : 0xf);
                writer.setArgb(x0 + dx, y0 + dy, color);
            }
        }
    }

    public void drawChunkImage(Chunk chunk) {
        final List<Byte> imageData = chunk.getBytes(0, chunk.getSize());
        final ImageData c = new ImageData(imageData, 0, 0, IMAGE_X, IMAGE_Y);
        drawImageData(c, 0, WHOLE_IMAGE, false);
    }

    public void drawCorner(int index) {
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
        final PixelRectangle gameplayArea = getHudRegionArea(HUD_GAMEPLAY).toPixel();
        final ImageData c = new ImageData(imageData, gameplayArea.x0() + x0, gameplayArea.y0() + y0, 2 * width, height);

        drawImageData(c, 4, gameplayArea, true);
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

    public void drawGrid(int x, int y, byte value) {
        Objects.requireNonNull(vb).set(x, y, value);
    }

    public void drawRectangle(PixelRectangle r, byte value) {
        Objects.requireNonNull(vb);
        for (int y = 0; y < IMAGE_Y; y++) {
            for (int x = 0; x < IMAGE_X; x++) {
                if (r.contains(x, y)) vb.set(x, y, value);
            }
        }
    }

    public void drawRectangle(PixelRectangle r, byte value, PixelWriter writer) {
        final int color = Images.convertColorIndex(value);
        for (int y = 0; y < IMAGE_Y; y++) {
            for (int x = 0; x < IMAGE_X; x++) {
                if (r.contains(x, y)) writer.setArgb(x, y, color);
            }
        }
    }

    public void drawRomImage(int index) {
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

    // invertbyte is only ever 0x00, 0x01 (doesn't do anything???), or 0x80 (flip x)
    public void drawTexture(Chunk chunk, int index, int x0i, int y0i, int invert, PixelRectangle mask) {
        final int baseAddress = chunk.getWord(index);
        if (baseAddress == 0 || baseAddress > chunk.getSize()) return;
        drawTextureData(chunk, baseAddress, x0i, y0i, invert, mask);
    }

    // same as above, but skip the index lookup
    public void drawTextureData(Chunk chunk, int baseAddress, int x0i, int y0i, int invert, PixelRectangle mask) {
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
        final int offsetY = chunk.getByte(baseAddress + 3);
        final int y0 = y0i + offsetY;

        int callIndex = 0;
        if ((x0 & 0x01) != 0) callIndex |= 0x02;
        if (x0 < 0) callIndex |= 0x04;
        if (invertX) callIndex |= 0x08;

//        System.out.format("[%02x] size:(%3d,%3d) offset:(%+4d,%+4d) pos:(%+4d,%+4d) inv:%02x call:%d\n",
//                index, width, height, offsetX, offsetY, x0, y0, invert, callIndex);

        final PixelRectangle gameplayArea = getHudRegionArea(HUD_GAMEPLAY).toPixel();
        final List<Byte> imageData = chunk.getBytes(baseAddress + 4, width * height);
        final ImageData c = new ImageData(imageData, gameplayArea.x0() + x0, gameplayArea.y0() + y0, 2 * width, height);

        if (callIndex == 0 || callIndex == 4) drawImageData(c, 0, mask, true);
        if (callIndex == 2 || callIndex == 6)
            drawImageData(c, 0, mask, true); // x0 is odd so there's only a one-byte draw?
        if (callIndex == 8) {
            drawImageFlip(c, 0, mask);
        }
    }

    public CharRectangle getHudRegionArea(int index) {
        final List<Integer> rect = codeChunk.getBytes(HUD_REGION_LUT_ADDRESS + (4 * index), 4)
                .stream().map(Interpreter::byteToInt).toList();
        if (index == 0xb) {
            // BUGFIX: the data in the executable is wrong for region 0xb
            return new CharRectangle(rect.get(0), rect.get(1) + 24, rect.get(2), rect.get(3) + 24);
        } else {
            return new CharRectangle(rect.get(0), rect.get(1), rect.get(2), rect.get(3));
        }
    }

    public PixelRectangle getRomImageArea(int index) {
        final int lutAddress = ROM_IMAGE_LUT_ADDRESS + (index * 2);
        final int baseAddress = codeChunk.getWord(lutAddress) - 0x0100;

        final int width = 2 * codeChunk.getUnsignedByte(baseAddress);
        final int height = codeChunk.getUnsignedByte(baseAddress + 1);
        final int x0 = 4 * codeChunk.getUnsignedByte(baseAddress + 2);
        final int y0 = codeChunk.getUnsignedByte(baseAddress + 3);

        return new PixelRectangle(x0, y0, x0 + width, y0 + height);
    }

    public void writeTo(String filename, double scale) {
        Objects.requireNonNull(vb).writeTo(filename, scale);
    }

    public void writeTo(PixelWriter writer, PixelRectangle mask, boolean respectChroma) {
        Objects.requireNonNull(vb).writeTo(writer, mask, respectChroma);
    }

    // ----- Draw helpers -----

    private void drawImageData(ImageData c, int basePointer, PixelRectangle mask, boolean respectChroma) {
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
                final boolean writePixel = mask.contains(x, y) && (!respectChroma || newPixel != CHROMA_KEY);
                if (writePixel) vb.set(x, y, newPixel);
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
}
