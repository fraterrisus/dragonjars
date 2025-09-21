package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

import java.util.List;

/** Copies automap data, a 0x700-byte buffer at [cs:d1b0], between a map data segment (at address AX) and the code
  * segment. If BL & 0x80 == 0 we read the buffer and write the map data; otherwise, the other way around. */
public class CopyAutomapBuffer implements Instruction {
    public static final int BUFFER_SIZE = 0x700;

    @Override
    public Address exec(Interpreter i) {
        final boolean toSegment = (i.getBL() & 0x80) == 0;
        final Address memAddress = new Address(i.getDS(), i.getAX(true));
        if (toSegment) {
            i.memory().writeList(memAddress, i.memory().automapChunk().getBytes(0, BUFFER_SIZE));
        } else {
            i.memory().automapChunk().setBytes(0, i.memory().readList(memAddress, BUFFER_SIZE));
        }
        return i.getIP().incr(OPCODE);
    }
}