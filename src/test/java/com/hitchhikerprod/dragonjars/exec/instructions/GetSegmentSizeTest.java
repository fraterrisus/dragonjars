package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Frob;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GetSegmentSizeTest {
    @Test
    public void chunkSize() {
        final Chunk program = new Chunk(List.of(
                (byte)0x9e, // GetSegmentSize
                (byte)0x5a  // Exit
        ));

        final Chunk data = new Chunk(new byte[0x135]);

        final Interpreter i = new Interpreter(null, List.of(program, data, Chunk.EMPTY)).init();
        i.setWidth(true);
        i.setAL(i.getSegmentForChunk(0x01, Frob.CLEAN));
        i.start(0, 0);

        assertEquals(0x0135, i.getAX(true));
        assertEquals(2, i.instructionsExecuted());
    }

}