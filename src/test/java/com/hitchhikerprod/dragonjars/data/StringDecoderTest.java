package com.hitchhikerprod.dragonjars.data;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StringDecoderTest {
    @Test
    public void decodeString() {
        final Chunk exec = new ExecutableImporter().getChunk();

        final Chunk data = new Chunk(List.of(
                (byte) 0xf2, (byte) 0x9d, (byte) 0x33, (byte) 0x46,
                (byte) 0x0c, (byte) 0x15, (byte) 0xc0, (byte) 0x00
        ));
        final StringDecoder uut = new StringDecoder(exec);

        uut.decodeString(data, 0x0);
        assertEquals("Interplay", uut.getDecodedString());
        assertEquals(List.of(0xc9, 0xee, 0xf4, 0xe5, 0xf2, 0xf0, 0xec, 0xe1, 0xf9), uut.getDecodedChars());
        assertEquals(0x07, uut.getPointer());
    }
}