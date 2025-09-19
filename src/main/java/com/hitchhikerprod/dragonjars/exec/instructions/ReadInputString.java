package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import javafx.scene.input.KeyCode;

import java.util.ArrayList;
import java.util.List;

public class ReadInputString implements Instruction {
    // write 0x00 at end of string
    // use x_31ed vs bbox_x1 (0x28)
    private static final int PROMPT_BOX = 0x0fe;

    private final List<Integer> chars = new ArrayList<>();

    @Override
    public Address exec(Interpreter i) {
        final Address nextIP = i.getIP().incr();
        i.drawString313e();

        chars.clear();
        i.drawChar(0xba);
        i.drawChar(PROMPT_BOX);
        i.app().setKeyHandler(event -> {
            final KeyCode keycode = event.getCode();
            final int scancode = keycode.getCode();
            // this probably isn't quite accurate, i made most of it up
            if (keycode == KeyCode.ESCAPE) {
                i.doLater(j -> {
                    j.heap(Heap.INPUT_STRING).write(0x00, 1);
                    j.start(nextIP);
                });
            } else if (keycode == KeyCode.ENTER) {
                i.doLater(j -> {
                    int ptr = Heap.INPUT_STRING;
                    for (int ch : chars) j.heap(ptr++).write(ch);
                    j.heap(ptr).write(0x00);
                    j.start(nextIP);
                });
            } else if (keycode == KeyCode.BACK_SPACE || keycode == KeyCode.DELETE) {
                i.doLater(j -> {
                    if (!chars.isEmpty()) {
                        chars.removeLast();
                        j.backSpace();
                        j.backSpace();
                        j.drawChar(PROMPT_BOX);
                    }
                });
            } else if ((0x41 <= scancode && scancode <= 0x5a) || (0x61 <= scancode && scancode <= 0x7a)) {
                i.doLater(j -> {
                    if (j.roomToDrawChar()) {
                        final int cap = (event.isShiftDown()) ? (scancode) | 0x80 : scancode | 0xe0;
                        chars.add(cap);
                        j.backSpace();
                        j.drawChar(cap);
                        j.drawChar(PROMPT_BOX);
                    }
                });
            } else if (0x30 <= scancode && scancode <= 0x39) {
                i.doLater(j -> {
                    if (j.roomToDrawChar()) {
                        chars.add(scancode | 0x80);
                        j.backSpace();
                        j.drawChar(scancode);
                        j.drawChar(PROMPT_BOX);
                    }
                });
            }
        });
        return null;
    }
}

// a 1010
// b 1011
// c 1100
// d 1101
