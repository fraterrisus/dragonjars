package com.hitchhikerprod.dragonjars.data;

public class TextureDecoder {
    private Chunk codeChunk;
    private int[] buffer;

    public TextureDecoder(Chunk codeChunk) {
        this.codeChunk = codeChunk;
        this.buffer = new int[1];
    }

    public void setBuffer(int[] buffer) {
        this.buffer = buffer;
    }

    public void entrypoint0ca7(Chunk chunk, int pointer, int offset, int x0_352e, int y0_3532, int unk_100e) { // 0x0ca7
        // chunk: stand-in for [1011/seg]
        // pointer: stand-in for [100f/adr]
        // offset: stand-in for bx
        final int value = chunk.getUnsignedByte(pointer + offset);
        if (value == 0) return;
        entrypoint0cb8(chunk, pointer + value, x0_352e, y0_3532, unk_100e);
    }

    public void entrypoint0cb8(Chunk chunk, int pointer, int x0_352e, int y0_3532, int unk_100e) { // 0cb8
        // chunk: stand-in for [1011/seg]
        // pointer: stand-in for [100f/adr]
        int width_1008 = chunk.getUnsignedByte(pointer++);
        int height_100d = chunk.getUnsignedByte(pointer++);

        // 0x0ccb
        int offsetX = chunk.getByte(pointer++); // sign extended
        if ((unk_100e & 0x80) > 0) offsetX = offsetX * -1;
        int x0 = x0_352e + (offsetX & 0xffff); // enough of this four-byte int nonsense
        if ((width_1008 & 0x80) > 0 && (unk_100e & 0x80) > 0) x0--;

        // 0x0cea
        width_1008 = width_1008 & 0x7f;
        int offsetY = chunk.getByte(pointer++); // sign extended
        if ((unk_100e & 0x40) > 0) offsetY = offsetY * -1;
        final int y0 = y0_3532 + offsetY;

        // 0x0cfe; index divided by two because we aren't using a word-based lookup table
        int callIndex = 0;
        if ((x0 & 0x0001) > 0) callIndex = callIndex | 0x02;
        if ((x0 & 0x8000) > 0) callIndex = callIndex | 0x04;
        if ((unk_100e & 0x0080) > 0) callIndex = callIndex | 0x08;

        final int factor_1013 = 0x50; // the automap uses 0x90 but everything else uses 0x50
        final int factorCopy_1015 = ((unk_100e & 0x40) > 0) ? -1 * factor_1013 : factor_1013;

        switch (callIndex) {
            case 0x0 -> decode_0d48(chunk, pointer, width_1008, height_100d, x0, y0, factor_1013, factorCopy_1015);
            case 0x2 -> decode_0dab();
            case 0x4 -> decode_0e2d();
            case 0x6 -> decode_0e85();
            case 0x8 -> decode_0efd();
            case 0xa -> decode_0f72();
            case 0xc, 0xe -> {}
            default -> throw new RuntimeException("Unrecognized call index " + callIndex + "; this shouldn't be possible");
        }
    }

    private void decode_0d48(Chunk chunk, final int pointer, int width, int height, int x0t2, int y0,
                             int factor, int factorCopy) {
        final boolean x0Sign = (x0t2 & 0x8000) > 0;
        final int x0 = (x0t2 >> 1) | (x0Sign ? 0x8000 : 0x0000);

        int width_100a = width; // was 1008
        int temp = width + x0 - factor;
        if (temp > 0) {
            width_100a -= temp;
            if (width_100a <= 0) return;
        }

        int y = height;

        // 0x0d68
        int dx = x0 + (y0 * factor); // via 0xac92 multiplication table
        int si = pointer;
        while (y > 0) {
            int x = width_100a;
            int di = dx;
            int save_si = si;
            while (x > 0) {
                final int bx = chunk.getUnsignedByte(si++);
                int pixel = buffer[di];
                final int byteAnd = codeChunk.getByte(0xada2 + bx);
                final int byteOr = codeChunk.getByte(0xaea2 + bx);
                pixel = pixel & byteAnd;
                pixel = pixel | byteOr;
                buffer[di++] = pixel;
                x--;
            }
            si = save_si + width;
            dx += factorCopy;
            y--;
        }
    }

    private void decode_0dab() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    private void decode_0e2d() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    private void decode_0e85() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    private void decode_0efd() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    private void decode_0f72() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
