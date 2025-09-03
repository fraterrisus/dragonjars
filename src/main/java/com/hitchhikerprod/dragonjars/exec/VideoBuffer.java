package com.hitchhikerprod.dragonjars.exec;

import com.hitchhikerprod.dragonjars.data.Images;
import com.hitchhikerprod.dragonjars.data.Rectangle;
import javafx.scene.image.PixelWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.hitchhikerprod.dragonjars.DragonWarsApp.IMAGE_X;
import static com.hitchhikerprod.dragonjars.DragonWarsApp.IMAGE_Y;

/**
 * A class representing an image, made up of one value (color index) per pixel.
 * @see com.hitchhikerprod.dragonjars.DragonWarsApp::IMAGE_X
 * @see com.hitchhikerprod.dragonjars.DragonWarsApp::IMAGE_Y
 */
public class VideoBuffer {
    public static byte CHROMA_KEY = (byte) 0x06;

    public static final Rectangle GAMEPLAY = new Rectangle(0x10, 0x08, 0xb0, 0x90);
    public static final Rectangle WHOLE_IMAGE = new Rectangle(0, 0, IMAGE_X, IMAGE_Y);

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
     * @param mask A Rectangle describing the area that should be copied. Pixels not `Rectangle#inside()` this area
     *             will not be copied.
     */
    public void writeTo(VideoBuffer that, Rectangle mask) {
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
     * Writes the contents of this VideoBuffer to an Image (via the provided PixelWriter). Respects the chroma key,
     * i.e. does not write if the source color index is 0x06.
     * @param writer A PixelWriter object for the desired target Image.
     * @param mask A Rectangle describing the area that should be copied. Pixels not `Rectangle#inside()` this area
     *             will not be copied.
     */
    public void writeTo(PixelWriter writer, Rectangle mask) {
        for (int y = 0; y < IMAGE_Y; y++) {
            for (int x = 0; x < IMAGE_X; x++) {
                if (!mask.contains(x, y)) continue;
                final int colorIndex = buffer()[indexOf(x, y)];
                if (colorIndex == CHROMA_KEY) continue;
                final int colorValue = Images.convertColorIndex(colorIndex);
                writer.setArgb(x, y, colorValue);
            }
        }
    }

    private int indexOf(int x, int y) {
        return x + (IMAGE_X * y);
    }
}
