package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.ModifiableChunk;
import com.hitchhikerprod.dragonjars.data.StringDecoder;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.List;

import static com.hitchhikerprod.dragonjars.exec.Interpreter.PARTY_SEGMENT;
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

    public static final Instruction SOFT_EXIT = (i) -> {
        if (i.getRecursiveDepth() == 1) Platform.exit();
        return null;
    };

    public static final Instruction HARD_EXIT = (i) -> {
        Platform.exit();
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

    public static List<Integer> getStringFromMemory(Interpreter i, Address addr) {
        final List<Integer> nameCh = new ArrayList<>();
        int pointer = addr.offset();
        int ch = 0x80;
        while ((ch & 0x80) > 0) {
            ch = i.memory().read(addr.segment(), pointer, 1);
            nameCh.add(ch);
            pointer++;
        }
        return nameCh;
    }

    public static void indirectFunction(Interpreter i, Address pointer) {
        // In theory we should be tracking what the indirect function currently is.
        // In practice it's basically always 0x30c1, and we've kind of rolled 30c1, draw_string_3031 (0x30a7),
        // and draw_char (0x3167) together because I don't really understand the differences between them.
        final List<Integer> str = getStringFromMemory(i, pointer);
        i.drawString(str);
    }

    public static Address printNumber(Interpreter i, int val) {
        i.drawString(String.valueOf(val).chars()
                .map(ch -> ch | 0x80)
                .boxed().toList());
        return i.getIP().incr(OPCODE);
    }
}