package com.hitchhikerprod.dragonjars.data;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public class StringDecoder {
    private final Chunk chunk;

    private int pointer;
    // private List<Integer> rawBytes;
    private Deque<Boolean> bitQueue;
    private List<Integer> decodedChars;

    public StringDecoder(Chunk chunk) {
        this.chunk = chunk;
    }

    public int getPointer() {
        return this.pointer;
    }

    public List<Integer> getDecodedChars() {
        return this.decodedChars;
    }

    public String getDecodedString() {
        StringBuilder builder = new StringBuilder();
        for (int i : decodedChars) {
            final int codePoint = i & 0x7f;
            if (codePoint == 0x0a || codePoint == 0x0d) {
                builder.append("\\n");
            } else {
                builder.appendCodePoint(codePoint);
            }
        }
        return builder.toString();
    }

    public void decodeString(int pointer) {
        this.pointer = pointer;
        bitQueue = new LinkedList<>();
        // rawBytes = new ArrayList<>();
        decodedChars = new ArrayList<>();

        boolean capitalize = false;
        while (true) {
            int value = shiftBits(5);
            if (value == 0x00) {
                return;
            }
            if (value == 0x1e) {
                capitalize = true;
                continue;
            }
            if (value > 0x1e) {
                value = shiftBits(6) + 0x1e;
            }
            value &= 0xff;
            int ascii = lookUp(value);
            if (capitalize && ascii >= 0xe1 && ascii <= 0xfa) {
                ascii &= 0xdf;
            }
            capitalize = false;
            decodedChars.add(ascii);

            // other, much more complicated logic in decode_string()
        }
    }

    private void unpackByte() {
        final int b = chunk.getByte(pointer);
        pointer++;
        // rawBytes.add(b);
        int x = 0x80;
        while (x > 0) {
            bitQueue.addLast((b & x) > 0);
            x = x >> 1;
        }
    }

    private int shiftBits(int len) {
        if (len < 1) {
            throw new IllegalArgumentException();
        }
        while (bitQueue.size() < len) {
            unpackByte();
        }
        int i = 0x0;
        int x = 0x1 << (len - 1);
        while (x > 0) {
            if (bitQueue.removeFirst()) i = i | x;
            x = x >> 1;
        }
        return i;
    }

    private int lookUp(int index) {
        return ((int)LUT_1CCA[index - 1]) & 0xff;
    }

    private static final byte[] LUT_1CCA = new byte[] {
            (byte) 0xa0, (byte) 0xe1, (byte) 0xe2, (byte) 0xe3,
            (byte) 0xe4, (byte) 0xe5, (byte) 0xe6, (byte) 0xe7,
            (byte) 0xe8, (byte) 0xe9, (byte) 0xeb, (byte) 0xec,
            (byte) 0xed, (byte) 0xee, (byte) 0xef, (byte) 0xf0,
            (byte) 0xf2, (byte) 0xf3, (byte) 0xf4, (byte) 0xf5,
            (byte) 0xf6, (byte) 0xf7, (byte) 0xf9, (byte) 0xae,
            (byte) 0xa2, (byte) 0xa7, (byte) 0xac, (byte) 0xa1,
            (byte) 0x8d, (byte) 0xea, (byte) 0xf1, (byte) 0xf8,
            (byte) 0xfa, (byte) 0xb0, (byte) 0xb1, (byte) 0xb2,
            (byte) 0xb3, (byte) 0xb4, (byte) 0xb5, (byte) 0xb6,
            (byte) 0xb7, (byte) 0xb8, (byte) 0xb9, (byte) 0x30,
            (byte) 0x31, (byte) 0x32, (byte) 0x33, (byte) 0x34,
            (byte) 0x35, (byte) 0x36, (byte) 0x37, (byte) 0x38,
            (byte) 0x39, (byte) 0x41, (byte) 0x42, (byte) 0x43,
            (byte) 0x44, (byte) 0x45, (byte) 0x46, (byte) 0x47,
            (byte) 0x48, (byte) 0x49, (byte) 0x4a, (byte) 0x4b,
            (byte) 0x4c, (byte) 0x4d, (byte) 0x4e, (byte) 0x4f,
            (byte) 0x50, (byte) 0x51, (byte) 0x52, (byte) 0x53,
            (byte) 0x54, (byte) 0x55, (byte) 0x56, (byte) 0x57,
            (byte) 0x58, (byte) 0x59, (byte) 0x5a, (byte) 0xa8,
            (byte) 0xa9, (byte) 0xaf, (byte) 0xdc, (byte) 0xa3,
            (byte) 0xaa, (byte) 0xbf, (byte) 0xbc, (byte) 0xbe,
            (byte) 0xba, (byte) 0xbb, (byte) 0xad, (byte) 0xa5
    };
}
