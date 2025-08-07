package com.hitchhikerprod.dragonjars.exec;

public class ALU {
    public record Result(int value, boolean carry, boolean sign, boolean zero) {}

    public static Result addWord(int op1, int op2) {
        final int value = (op1 & 0xffff) + (op2 & 0xffff);
        return new Result(
                value & 0xffff,
                (value & 0x10000) > 0,
                (value & 0x8000) > 0,
                (value & 0xffff) == 0
        );
    }

    public static Result addByte(int op1, int op2) {
        final int value = (op1 & 0xff) + (op2 & 0xff);
        return new Result(
                value & 0xff,
                (value & 0x100) > 0,
                (value & 0x80) > 0,
                (value & 0xff) == 0
        );
    }

    public static Result subWord(int op1, int op2) {
        final int value = (op1 & 0xffff) - (op2 & 0xffff);
        return new Result(
                value & 0xffff,
                // Borrow = we started with a positive number and ended with a negative one
                ((op1 & 0x8000) == 0) && ((value & 0x8000) > 0),
                (value & 0x8000) > 0,
                (value & 0xffff) == 0
        );
    }

    public static Result subByte(int op1, int op2) {
        final int value = (op1 & 0xff) - (op2 & 0xff);
        return new Result(
                value & 0xff,
                ((op1 & 0x80) == 0) && ((value & 0x80) > 0),
                (value & 0x80) > 0,
                (value & 0xff) == 0
        );
    }
}
