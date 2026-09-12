package com.hitchhikerprod.dragonjars.data;

import com.hitchhikerprod.dragonjars.exec.instructions.DecodeStringFrom;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;

import java.util.ArrayList;
import java.util.List;

public class ChunkStringBinding extends StringBinding {
    private final List<ObjectBinding<Byte>> byteBindings = new ArrayList<>();
    private final StringDecoder decoder;

    public ChunkStringBinding(Chunk chunk, StringDecoder decoder, int offset, int len) {
        super();
        this.decoder = decoder;
        for (int i = 0; i < len; i++) {
            final ObjectBinding<Byte> binding = chunk.watchByte(offset + i);
            byteBindings.add(binding);
            super.bind(binding);
        }
    }

    @Override
    protected String computeValue() {
        final List<Byte> tempData = new ArrayList<>();
        for (var binding : byteBindings) tempData.add(binding.get());
        final Chunk fakeChunk = new Chunk(tempData);
        decoder.decodeString(fakeChunk, 0);
        return StringDecoder.decodeString(DecodeStringFrom.pluralize(decoder.getDecodedChars(), true));
    }
}
