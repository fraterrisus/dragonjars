package com.hitchhikerprod.dragonjars.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExecutableLayoutTest {
    public boolean noProps() {
        final String basePath = LocalProperties.getBasePath();
        return Objects.isNull(basePath) || basePath.isBlank();
    }

    @Test
    @DisabledIf("noProps")
    public void myBinary() {
        final ExecutableImporter importer = new ExecutableImporter();
        importer.getChunk(LocalProperties.getBasePath() + "/DRAGON.COM");

        final ExecutableLayout instance = ExecutableLayout.getInstance();
        assertEquals(0x6500, instance.getLittleManTextureAddress());
        assertEquals(0x2544, instance.getHudRegionLutAddress());
        assertEquals(0x6428, instance.getCornerLutAddress());
        assertEquals(0x67c0, instance.getRomImageLutAddress());
        assertEquals(0xb8a2, instance.getFontAddress());
        assertEquals(0x1a61, instance.getStatusBitmaskLutAddress());
        assertEquals(0x1bca, instance.getStringDecoderLutAddress());
        assertEquals(0x5edc, instance.getTitleMusicAddress());
    }

    @Test
    @DisabledIf("noProps")
    public void steamBinary() {
        final ExecutableImporter importer = new ExecutableImporter();
        importer.getChunk(LocalProperties.getBasePath() + "/steam/DRAGON/DRAGON.COM");

        final ExecutableLayout instance = ExecutableLayout.getInstance();
        assertEquals(0x6720, instance.getLittleManTextureAddress());
        assertEquals(0x2694, instance.getHudRegionLutAddress());
        assertEquals(0x6648, instance.getCornerLutAddress());
        assertEquals(0x69e0, instance.getRomImageLutAddress());
        assertEquals(0xbe52, instance.getFontAddress());
        assertEquals(0x1ac1, instance.getStatusBitmaskLutAddress());
        assertEquals(0x1c2a, instance.getStringDecoderLutAddress());
        assertEquals(0x60fc, instance.getTitleMusicAddress());
    }
}
