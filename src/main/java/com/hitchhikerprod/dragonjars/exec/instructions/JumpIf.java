package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

import java.util.function.Function;

public class JumpIf implements Instruction {
    private final Function<Interpreter, Boolean> takeJump;

    public JumpIf(Function<Interpreter, Boolean> condition) {
        this.takeJump = condition;
    }

    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int address = i.readWord(ip.incr(1));
        if (takeJump.apply(i)) {
            return new Address(ip.segment(), address);
        } else {
            return ip.incr(OPCODE + ADDRESS);
        }
    }
}
