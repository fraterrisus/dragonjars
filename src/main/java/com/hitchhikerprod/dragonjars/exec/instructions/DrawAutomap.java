package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class DrawAutomap implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        i.drawModal(i.loadFromCodeSegment(0x1779, 0, 4)
                .stream().map(Interpreter::byteToInt).toList());
        // TODO 0x16f0
        return ip.incr(OPCODE);
    }
}
