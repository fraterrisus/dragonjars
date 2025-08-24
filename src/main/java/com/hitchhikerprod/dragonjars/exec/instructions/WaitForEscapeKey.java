package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import javafx.scene.input.KeyCode;

public class WaitForEscapeKey implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address nextIP = i.getIP().incr(OPCODE);
        i.app.setKeyHandler(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                i.fillRectangle();
                i.setAX(0x9b);
                i.start(nextIP);
            }
        });
        return null;
    }
}
