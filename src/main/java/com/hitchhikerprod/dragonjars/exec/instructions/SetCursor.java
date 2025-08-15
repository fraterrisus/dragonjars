package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class SetCursor implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        i.drawString313e();
        final Interpreter.Rectangle bbox = i.getBBox();
        i.y_31ef = i.getBX(true) + bbox.y0();
        i.x_31ed = i.getAX(true) + bbox.x0();
        i.x_3166 = i.x_31ed;
        return i.getIP().incr();
    }
}
