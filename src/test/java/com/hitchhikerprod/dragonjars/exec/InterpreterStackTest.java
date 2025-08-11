package com.hitchhikerprod.dragonjars.exec;

import com.hitchhikerprod.dragonjars.data.Chunk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InterpreterStackTest {
    private static final Chunk EMPTY = new Chunk(List.of());

    @Test
    public void roundTrip() {
        final Interpreter i = new Interpreter(null, List.of(EMPTY), 0, 0);
        i.push(0xffff);
        final int value = i.pop();
        assertEquals(0xff, value);
    }
}
