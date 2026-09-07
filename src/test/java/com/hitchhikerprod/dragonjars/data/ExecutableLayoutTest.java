package com.hitchhikerprod.dragonjars.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExecutableLayoutTest {
    @Test
    public void myBinary() {
        final ExecutableImporter importer = new ExecutableImporter();
        final Chunk executable = importer.getChunk("/home/bcordes/pc-games/dragonwars/DRAGON.COM");
        ExecutableLayout.detect(executable);
        final ExecutableLayout instance = ExecutableLayout.getInstance();
//        System.out.println(instance);
        assertEquals(0x6500, instance.getLittleManTextureAddress());
        assertEquals(0x2544, instance.getHudRegionLutAddress());
        assertEquals(0x6428, instance.getCornerLutAddress());
        assertEquals(0x67c0, instance.getRomImageLutAddress());
        assertEquals(0xb8a2, instance.getFontAddress());
    }

    @Test
    public void steamBinary() {
        final ExecutableImporter importer = new ExecutableImporter();
        final Chunk executable = importer.getChunk("/home/bcordes/pc-games/dragonwars/steam/DRAGON/DRAGON.COM");
        ExecutableLayout.detect(executable);
        final ExecutableLayout instance = ExecutableLayout.getInstance();
//        System.out.println(instance);
        assertEquals(0x6720, instance.getLittleManTextureAddress());
        assertEquals(0x2694, instance.getHudRegionLutAddress());
        assertEquals(0x6648, instance.getCornerLutAddress());
        assertEquals(0x69e0, instance.getRomImageLutAddress());
        assertEquals(0xbe52, instance.getFontAddress());
    }
}