package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.hitchhikerprod.dragonjars.exec.instructions.Instruction.OPCODE;

public class Instructions {
    public static final Instruction SET_WIDE = (i) -> {
        i.setWidth(true);
        return i.getIP().incr(OPCODE);
    };

    public static final Instruction SET_NARROW = (i) -> {
        i.setWidth(false);
        i.setAH(0x00);
        return i.getIP().incr(OPCODE);
    };

    public static final Instruction PUSH_CS = (i) -> {
        i.pushByte(i.getCS());
        return i.getIP().incr(OPCODE);
    };

    public static final Instruction PUSH_DS = (i) -> {
        i.pushByte(i.getDS());
        return i.getIP().incr(OPCODE);
    };

    public static final Instruction POP_DS = (i) -> {
        i.setDS(i.popByte());
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

    public static final Instruction COPY_HEAP_3F_3E = (i) -> {
        final int value = i.heap(Heap.NEXT_EVENT).read();
        i.heap(Heap.RECENT_EVENT).write(value);
        return i.getIP().incr(OPCODE);
    };

    public static final Instruction FILL_BBOX = (i) -> {
        i.fillRectangle();
        return i.getIP().incr();
    };

    public static final Instruction NOOP = (i) -> i.getIP().incr(OPCODE);

    public static final Instruction SOFT_EXIT = Interpreter::finish;

    public static final Instruction HARD_EXIT = (i) -> {
        if (Objects.nonNull(i.app())) i.app().close();
        return null;
    };

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
}