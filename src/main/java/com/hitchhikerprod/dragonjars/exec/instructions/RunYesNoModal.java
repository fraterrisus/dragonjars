package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import javafx.scene.input.KeyCode;

import java.util.List;

public class RunYesNoModal implements Instruction {
    // encoded at 0x47eb in the middle of the 0x8c handler, fuckers
    private static final List<Integer> YES_NO_STRING = List.of(
            0xd9, 0xe5, 0xf3, 0x8d, 0xce, 0xef // "Yes\nNo"
    );

    @Override
    public Address exec(Interpreter i) {
        i.addToString313e(YES_NO_STRING);
        i.drawString313e();
        final Address nextIP = i.getIP().incr(OPCODE);
        i.app().setKeyHandler(event -> {
            if (event.getCode() == KeyCode.Y) {
                i.setZeroFlag(true);
                i.start(nextIP);
            } else if (event.getCode() == KeyCode.N) {
                i.setZeroFlag(false);
                i.start(nextIP);
            }
        });
        return null;
    }
}
