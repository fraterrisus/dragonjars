package com.hitchhikerprod.dragonjars.data;

import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.function.Function;

public class Images {
    public static WritableImage blankImage(int dimX, int dimY) {
        final WritableImage image = new WritableImage(dimX, dimY);
        final PixelWriter writer = image.getPixelWriter();
        for (int y = 0; y < dimY; y++) {
            for (int x = 0; x < dimX; x++) {
                writer.setColor(x, y, Color.gray(0.40));
            }
        }
        return image;
    }

    public static void convertEgaData(PixelWriter writer, Function<Integer, Integer> lookup,
                                      int baseIndex, int x, int y) {
        final int[] indices = new int[4];
        final int[] words = new int[4];

        for (int i = 0; i < 4; i++) { indices[i] = lookup.apply(baseIndex + i); }

        words[3] = get2A80(indices[0]) |
                get2E80(indices[1]) |
                get3280(indices[2]) |
                get3680(indices[3]);
        words[1] = get2C80(indices[0]) |
                get3080(indices[1]) |
                get3480(indices[2]) |
                get3880(indices[3]);

        words[2] = (words[3] & 0xff00) >> 8;
        words[3] =  words[3] & 0x00ff;
        words[0] = (words[1] & 0xff00) >> 8;
        words[1] =  words[1] & 0x00ff;

        for (int i = 7; i >= 0; i--) {
            final int color = ((words[3] >> i) & 0x01) << 3
                    | ((words[2] >> i) & 0x01) << 2
                    | ((words[1] >> i) & 0x01) << 1
                    | ((words[0] >> i) & 0x01);
            final int rx = x + (7 - i);
            final int colorIndex = convertColorIndex(color);
            // System.out.printf("(%3d,%3d) = %02d %08x\n", rx, y, color, colorIndex);
            writer.setArgb(rx, y, colorIndex);
        }
    }

    /* These tables are generated during video_setup_ega() at 0x0322 and written to the video buffer (seg 0x0f69).
     * EGA drawing routines refer to them to translate images into video drawing commands. The lookup functions here
     * have been validated against the computed values. */

    public static int get2A80(int index) {
        int value = 0;
        if ((index & 0x04) > 0) value |= 0x4000;
        if ((index & 0x08) > 0) value |= 0x0040;
        if ((index & 0x40) > 0) value |= 0x8000;
        if ((index & 0x80) > 0) value |= 0x0080;
        return value;
    }

    public static int get2C80(int index) {
        int value = 0;
        if ((index & 0x01) > 0) value |= 0x4000;
        if ((index & 0x02) > 0) value |= 0x0040;
        if ((index & 0x10) > 0) value |= 0x8000;
        if ((index & 0x20) > 0) value |= 0x0080;
        return value;
    }

    public static int get2E80(int index) {
        int value = 0;
        if ((index & 0x04) > 0) value |= 0x1000;
        if ((index & 0x08) > 0) value |= 0x0010;
        if ((index & 0x40) > 0) value |= 0x2000;
        if ((index & 0x80) > 0) value |= 0x0020;
        return value;
    }

    public static int get3080(int index) {
        int value = 0;
        if ((index & 0x01) > 0) value |= 0x1000;
        if ((index & 0x02) > 0) value |= 0x0010;
        if ((index & 0x10) > 0) value |= 0x2000;
        if ((index & 0x20) > 0) value |= 0x0020;
        return value;
    }

    public static int get3280(int index) {
        int value = 0;
        if ((index & 0x04) > 0) value |= 0x0400;
        if ((index & 0x08) > 0) value |= 0x0004;
        if ((index & 0x40) > 0) value |= 0x0800;
        if ((index & 0x80) > 0) value |= 0x0008;
        return value;
    }

    public static int get3480(int index) {
        int value = 0;
        if ((index & 0x01) > 0) value |= 0x0400;
        if ((index & 0x02) > 0) value |= 0x0004;
        if ((index & 0x10) > 0) value |= 0x0800;
        if ((index & 0x20) > 0) value |= 0x0008;
        return value;
    }

    public static int get3680(int index) {
        int value = 0;
        if ((index & 0x04) > 0) value |= 0x0100;
        if ((index & 0x08) > 0) value |= 0x0001;
        if ((index & 0x40) > 0) value |= 0x0200;
        if ((index & 0x80) > 0) value |= 0x0002;
        return value;
    }

    public static int get3880(int index) {
        int value = 0;
        if ((index & 0x01) > 0) value |= 0x0100;
        if ((index & 0x02) > 0) value |= 0x0001;
        if ((index & 0x10) > 0) value |= 0x0200;
        if ((index & 0x20) > 0) value |= 0x0002;
        return value;
    }

    /* This is the default EGA color palette, taken from the EGA / VGA Programmer's Reference Manual p202.
     * I used the colors as displayed by DOSBOX on my modern screen. */
    public static int convertColorIndex(int index) {
        switch(index & 0x0f) {
            case 0 ->  { return 0xff000000; } // black
            case 1 ->  { return 0xff0000aa; } // blue
            case 2 ->  { return 0xff00aa00; } // green
            case 3 ->  { return 0xff00aaaa; } // cyan
            case 4 ->  { return 0xffaa0000; } // red
            case 5 ->  { return 0xffaa00aa; } // magenta
            case 6 ->  { return 0xffaa5500; } // brown
            case 7 ->  { return 0xffaaaaaa; } // white
            case 8 ->  { return 0xff555555; } // dark gray
            case 9 ->  { return 0xff5555ff; } // light blue
            case 10 -> { return 0xff55ff55; } // light green
            case 11 -> { return 0xff55ffff; } // light cyan
            case 12 -> { return 0xffff5555; } // light red
            case 13 -> { return 0xffff55ff; } // light magenta
            case 14 -> { return 0xffffff55; } // yellow
            case 15 -> { return 0xffffffff; } // bright white
            default -> {
                System.out.println("Default: " + index);
                return 0xff333333;
            }
        }
    }
}