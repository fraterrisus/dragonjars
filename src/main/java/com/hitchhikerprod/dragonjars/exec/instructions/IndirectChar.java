package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

import java.util.List;

public class IndirectChar implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final int ax = i.getAX(true);
        final List<Integer> chars = (i.isWide()) ? List.of((ax >> 4) & 0x0f, ax & 0x0f) : List.of(ax & 0x0f);
        i.drawString(chars);
        return i.getIP().incr(OPCODE);
    }
}
