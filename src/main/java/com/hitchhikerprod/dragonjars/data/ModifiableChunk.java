package com.hitchhikerprod.dragonjars.data;

import java.util.List;

public class ModifiableChunk extends Chunk {
    public ModifiableChunk(List<Byte> raw) {
        super(raw);
    }

    public ModifiableChunk(byte[] rawBytes) {
        super(rawBytes);
    }

    public ModifiableChunk(Chunk chunk) {
        super(chunk);
    }

    public void write(int index, int length, int value) {
        int v = value;
        for (int i = 0; i < length; i++) {
            this.raw.set(index + i, (byte) (v & 0xff));
            v = v >> 8;
        }
    }
}