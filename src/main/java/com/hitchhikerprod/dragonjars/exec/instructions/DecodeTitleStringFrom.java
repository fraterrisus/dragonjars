package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.ModifiableChunk;
import com.hitchhikerprod.dragonjars.data.StringDecoder;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

import java.util.List;
import java.util.function.Function;

public class DecodeTitleStringFrom implements Instruction {
    // (See 0x2693.) Sets the indirect function pointer to 0x26aa, which saves the decoded string at 0x273a
    // instead of printing it to the screen. Then it resets the indirect function to 0x30c1 and calls a helper
    // (0x26be) which forwards to drawMapTitle() (0x26d4) after doing a bounds check that we ignore, heh. We
    // roll that call into i.setTitleString, below.

    private final Function<Interpreter, Address> getPointer;
    private final boolean saveAX;

    private DecodeTitleStringFrom(Function<Interpreter, Address> getPointer, boolean saveAX) {
        this.getPointer = getPointer;
        this.saveAX = saveAX;
    }

    public static DecodeTitleStringFrom CS = new DecodeTitleStringFrom(i -> i.getIP().incr(OPCODE), false);
    public static DecodeTitleStringFrom DS = new DecodeTitleStringFrom(i -> new Address(i.getDS(), i.getAX(true)), true);

    @Override
    public Address exec(Interpreter i) {
        final Address addr = getPointer.apply(i);
        final StringDecoder decoder = i.stringDecoder();
        final ModifiableChunk chunk = i.memory().getSegment(addr.segment());
        decoder.decodeString(chunk, addr.offset());
        if (saveAX) {
            final int postStringPointer = decoder.getPointer();
            i.setAX(postStringPointer, true);
        }
        final List<Integer> chars = decoder.getDecodedChars();
        i.setTitleString(chars);
        return new Address(addr.segment(), decoder.getPointer());
    }
}
