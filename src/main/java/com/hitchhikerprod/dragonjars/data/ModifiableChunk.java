package com.hitchhikerprod.dragonjars.data;

import java.util.List;

public class ModifiableChunk extends Chunk {
    private boolean modified = false;

    public ModifiableChunk(List<Byte> raw) {
        super(raw);
    }

    public ModifiableChunk(byte[] rawBytes) {
        super(rawBytes);
    }

    public ModifiableChunk(Chunk chunk) {
        super(chunk);
    }

    public boolean isDirty() {
        return modified;
    }

    public void clean() {
        modified = false;
    }

    public void write(int index, int length, int value) {
        int v = value;
        for (int i = 0; i < length; i++) {
            final byte newValue = (byte)(v & 0xff);
            if (this.raw.get(index + i) != newValue) {
                this.raw.set(index + i, newValue);
                modified = true;
            }
            v = v >> 8;
        }
    }

    public void setBytes(int index, List<Byte> values) {
        for (byte b : values) {
            final byte newValue = (byte)(b & 0xff);
            if (this.raw.get(index) != newValue) {
                this.raw.set(index, newValue);
                modified = true;
            }
            index++;
        }
    }
}