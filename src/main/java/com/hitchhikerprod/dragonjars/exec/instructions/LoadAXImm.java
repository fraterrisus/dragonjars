package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class LoadAXImm implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final int offset = i.getIP().offset();
        final int value = (i.isWide()) ? i.readWord(offset + 1) : i.readByte(offset + 1);
        i.setAX(value);
        return i.getIP().incr(OPCODE + wordSize(i));
    }
}
