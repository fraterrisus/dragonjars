package com.hitchhikerprod.dragonjars.data;

import java.util.List;

public class ModifiableChunk extends Chunk {
    public ModifiableChunk(List<Byte> raw) {
        super(raw);
    }

    public ModifiableChunk(byte[] rawBytes) {
        super(rawBytes);
    }

    public void setByte(int index, byte value) {
        this.raw.set(index, value);
    }

    public void setByte(int index, int value) {
        this.raw.set(index, (byte) (value & 0xff));
    }

    public void setWord(int index, int value) {
        this.raw.set(index, (byte) (value & 0x00ff));
        this.raw.set(index + 1, (byte) ((value & 0xff00) >> 8));
    }
}