package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import javafx.scene.input.KeyCode;

import java.util.ArrayList;
import java.util.List;

public class ReadInputString implements Instruction {
    // write 0x00 at end of string
    // use x_31ed vs bbox_x1 (0x28)
    final List<Integer> chars = new ArrayList<>();

    @Override
    public Address exec(Interpreter i) {
        final Address nextIP = i.getIP().incr();
        chars.clear();
        i.drawChar(0xba); // ':' unclear if this gets written for us
        i.drawChar(0xe7); // prompt box
        i.app.setKeyHandler(event -> {
            final KeyCode keycode = event.getCode();
            final int scancode = keycode.getCode();
            // this probably isn't quite accurate, i made most of it up
            if (keycode == KeyCode.ESCAPE) {
                i.heap(0xc6).write(0x00, 1);
                i.start(nextIP);
            } else if (keycode == KeyCode.ENTER) {
                int ptr = 0xc6;
                for (int ch : chars) i.heap(ptr++).write(ch);
                i.heap(ptr).write(0x00);
                i.start(nextIP);
            } else if (keycode == KeyCode.BACK_SPACE || keycode == KeyCode.DELETE) {
                i.backSpace();
                i.backSpace();
                i.drawChar(0xe7);
            } else if ((0x41 <= scancode && scancode <= 0x5a) || (0x61 <= scancode && scancode <= 0x7a)) {
                if (i.roomToDrawChar()) {
                    final int cap = (scancode & 0xdf) | 0x80;
                    chars.add(cap);
                    i.backSpace();
                    i.drawChar(cap);
                    i.drawChar(0xe7);
                }
            } else if (0x30 <= scancode && scancode <= 0x39) {
                if (i.roomToDrawChar()) {
                    chars.add(scancode | 0x80);
                    i.backSpace();
                    i.drawChar(scancode);
                    i.drawChar(0xe7);
                }
            }
        });
        return null;
    }
}

// a 1010
// b 1011
// c 1100
// d 1101
