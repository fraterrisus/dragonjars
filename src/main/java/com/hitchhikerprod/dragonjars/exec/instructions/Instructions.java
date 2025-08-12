package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.StringDecoder;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

import java.util.ArrayList;
import java.util.List;

import static com.hitchhikerprod.dragonjars.exec.instructions.Instruction.OPCODE;

public class Instructions {
    public static final Instruction SET_WIDE = (i) -> {
        i.setWidth(true);
        return i.getIP().incr(OPCODE);
    };

    public static final Instruction SET_NARROW = (i) -> {
        i.setWidth(false);
        i.setAL(0x00); // ???????
        return i.getIP().incr(OPCODE);
    };

    public static final Instruction PUSH_CS = (i) -> {
        i.push(i.getCS());
        return i.getIP().incr(OPCODE);
    };

    public static final Instruction PUSH_DS = (i) -> {
        i.push(i.getDS());
        return i.getIP().incr(OPCODE);
    };

    public static final Instruction POP_DS = (i) -> {
        i.setDS(i.pop());
        return i.getIP().incr(OPCODE);
    };

    public static final Instruction CLEAR_CARRY = (i) -> {
        i.setCarryFlag(false);
        return i.getIP().incr(OPCODE);
    };

    public static final Instruction SET_CARRY = (i) -> {
        i.setCarryFlag(true);
        return i.getIP().incr(OPCODE);
    };

    public static final Instruction COPY_HEAP_3E_3F = (i) -> {
        i.setHeapBytes(0x3e, 1, i.getHeapBytes(0x3f, 1));
        return i.getIP().incr(OPCODE);
    };

    public static final Instruction FILL_BBOX = (i) -> {
        i.fillRectangle();
        return i.getIP().incr();
    };

    public static final Instruction NOOP = (i) -> i.getIP().incr(OPCODE);

    public static final Instruction EXIT = (i) -> null;

    public static Address compose(Interpreter i, Instruction... ins) {
        Address result = null;
        for (Instruction instruction : ins) {
            result = instruction.exec(i);
        }
        return result;
    }

    public static Address decodeTitleString(Interpreter i, Address addr) {
        final StringDecoder decoder = new StringDecoder(i.getChunk(addr.chunk()));
        decoder.decodeString(addr.offset());
        final List<Integer> chars = decoder.getDecodedChars();
        if (chars.getFirst() == 0x00) return addr;
        final int x = (16 - chars.size()) / 2 + 4;
        i.setCharCoordinates(x, 0);
        i.setInvertChar(0xff);
        i.drawString(chars);
        return new Address(addr.chunk(), decoder.getPointer());
    }

    public static Address decodeString(Interpreter i, Address addr) {
        final StringDecoder decoder = new StringDecoder(i.getChunk(addr.chunk()));
        decoder.decodeString(addr.offset());
        final List<Integer> chars = decoder.getDecodedChars();
        if (chars.getFirst() == 0x00) return addr;

        if ((i.getHeapBytes(0x08, 1) & 0x80) == 0) {
            i.setHeapBytes(0x08, 1, chars.getFirst() | 0x80);
        }

        boolean writeSingular = true;
        final List<Integer> singular = new ArrayList<>();
        boolean writePlural = true;
        final List<Integer> plural = new ArrayList<>();
        for (int ch : chars) {
            switch (ch) {
                case 0xaf -> {
                    writeSingular = true;
                    writePlural = false;
                }
                case 0xdc -> {
                    writePlural = true;
                    writeSingular = false;
                }
                default -> {
                    if (writeSingular) singular.add(ch);
                    if (writePlural) plural.add(ch);
                }
            }
        }

        if (i.getHeapBytes(0x09, 1) == 0x00) {
            i.drawString(singular);
        } else {
            i.drawString(plural);
        }

        return new Address(addr.chunk(), decoder.getPointer());
    }
}