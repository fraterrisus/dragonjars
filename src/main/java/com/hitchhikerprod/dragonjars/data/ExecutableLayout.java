package com.hitchhikerprod.dragonjars.data;

import java.util.ArrayList;
import java.util.List;

public class ExecutableLayout {
    private static final ExecutableLayout INSTANCE = new ExecutableLayout();

    private static final List<Byte> LITTLE_MAN_BYTES = List.of((byte)0x0d, (byte)0x18, (byte)0xfe, (byte)0xf8);
    private static final List<Byte> HUD_REGION_LUT_BYTES = List.of((byte)0x00, (byte)0xb8, (byte)0x28, (byte)0xc0);
    private static final List<Byte> CORNER_0_BYTES = List.of(
            (byte)0x66, (byte)0x66, (byte)0x66, (byte)0x66, (byte)0x60, (byte)0xF0, (byte)0x87, (byte)0x71);
    private static final List<Byte> ROM_IMAGE_0_BYTES = List.of(
            (byte)0xA0, (byte)0x10, (byte)0x00, (byte)0xB8, (byte)0x11, (byte)0x10, (byte)0x01, (byte)0x99,
            (byte)0x90, (byte)0x99, (byte)0x99, (byte)0x19, (byte)0x99, (byte)0x99, (byte)0x99, (byte)0x99);
    private static final List<Byte> FONT_BYTES = List.of(
            (byte)0xff, (byte)0xff, (byte)0xc0, (byte)0xc0, (byte)0xcf, (byte)0xcf, (byte)0xcc, (byte)0xcc);

    private int littleManTextureAddress;
    private int hudRegionLutAddress;
    private int cornerLutAddress;
    private int romImageLutAddress;
    private int fontAddress;
    private int stringDecoderLutAddress;
    private int titleMusicAddress;
    private int titleDurationLutAddress;
    private int titleFrequencyLutAddress;

    private ExecutableLayout() {}

    public static ExecutableLayout getInstance() {
        return INSTANCE;
    }

    public static void detect(Chunk executable) {
        INSTANCE.littleManTextureAddress = executable.search(LITTLE_MAN_BYTES);
        INSTANCE.hudRegionLutAddress = executable.search(HUD_REGION_LUT_BYTES);

        int corner0Address = executable.search(CORNER_0_BYTES);
        final List<Byte> cornerLutAddress = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            cornerLutAddress.add((byte)(corner0Address & 0x000000ff));
            corner0Address = corner0Address >> 8;
        }
        INSTANCE.cornerLutAddress = executable.search(cornerLutAddress);

        int romImage0Address = executable.search(ROM_IMAGE_0_BYTES);
        romImage0Address = romImage0Address + 0x100;
        final List<Byte> romImageLutAddress = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            romImageLutAddress.add((byte)(romImage0Address & 0x000000ff));
            romImage0Address = romImage0Address >> 8;
        }
        INSTANCE.romImageLutAddress = executable.search(romImageLutAddress);

        INSTANCE.fontAddress = executable.search(FONT_BYTES);
    }

    public int getLittleManTextureAddress() {
        return littleManTextureAddress;
    }

    public int getHudRegionLutAddress() {
        return hudRegionLutAddress;
    }

    public int getCornerLutAddress() {
        return cornerLutAddress;
    }

    public int getRomImageLutAddress() {
        return romImageLutAddress;
    }

    public int getFontAddress() {
        return fontAddress;
    }

    public int getStringDecoderLutAddress() {
        return stringDecoderLutAddress;
    }

    public int getTitleMusicAddress() {
        return titleMusicAddress;
    }

    public int getTitleDurationLutAddress() {
        return titleDurationLutAddress;
    }

    public int getTitleFrequencyLutAddress() {
        return titleFrequencyLutAddress;
    }

    @Override
    public String toString() {
        return "ExecutableLayout[" +
                String.format("littleManTextureAddress=0x%x,", littleManTextureAddress) +
                String.format("hudRegionLutAddress=0x%x,", hudRegionLutAddress) +
                String.format("cornerLutAddress=0x%x,", cornerLutAddress) +
                String.format("romImageLutAddress=0x%x,", romImageLutAddress) +
                String.format("fontAddress=0x%x,", fontAddress) +
                String.format("stringDecoderLutAddress=0x%x,", stringDecoderLutAddress) +
                String.format("titleMusicAddress=0x%x,", titleMusicAddress) +
                String.format("titleDurationLutAddress=0x%x,", titleDurationLutAddress) +
                String.format("titleFrequencyLutAddress=0x%x", titleFrequencyLutAddress) +
                "]";
    }
}