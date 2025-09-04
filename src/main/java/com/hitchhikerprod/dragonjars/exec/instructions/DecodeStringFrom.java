package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.ModifiableChunk;
import com.hitchhikerprod.dragonjars.data.StringDecoder;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/*       0 1 2 3 4 5 6 7 8 9 A B C D E F
 *   Ax    ! " # $ % & ' ( ) * + , - . /   0xa0: space
 *   Bx  0 1 2 3 4 5 6 7 8 9 : ; < = > ?
 *   Cx  @ A B C D E F G H I J K L M N O
 *   Dx  P Q R S T U V W X Y Z [ \ ] ^ _
 *   Ex  ` a b c d e f g h i j k l m n o
 *   Fx  p q r s t u v w x y z { | }       0xfe: black box  0xff: white box
 */

public class DecodeStringFrom implements Instruction {
    // See chunk 0x0f adr 0x0102 pauseGame():
    // @0x107 ins 78 decodeString seems to write "The game is paused" to the 313e string
    // @0x115 inx 89 readKeySwitch starts by running printString313e
    private final Function<Interpreter, Address> getPointer;
    private final Function<Interpreter, Address> getNextIp;
    private final BiConsumer<Interpreter, List<Integer>> outputFn;
    private final boolean withFill;
    private final boolean saveAX;

    private DecodeStringFrom(
            Function<Interpreter, Address> getPointer,
            Function<Interpreter, Address> getNextIp,
            BiConsumer<Interpreter, List<Integer>> outputFn,
            boolean withFill,
            boolean saveAX
    ) {
        this.getPointer = getPointer;
        this.getNextIp = getNextIp;
        this.outputFn = outputFn;
        this.withFill = withFill;
        this.saveAX = saveAX;
    }

    private static final BiConsumer<Interpreter, List<Integer>> DRAW_STRING = (i, ch) -> { i.drawString(ch); i.composeVideoLayers(false, false, true); };

    public static DecodeStringFrom CS = new DecodeStringFrom(
            i -> i.getIP().incr(OPCODE),
            i -> new Address(i.getIP().segment(), i.stringDecoder().getPointer()),
            DRAW_STRING,
            false,
            false
    );

    public static DecodeStringFrom CS_WITH_FILL = new DecodeStringFrom(
            i -> i.getIP().incr(OPCODE),
            i -> new Address(i.getIP().segment(), i.stringDecoder().getPointer()),
            DRAW_STRING,
            true,
            false
    );

    public static DecodeStringFrom DS = new DecodeStringFrom(
            i -> new Address(i.getDS(), i.getAX(true)),
            i -> i.getIP().incr(OPCODE),
            DRAW_STRING,
            false,
            true
    );

    public static DecodeStringFrom DS_WITH_FILL = new DecodeStringFrom(
            i -> new Address(i.getDS(), i.getAX(true)),
            i -> i.getIP().incr(OPCODE),
            DRAW_STRING,
            true,
            true
    );

    public static DecodeStringFrom CS_TO_TITLE = new DecodeStringFrom(
            i -> i.getIP().incr(OPCODE),
            i -> new Address(i.getIP().segment(), i.stringDecoder().getPointer()),
            Interpreter::setTitleString,
            false,
            false
    );

    public static DecodeStringFrom DS_TO_TITLE = new DecodeStringFrom(
            i -> new Address(i.getDS(), i.getAX(true)),
            i -> i.getIP().incr(OPCODE),
            Interpreter::setTitleString,
            false,
            true
    );


    @Override
    public Address exec(Interpreter i) {
        if (withFill) i.fillRectangle();
        final Address addr = getPointer.apply(i);
        final StringDecoder decoder = i.stringDecoder();
        final ModifiableChunk chunk = i.memory().getSegment(addr.segment());
        decoder.decodeString(chunk, addr.offset());
        final List<Integer> chars = decoder.getDecodedChars();
        if (saveAX) {
            final int postStringPointer = decoder.getPointer();
            i.setAX(postStringPointer, true);
        }
        if (chars.getFirst() == 0x00) return getNextIp.apply(i);

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
                    writePlural = !writePlural;
                }
                case 0xdc -> {
                    writePlural = true;
                    writeSingular = !writeSingular;
                }
                default -> {
                    if (writeSingular) singular.add(ch);
                    if (writePlural) plural.add(ch);
                }
            }
        }

        outputFn.accept(i, (i.heap(0x09).read() == 0x00) ? singular : plural);
        return getNextIp.apply(i);
    }
}
