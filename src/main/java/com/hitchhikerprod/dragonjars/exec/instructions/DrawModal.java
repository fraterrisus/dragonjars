package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.CharRectangle;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

import java.util.List;

public class DrawModal implements Instruction {
    // x is a character address, y is a pixel address
    // bbox is x0,y0,x1,y1
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();

        final List<Integer> bytes = i.memory().readList(ip.incr(), 4)
                .stream().map(Interpreter::byteToInt).toList();
        final int x0 = bytes.get(0);
        final int y0 = bytes.get(1);
        final int x1 = bytes.get(2);
        final int y1 = bytes.get(3);
        i.drawModal(new CharRectangle(x0, y0, x1, y1));

        return ip.incr(OPCODE + RECTANGLE);
    }
}
