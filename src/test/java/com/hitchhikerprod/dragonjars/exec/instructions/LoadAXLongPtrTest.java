package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Frob;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LoadAXLongPtrTest {
    @Test
    public void wide() {
        final Chunk program = new Chunk(List.of(
                (byte)0x00, // SetWide
                (byte)0x0f, // LoadAXLongPtr
                (byte)Heap.LONGPTR_ADR, // heap index
                (byte)0x1e  // Exit
        ));
        final Chunk data = new Chunk(List.of(
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0xaa, (byte)0xbb, (byte)0xcc, (byte)0xdd,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
        ));
        final Interpreter i = new Interpreter(null, List.of(program, data, Chunk.EMPTY)).init();
        final int dataSegment = i.getSegmentForChunk(0x01, Frob.IN_USE);
        i.setAH(0xff);
        i.setAL(0xff);
        Heap.get(Heap.LONGPTR_ADR).write(0x0008, 2); // segment offset lo
        Heap.get(Heap.LONGPTR_SEG).write(dataSegment, 1); // segment ID

        i.start(0, 0);

        assertEquals(0x0000bbaa, i.getAX());
        assertEquals(3, i.instructionsExecuted());
        assertEquals(program.getSize() - 1, i.getIP().offset());
    }

    @Test
    public void narrow() {
        final Chunk program = new Chunk(List.of(
                (byte)0x01, // SetNarrow
                (byte)0x0f, // LoadAXLongPtr
                (byte)Heap.LONGPTR_ADR, // heap index
                (byte)0x1e  // Exit
        ));
        final Chunk data = new Chunk(List.of(
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0xaa, (byte)0xbb, (byte)0xcc, (byte)0xdd,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
        ));
        final Interpreter i = new Interpreter(null, List.of(program, data, Chunk.EMPTY)).init();
        final int dataSegment = i.getSegmentForChunk(0x01, Frob.IN_USE);
        i.setAH(0xff);
        i.setAL(0xff);
        Heap.get(Heap.LONGPTR_ADR).write(0x0008, 2); // segment offset lo
        Heap.get(Heap.LONGPTR_SEG).write(dataSegment, 1); // segment ID

        i.start(0, 0);

        assertEquals(0x000000aa, i.getAX());
        assertEquals(3, i.instructionsExecuted());
        assertEquals(program.getSize() - 1, i.getIP().offset());
    }}
