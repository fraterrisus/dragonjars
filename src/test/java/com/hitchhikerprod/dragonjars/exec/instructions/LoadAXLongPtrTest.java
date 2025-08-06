package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
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
                (byte)0x34, // heap index
                (byte)0x5a  // Exit
        ));
        final Chunk data = new Chunk(List.of(
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0xaa, (byte)0xbb, (byte)0xcc, (byte)0xdd,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
        ));
        final Interpreter i = new Interpreter(List.of(program, data), 0, 0);
        i.setAH(0xff);
        i.setAL(0xff);
        i.setHeap(0x34, 0x08); // chunk offset lo
        i.setHeap(0x35, 0x00); // chunk offset hi
        i.setHeap(0x36, 0x01); // chunk ID

        i.start();

        assertEquals(0x0000bbaa, i.getAX());
        assertEquals(3, i.instructionsExecuted());
        assertEquals(program.getSize() - 1, i.getIP().offset());
    }

    @Test
    public void narrow() {
        final Chunk program = new Chunk(List.of(
                (byte)0x01, // SetNarrow
                (byte)0x0f, // LoadAXLongPtr
                (byte)0x34, // heap index
                (byte)0x5a  // Exit
        ));
        final Chunk data = new Chunk(List.of(
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0xaa, (byte)0xbb, (byte)0xcc, (byte)0xdd,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
        ));
        final Interpreter i = new Interpreter(List.of(program, data), 0, 0);
        i.setAH(0xff);
        i.setAL(0xff);
        i.setHeap(0x34, 0x08); // chunk offset lo
        i.setHeap(0x35, 0x00); // chunk offset hi
        i.setHeap(0x36, 0x01); // chunk ID

        i.start();

        assertEquals(0x000000aa, i.getAX());
        assertEquals(3, i.instructionsExecuted());
        assertEquals(program.getSize() - 1, i.getIP().offset());
    }}
