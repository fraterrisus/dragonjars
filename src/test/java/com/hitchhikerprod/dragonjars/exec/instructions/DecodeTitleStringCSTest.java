package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.ExecutableImporter;
import com.hitchhikerprod.dragonjars.data.StringDecoder;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class DecodeTitleStringCSTest {
    @Test
    public void decodeTitleStringCS() {
        final Chunk exec = new ExecutableImporter().getChunk();

        final Chunk program = new Chunk(List.of(
                (byte) 0x7b, // DecodeTitleStringCS
                (byte) 0xf2, (byte) 0x9d, (byte) 0x33, (byte) 0x46,
                (byte) 0x0c, (byte) 0x15, (byte) 0xc0,
                (byte) 0x5a  // Exit
        ));

        final StringDecoder decoder = new StringDecoder(exec);
        final Interpreter i = new Interpreter(null, List.of(Chunk.EMPTY, program, exec));
        final Interpreter j = spy(i);
        doReturn(decoder).when(j).stringDecoder();
        doNothing().when(j).setTitleString(anyList());
        j.start(0x1, 0x0);

        final List<Integer> expectedChars = decoder.getDecodedChars();
        verify(j).setTitleString(ArgumentMatchers.same(expectedChars));
        assertEquals(2, j.instructionsExecuted());
    }
}