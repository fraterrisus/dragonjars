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
        final int imm1 = i.readByte(ip.incr(1));
        int imm2 = i.readByte(ip.incr(2));
        // [2a44] <- imm1
        // [2a45] <- imm2
        int pointer = ip.offset() + 3;
        // [4a7c] <- imm2 & 0x20;
        if ((imm2 & 0x10) != 0) {
            imm2 = i.readByte(ip.incr(3));
            pointer++;
        }
        // [2a46] <- imm2
        final Map<KeyCode, Address> prompts = new HashMap<>();
        while (true) {
            final int ch = i.readByte(ip.segment(), pointer);
            if (ch == 0xff) break;
            final int target = i.readWord(ip.segment(), pointer + 1);
            prompts.put(Instructions.keyCodeOf(ch), new Address(ip.segment(), target));
            pointer += 3;
        }
        i.setPrompt(prompts);
        return null;
    }
}
