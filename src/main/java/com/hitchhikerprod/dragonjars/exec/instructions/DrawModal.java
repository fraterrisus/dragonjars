package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

import java.util.List;

public class DrawModal implements Instruction {
    // x is a character address, y is a pixel address
    // bbox is x0,y0,x1,y1
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final List<Byte> bytes = i.memory().readList(ip.incr(), 4);
        i.drawModal(bytes.stream().map(Interpreter::byteToInt).toList());
        i.composeVideoLayers(false, false, true);
        return ip.incr(OPCODE + RECTANGLE);
    }
}
