package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.ModifiableChunk;
import com.hitchhikerprod.dragonjars.data.StringDecoder;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import javafx.scene.input.KeyCode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

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
        i.heap(0x3e).write(i.heap(0x3f).read());
        return i.getIP().incr(OPCODE);
    };

    public static final Instruction FILL_BBOX = (i) -> {
        i.fillRectangle();
        return i.getIP().incr();
    };

    public static final Instruction NOOP = (i) -> i.getIP().incr(OPCODE);

    public static final Instruction EXIT = (i) -> {
        // System.out.println("** exit");
        return null;
    };

    public static Address compose(Interpreter i, Instruction... ins) {
        Address result = null;
        for (Instruction instruction : ins) {
            result = instruction.exec(i);
        }
        return result;
    }

    /**
     * Decodes a string from the provided pointer. The decoded string is passed to the interpreter's #setTitleString()
     * method for further handling.
     * @param i Interpreter.
     * @param addr Segment/offset pair to begin decoding.
     * @return Segment/offset pair pointing at the last decoded byte. When executing an instruction, the next opcode
     * will be one byte beyond this pointer.
     */
    public static Address decodeTitleString(Interpreter i, Address addr) {
        // (See 0x2693.) Sets the indirect function pointer to 0x26aa, which saves the decoded string at 0x273a
        // instead of printing it to the screen. Then it resets the indirect function to 0x30c1 and calls a helper
        // (0x26be) which forwards to drawMapTitle() (0x26d4) after doing a bounds check that we ignore, heh. We
        // roll that call into i.setTitleString, below.
        final StringDecoder decoder = i.stringDecoder();
        final ModifiableChunk chunk = i.memory().getSegment(addr.segment());
        decoder.decodeString(chunk, addr.offset());
        final List<Integer> chars = decoder.getDecodedChars();
        i.setTitleString(chars);
        return new Address(addr.segment(), decoder.getPointer());
    }

    public static Address decodeString(Interpreter i, Address addr) {
        final StringDecoder decoder = i.stringDecoder();
        final ModifiableChunk chunk = i.memory().getSegment(addr.segment());
        decoder.decodeString(chunk, addr.offset());
        final List<Integer> chars = decoder.getDecodedChars();
        if (chars.getFirst() == 0x00) return addr;

        if ((i.heap(0x08).read() & 0x80) == 0) {
            i.heap(0x08).write(chars.getFirst() | 0x80);
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

        if (i.heap(0x09).read() == 0x00) {
            i.drawString(singular);
        } else {
            i.drawString(plural);
        }

        return new Address(addr.segment(), decoder.getPointer());
    }

    public static Address printNumber(Interpreter i, int val) {
        i.drawString(String.valueOf(val).chars()
                .map(ch -> ch | 0x80)
                .boxed().toList());
        return i.getIP().incr(OPCODE);
    }
}