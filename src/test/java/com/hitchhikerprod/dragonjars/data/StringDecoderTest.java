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

    @Test
    public void longTest() {
        final List<Byte> b = List.of(
                (byte) 0xF3, (byte) 0x44, (byte) 0xC5, (byte) 0x21, (byte) 0xC1, (byte) 0x38, (byte) 0x95, (byte) 0x1B,
                (byte) 0x86, (byte) 0x50, (byte) 0x54, (byte) 0x55, (byte) 0x39, (byte) 0x05, (byte) 0x14, (byte) 0x14,
                (byte) 0x4A, (byte) 0x19, (byte) 0xA5, (byte) 0x52, (byte) 0x0C, (byte) 0x05, (byte) 0x29, (byte) 0x60,
                (byte) 0x3E, (byte) 0x9A, (byte) 0x4D, (byte) 0x70, (byte) 0x94, (byte) 0xCD, (byte) 0x13, (byte) 0x8A,
                (byte) 0x19, (byte) 0xA4, (byte) 0xC1, (byte) 0x84, (byte) 0x54, (byte) 0x43, (byte) 0x05, (byte) 0xE7,
                (byte) 0x0B, (byte) 0x14, (byte) 0x73, (byte) 0x04, (byte) 0x66, (byte) 0x3B, (byte) 0xE2, (byte) 0x60,
                (byte) 0xDD, (byte) 0xF4, (byte) 0x0B, (byte) 0x45, (byte) 0x70, (byte) 0xC0, (byte) 0x52, (byte) 0x96,
                (byte) 0x03, (byte) 0xE2, (byte) 0xBC, (byte) 0x37, (byte) 0x7D, (byte) 0x02, (byte) 0x47, (byte) 0xBA,
                (byte) 0x46, (byte) 0x74, (byte) 0xFF, (byte) 0xC7, (byte) 0x7A, (byte) 0x00
        );

        final Chunk data = new Chunk(b);
        final Chunk exec = new ExecutableImporter().getChunk();
        final StringDecoder uut = new StringDecoder(exec);
        uut.decodeString(data, 0x0);

        final StringEncoder enc = new StringEncoder();
        final List<Byte> result = enc.encodeString(uut.getDecodedString());
        assertEquals(b, result);
    }

    @Test
    public void encode() {
        final String text1 = "You wake up in a small, cold cell. The lock in only accessable on the outside of the door.";
        final String text2 = "You wake up in a small, cold cell. The lock is only accessible on the outside of the door.";
        final StringEncoder enc = new StringEncoder();
        final List<Byte> result1 = enc.encodeString(text1);
        final List<Byte> result2 = enc.encodeString(text2);
        for (int i = 0; i < result1.size(); i++) {
            if (!result1.get(i).equals(result2.get(i))) {
                System.out.format("[0x%03x] old:%02x new:%02x\n", i, result1.get(i), result2.get(i));
            }
        }
    }
}