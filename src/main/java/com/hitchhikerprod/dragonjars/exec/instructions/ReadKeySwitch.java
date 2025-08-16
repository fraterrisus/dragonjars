package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import javafx.scene.input.KeyCode;

import java.util.HashMap;
import java.util.Map;

public class ReadKeySwitch implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        i.drawString313e(); //?
        final int imm1 = i.memory().read(ip.incr(1), 1);
        int imm2 = i.memory().read(ip.incr(2), 1);
        // [2a44] <- imm1
        // [2a45] <- imm2
        int pointer = ip.offset() + 3;
        // [4a7c] <- imm2 & 0x20;
        if ((imm2 & 0x10) != 0) {
            imm2 = i.memory().read(ip.incr(3), 1);
            pointer++;
        }
        // [2a46] <- imm2
        final Map<KeyCode, Address> prompts = new HashMap<>();
        while (true) {
            final int ch = i.memory().read(ip.segment(), pointer, 1);
            if (ch == 0xff) break;
            final int target = i.memory().read(ip.segment(), pointer + 1, 2);
            prompts.put(Instructions.keyCodeOf(ch), new Address(ip.segment(), target));
            pointer += 3;
        }
        i.setPrompt(prompts);
        return null;
    }
}
