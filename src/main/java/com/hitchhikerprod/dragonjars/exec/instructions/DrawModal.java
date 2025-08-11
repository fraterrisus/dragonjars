package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class DrawModal implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        // bx <- si
        // cx <- es
        // es <- cs
        // draw_modal(bx,cx)
        return ip.incr(OPCODE + RECTANGLE);
    }
}
