package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArrayMaxTest {
    @Test
    public void test() {
        final Chunk program = new Chunk(List.of(
                (byte)0x51, // ArrayMax
                (byte)0x10,  //  address (lo)
                (byte)0x00, //   address (hi)
                (byte)0x5a  // Exit
        ));

        final byte[] dataBytes = new byte[0x200];
        dataBytes[0x110] = (byte)0x10;
        dataBytes[0x111] = (byte)0x9c;
        dataBytes[0x112] = (byte)0x4f;
        dataBytes[0x113] = (byte)0xf2;
        dataBytes[0x114] = (byte)0x08;
        dataBytes[0x115] = (byte)0x31;
        final Chunk data = new Chunk(dataBytes);

        final Interpreter i = new Interpreter(null, List.of(program, data));
        i.setWidth(true);
        i.setDS(0x01);
        i.setBX(0x0106);
        i.start(0, 0);

        assertEquals(0x03, i.getBL());
        assertEquals(0xf2, i.getAL());
        assertEquals(2, i.instructionsExecuted());
    }

}