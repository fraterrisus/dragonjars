package com.hitchhikerprod.dragonjars.data;

public class DataString {
    private final String value;

    public DataString(Chunk chunk, int offset, int length) {
        this.value = parseString(chunk.getBytes(offset, length));
    }

    public String toString() {
        return value;
    }

    private static String parseString(final Iterable<Byte> bytes) {
        final StringBuilder builder = new StringBuilder();
        for (final Byte b : bytes) {
            final boolean done = (b & 0x80) == 0x0;
            final char c = (char)(b & 0x7f);
            if (!Character.isISOControl(c)) {
                builder.append((char) (b & 0x7F));
            } else { break; }
            if (done) { break; }
        }
        return builder.toString();
    }
}
