package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class DrawAutomap implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final byte[] buf = new byte[4];
        i.loadFromCodeSegment(0x1779, 0, buf, 4);
        i.drawModal(buf[0], buf[1], buf[2], buf[3]);
        // TODO 0x16f0
        return ip.incr(OPCODE);
    }
}
