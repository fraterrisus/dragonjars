package com.hitchhikerprod.dragonjars.exec;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InterpreterStackTest {
    @Test
    public void roundTrip() {
        final Interpreter i = new Interpreter(List.of(), 0, 0);
        i.push(0xffff);
        final int value = i.pop();
        assertEquals(0xff, value);
    }
}
