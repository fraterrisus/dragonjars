package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

import java.util.function.Function;

public class JumpIf implements Instruction {
    private final Function<Interpreter, Boolean> takeJump;

    public static final JumpIf ALWAYS = new JumpIf(i -> true);
    public static final JumpIf NOT_CARRY = new JumpIf(i -> !i.getCarryFlag());
    public static final JumpIf CARRY = new JumpIf(Interpreter::getCarryFlag);
    public static final JumpIf ABOVE = new JumpIf(i -> i.getCarryFlag() & ! i.getZeroFlag());
    public static final JumpIf EQUAL = new JumpIf(Interpreter::getZeroFlag);
    public static final JumpIf NOT_EQUAL = new JumpIf(i -> !i.getZeroFlag());
    public static final JumpIf SIGN = new JumpIf(Interpreter::getSignFlag);
    public static final JumpIf NOT_SIGN = new JumpIf(i -> !i.getSignFlag());

    private JumpIf(Function<Interpreter, Boolean> condition) {
        this.takeJump = condition;
    }

    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int address = i.memory().read(ip.incr(1), 2);
        if (takeJump.apply(i)) {
            return new Address(ip.segment(), address);
        } else {
            return ip.incr(OPCODE + ADDRESS);
        }
    }
}
