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

    List<Byte> was = List.of(
            (byte) 0xf4, (byte) 0xd2, (byte) 0x60, (byte) 0xfa, (byte) 0x56, (byte) 0x7c, (byte) 0x4a, (byte) 0x17,
            (byte) 0x9c, (byte) 0x3e, (byte) 0x3c, (byte) 0x4c, (byte) 0x62, (byte) 0xbd, (byte) 0xa1, (byte) 0x54,
            (byte) 0x83, (byte) 0x77, (byte) 0xd2, (byte) 0x3a, (byte) 0x90, (byte) 0x4e, (byte) 0xf8, (byte) 0x86,
            (byte) 0x69, (byte) 0x30, (byte) 0x66, (byte) 0x25, (byte) 0xa9, (byte) 0xc8, (byte) 0xc7, (byte) 0x7b,
            (byte) 0xeb, (byte) 0x25, (byte) 0xe1, (byte) 0xb2, (byte) 0x98, (byte) 0xc0, (byte) 0xcc, (byte) 0x4b,
            (byte) 0x30, (byte) 0x66, (byte) 0x93, (byte) 0x07, (byte) 0xd2, (byte) 0xb3, (byte) 0xe2, (byte) 0x5f,
            (byte) 0xf1, (byte) 0xde, (byte) 0x80
    );

    List<Byte> is = List.of(
            (byte) 0xf4, (byte) 0xd2, (byte) 0x60, (byte) 0xfa, (byte) 0x56, (byte) 0x7c, (byte) 0x4a, (byte) 0x17,
            (byte) 0x9c, (byte) 0x3e, (byte) 0x3c, (byte) 0x4c, (byte) 0x62, (byte) 0xbd, (byte) 0xa1, (byte) 0x54,
            (byte) 0x83, (byte) 0x77, (byte) 0xd2, (byte) 0x32, (byte) 0x09, (byte) 0xdf, (byte) 0x10, (byte) 0xcd,
            (byte) 0x26, (byte) 0x0c, (byte) 0xc4, (byte) 0xb5, (byte) 0x39, (byte) 0x18, (byte) 0xef, (byte) 0x7d,
            (byte) 0x64, (byte) 0xbc, (byte) 0x36, (byte) 0x53, (byte) 0x18, (byte) 0x19, (byte) 0x89, (byte) 0x66,
            (byte) 0x0c, (byte) 0xd2, (byte) 0x60, (byte) 0xfa, (byte) 0x56, (byte) 0x7c, (byte) 0x4b, (byte) 0xfe,
            (byte) 0x3b, (byte) 0xd0, (byte) 0x00
    );

    // "The Sword of Freedom is your's for the taking.\n\nWho will take the Sword?\n\n"
    //  f4 d2 60 fa 56 7c 4a 17 9c 3e 3c 4c 62 bd a1 54 83 77 d2 3a 90 4e f8 86 69 30 66 25 a9 c8 c7 7b eb 25 e1 b2 98 c0 cc 4b 30 66 93 07 d2 b3 e2 5f f1 de 80
    //  f4 d2 60 fa 56 7c 4a 17 9c 3e 3c 4c 62 bd a1 54 83 77 d2 32 09 df 10 cd 26 0c c4 b5 39 18 ef 7d 64 bc 36 53 18 19 89 66 0c d2 60 fa 56 7c 4b fe 3b d0 00
    @Test
    public void encode() {
        final String text = "The Sword of Freedom is yours for the taking.\n\nWho will take the Sword?\n\n";
        final StringEncoder enc = new StringEncoder();
        final List<Byte> result = enc.encodeString(text);
        result.forEach(b -> System.out.format(" %02x", b));
        System.out.println();
    }
}