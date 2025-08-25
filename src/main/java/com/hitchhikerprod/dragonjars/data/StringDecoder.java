package com.hitchhikerprod.dragonjars.data;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public class StringDecoder {
    private final Chunk executable;

    private List<Byte> lut;
    private Chunk chunk;
    private int pointer;
    private Deque<Boolean> bitQueue;
    private List<Integer> decodedChars;

    public StringDecoder(Chunk executable) {
        this.executable = executable;
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

    public void decodeString(Chunk chunk, int pointer) {
        this.chunk = chunk;
        this.pointer = pointer;
        this.lut = executable.getBytes(0x1bca, 92);
        bitQueue = new LinkedList<>();
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
        return lut.get(index - 1) & 0xff;
    }
}
