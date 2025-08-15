package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LoadAXTest {
    private static final Chunk PROGRAM = new Chunk(List.of(
            (byte)0x5a, // padding
            (byte)0x0c, // LoadAX
            (byte)0x07, // address (lo)
            (byte)0x00, // address (hi)
            (byte)0x5a  // exit
    ));
    private static final Chunk DATA = new Chunk(List.of(
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0xaa,
            (byte)0xbb, (byte)0x00, (byte)0x00, (byte)0x00
    ));

    @Test
    public void wide() {
        final Interpreter i = new Interpreter(null, List.of(PROGRAM, DATA));
        i.setWidth(true);
        i.setAX(0x0000ffff);
        i.setDS(0x01);
        i.start(0, 1);

        assertEquals(0x0000bbaa, i.getAX());
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void narrow() {
        final Interpreter i = new Interpreter(null, List.of(PROGRAM, DATA));
        i.setWidth(true);
        i.setAX(0x0000ffff);
        i.setDS(0x1);
        i.setWidth(false);
        i.start(0, 1);

        assertEquals(0x000000aa, i.getAX());
        assertEquals(2, i.instructionsExecuted());
    }
}
