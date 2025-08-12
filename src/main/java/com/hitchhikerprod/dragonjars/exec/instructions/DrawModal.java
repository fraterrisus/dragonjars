package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class DrawModal implements Instruction {
    // x is a character address, y is a pixel address
    // bbox is x0,y0,x1,y1
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        i.drawModal(ip.incr(OPCODE));
        return ip.incr(OPCODE + RECTANGLE);
    }
}
