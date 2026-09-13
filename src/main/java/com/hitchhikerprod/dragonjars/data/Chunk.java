package com.hitchhikerprod.dragonjars.data;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.stream.IntStream;

public class Chunk {
    public static final Chunk EMPTY = new Chunk(List.of());

    final ObservableList<Byte> raw;

    public Chunk(List<Byte> raw) {
        this.raw = FXCollections.observableArrayList(raw);
    }

    public Chunk(byte[] rawBytes) {
        this(IntStream.range(0, rawBytes.length)
                .mapToObj(i -> rawBytes[i])
                .toList());
    }

    public Chunk(Chunk that) {
        this.raw = FXCollections.observableArrayList(that.raw);
    }

    public byte getByte(int i) {
        return this.raw.get(i);
    }

    public List<Byte> getBytes(int offset, int length) {
        int end = offset + length;
        if (end > getSize()) { end = getSize(); }
        if (offset > end) { return List.of(); }
        return this.raw.subList(offset, end);
    }

    public int getSize() {
        return this.raw.size();
    }

    public int getUnsignedByte(int i) {
        return this.raw.get(i) & 0xff;
    }

    public int getWord(int i) {
        int b0 = getUnsignedByte(i);
        int b1 = getUnsignedByte(i + 1);
        return (b1 << 8) | b0;
    }

    public int getQuadWord(int i) {
        int b0 = getUnsignedByte(i);
        int b1 = getUnsignedByte(i + 1);
        int b2 = getUnsignedByte(i + 2);
        int b3 = getUnsignedByte(i + 3);
        return (b3 << 24) | (b2 << 16) | (b1 << 8) | b0;
    }

    public int read(int offset, int num) {
        if (num > 4) throw new IllegalArgumentException("Can't read more bytes than fit in an int (4)");
        int value = 0;
        for (int i = num - 1; i >= 0; i--) {
            value = value << 8;
            value = value | getUnsignedByte(i + offset);
        }
        return value;
    }

    public IntegerBinding watch(int offset, int len) {
        return new ChunkBinding(offset, len);
    }

    public int search(List<Byte> bytes) {
        if (Objects.isNull(bytes)) throw new IllegalArgumentException("Null input list");
        if (bytes.isEmpty()) throw new IllegalArgumentException("Empty input list");

        final ListIterator<Byte> haystack = raw.listIterator();
        while (haystack.hasNext()) {
            final Byte nextByte = haystack.next();

            if (nextByte.equals(bytes.getFirst())) {
                int marker = haystack.previousIndex();
                final ListIterator<Byte> needle = bytes.listIterator();
                needle.next();

                boolean match = true;
                while (needle.hasNext()) {
                    if (!haystack.hasNext()) {
                        match = false;
                        break;
                    }
                    final Byte haystackByte = haystack.next();
                    final Byte needleByte = needle.next();
                    if (!haystackByte.equals(needleByte)) {
                        while (haystack.previousIndex() > marker) haystack.previous();
                        match = false;
                    }
                }

                if (match) {
                    return marker;
                }
            }
        }
        return -1;
    }

    public void display() {
        display(0, raw.size());
    }

    public void display(int start) {
        display(start, raw.size());
    }

    public void display(int start, int end) {
        int counter = start & 0xfff0;
        if (raw.size() < end) end = raw.size();
        while (counter < end) {
            final byte b = raw.get(counter);
            if (counter % 16 == 0) {
                System.out.printf("\n%08x", counter);
            }
            if (counter % 4 == 0) {
                System.out.print(" ");
            }
            if (counter < start) {
                System.out.print(" --");
            } else {
                System.out.printf(" %02x", b & 0xff);
            }
            counter++;
        }
        System.out.println();
    }

    class ChunkBinding extends IntegerBinding {
        private final List<ObjectBinding<Byte>> byteBindings = new ArrayList<>();

        public ChunkBinding(int offset, int len) {
            super();
            for (int i = 0; i < len; i++) {
                final ObjectBinding<Byte> binding = Bindings.valueAt(raw, offset + i);
                byteBindings.add(binding);
                super.bind(binding);
            }
        }

        @Override
        protected int computeValue() {
            int value = 0;
            int offset = 0;
            for (ObjectBinding<Byte> binding : byteBindings) {
                value = value | ((binding.getValue() & 0xff) << offset);
                offset += 8;
            }
            return value;
        }
    }
}
