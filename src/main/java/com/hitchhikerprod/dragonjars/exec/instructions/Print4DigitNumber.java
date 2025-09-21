package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class Print4DigitNumber implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final int value = i.getAX(true);
        printNumber(i, value);
        return i.getIP().incr(OPCODE);
    }

    public static void printNumber(Interpreter i, int val) {
        i.addToStringBuffer(String.valueOf(val).chars()
                .map(ch -> ch | 0x80)
                .boxed().toList());
    }
}
