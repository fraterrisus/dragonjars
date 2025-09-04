package com.hitchhikerprod.dragonjars.exec;

import com.hitchhikerprod.dragonjars.data.CharRectangle;
import com.hitchhikerprod.dragonjars.data.Images;
import com.hitchhikerprod.dragonjars.data.PixelRectangle;
import javafx.scene.image.PixelWriter;

import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.hitchhikerprod.dragonjars.DragonWarsApp.IMAGE_X;
import static com.hitchhikerprod.dragonjars.DragonWarsApp.IMAGE_Y;
import static com.hitchhikerprod.dragonjars.DragonWarsApp.SCALE_FACTOR;

/**
 * A class representing an image, made up of one value (color index) per pixel.
 * @see com.hitchhikerprod.dragonjars.DragonWarsApp::IMAGE_X
 * @see com.hitchhikerprod.dragonjars.DragonWarsApp::IMAGE_Y
 */
public class VideoBuffer {
    public static byte CHROMA_KEY = (byte) 0x06;

    public static final PixelRectangle AUTOMAP = new PixelRectangle(0, 0, IMAGE_X, IMAGE_Y); // FIXME
    public static final PixelRectangle GAMEPLAY = new PixelRectangle(0x010, 0x008, 0x0b0, 0x090);
    public static final PixelRectangle MESSAGE_PANE = new PixelRectangle(0x008, 0x098, 0x138, 0x0b8);
    public static final PixelRectangle WHOLE_IMAGE = new PixelRectangle(0, 0, IMAGE_X, IMAGE_Y);
    public static final PixelRectangle PARTY_INFO = new PixelRectangle(0x0d8, 0x008, 0x138, 0x090);
    public static final PixelRectangle TITLE_BAR = new PixelRectangle(0x020, 0x000, 0x098, 0x008);

    public static final CharRectangle DEFAULT_RECT = new CharRectangle(0x01, 0x08, 0x27, 0xb8);

    private final byte[] buffer;

    /**
     * Create a new VideoBuffer.
     */
    public VideoBuffer() {
        this.buffer = new byte[IMAGE_X * IMAGE_Y];
    }

    /**
     * Create a new VideoBuffer with every pixel initialized to the given value.
     * @param value The new value
     */
    public VideoBuffer(byte value) {
        this();
        Arrays.fill(buffer(), value);
    }

    /**
     * Create a new VideoBuffer by copying the values from an existing VideoBuffer.
     * @param that The source VideoBuffer to copy
     */
    public VideoBuffer(VideoBuffer that) {
        this();
        System.arraycopy(that.buffer(), 0, this.buffer(), 0, this.buffer().length);
    }

    /**
     * Load the buffer with a specific value (for instance, 0x06 `CHROMA_KEY`).
     * @param value
     */
    public void reset(byte value) {
        Arrays.fill(buffer(), value);
    }

    /**
     * Returns a reference to the underlying byte array.
     * @return byte[]
     */
    public byte[] buffer() {
        return buffer;
    }

    /**
     * Returns a (new) List of Byte, which is a copy of the underlying byte array.
     * @return List&lt;Byte&gt;
     */
    public List<Byte> toList() {
        final List<Byte> l = new ArrayList<>();
        for (int i = 0; i < buffer().length; i++) l.add(buffer()[i]);
        return l;
    }

    /**
     * Fetch a single pixel.
     * @param x X coordinate of pixel
     * @param y Y coordinate of pixel
     * @return The color value of the given pixel.
     */
    public byte get(int x, int y) {
        return buffer()[indexOf(x, y)];
    }

    /**
     * Set a single pixel to a new value.
     * @param x X coordinate of pixel
     * @param y Y coordinate of pixel
     * @param value New color value of the given pixel.
     */
    public void set(int x, int y, byte value) {
        buffer()[indexOf(x, y)] = value;
    }

    /**
     * Writes the contents of this VideoBuffer to that VideoBuffer. Respects the chroma key, i.e. does not write if
     * the source color index is 0x06.
     * @param that The destination VideoBuffer
     * @param mask A PixelRectangle describing the area that should be copied. Pixels not `#inside()` this area will
     *            not be copied.
     */
    public void writeTo(VideoBuffer that, PixelRectangle mask) {
        byte[] src = this.buffer();
        byte[] dest = that.buffer();
        for (int y = 0; y < IMAGE_Y; y++) {
            for (int x = 0; x < IMAGE_X; x++) {
                if (!mask.contains(x, y)) continue;
                final int offset = indexOf(x, y);
                if (src[offset] != CHROMA_KEY) dest[offset] = src[offset];
            }
        }
    }

    /**
     * Writes the contents of this VideoBuffer to an Image (via the provided PixelWriter).
     * @param writer A PixelWriter object for the desired target Image.
     * @param mask A PixelRectangle describing the area that should be copied. Pixels not `#inside()` this area will
     *     not be copied.
     * @param respectChroma If true, the chroma key will be respected, i.e. source color index 0x06 will result in
     *     not writing certain pixels to the target image.
     */
    public void writeTo(PixelWriter writer, PixelRectangle mask, boolean respectChroma) {
        for (int y = 0; y < IMAGE_Y; y++) {
            for (int x = 0; x < IMAGE_X; x++) {
                if (!mask.contains(x, y)) continue;
                final int colorIndex = buffer()[indexOf(x, y)];
                if (respectChroma && colorIndex == CHROMA_KEY) continue;
                final int colorValue = Images.convertColorIndex(colorIndex);
                writer.setArgb(x, y, colorValue);
            }
        }
    }

    /**
     * Writes the contents of this VideoBuffer to a PNG image file.
     * @param filename The filename to write (should end in ".png")
     */
    public void writeTo(String filename) {
        final BufferedImage image = new BufferedImage(IMAGE_X, IMAGE_Y, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < IMAGE_Y; y++) {
            for (int x = 0; x < IMAGE_X; x++) {
                final int colorIndex = buffer()[indexOf(x, y)];
                final int colorValue = Images.convertColorIndex(colorIndex);
                image.setRGB(x, y, colorValue);
            }
        }
        try {
            ImageIO.write(scale(image, SCALE_FACTOR, AffineTransformOp.TYPE_NEAREST_NEIGHBOR),
                    "png", new File(filename));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
    
    private int indexOf(int x, int y) {
        return x + (IMAGE_X * y);
    }

    public static BufferedImage scale(final BufferedImage before, final double scale, final int type) {
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
}
