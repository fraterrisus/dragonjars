package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class WaitForEscapeKey implements Instruction {
    // Draws the "Press ESC" footer, but accepts any key.
    @Override
    public Address exec(Interpreter i) {
        final Address nextIP = i.getIP().incr(OPCODE);
        i.drawString313e();
        i.printFooter(0x02); // hardcoded at 0x2bab
        i.app().setKeyHandler(event -> {
            if (event.getCode().isModifierKey()) return;
            i.fillRectangle();
            i.start(nextIP);
        });
        return null;
    }
}
