package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Frob;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LoadAXIndirectTest {
    private static final Chunk PROGRAM = new Chunk(List.of(
            (byte)0x5a, // padding
            (byte)0x0e, // LoadAXIndirect
            (byte)0x31, // heap address
            (byte)0x5a  // exit
    ));
    private static final Chunk DATA = new Chunk(List.of(
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0xaa,
            (byte)0xbb, (byte)0x00, (byte)0x00, (byte)0x00
    ));

    @Test
    public void wide() {
        final Interpreter i = new Interpreter(null, List.of(PROGRAM, DATA, Chunk.EMPTY)).init();
        i.setWidth(true);
        i.setAX(0xffff);
        i.heap(0x31).write(0x0005, 2);
        i.setBL(0x0002);
        i.setDS(i.getSegmentForChunk(0x01, Frob.IN_USE));
        i.start(0, 1);

        assertEquals(0x0000bbaa, i.getAX());
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void narrow() {
        final Interpreter i = new Interpreter(null, List.of(PROGRAM, DATA, Chunk.EMPTY)).init();
        i.setWidth(true);
        i.setAX(0xffff);
        i.heap(0x31).write(0x0005, 2);
        i.setBL(0x0002);
        i.setDS(i.getSegmentForChunk(0x1, Frob.IN_USE));
        i.setWidth(false);
        i.start(0, 1);

        assertEquals(0x000000aa, i.getAX());
        assertEquals(2, i.instructionsExecuted());
    }
}
