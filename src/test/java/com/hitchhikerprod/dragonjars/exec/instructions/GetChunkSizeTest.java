package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GetChunkSizeTest {
    @Test
    public void chunkSize() {
        final Chunk program = new Chunk(List.of(
                (byte)0x9e, // GetChunkSize
                (byte)0x5a  // Exit
        ));

        final Chunk data = new Chunk(new byte[0x135]);

        final Interpreter i = new Interpreter(null, List.of(program, data), 0, 0);
        i.loadChunk(0x01);
        i.setWidth(true);
        i.setAL(0x0001);
        i.start();

        assertEquals(0x0135, i.getAX(true));
        assertEquals(2, i.instructionsExecuted());
    }

}