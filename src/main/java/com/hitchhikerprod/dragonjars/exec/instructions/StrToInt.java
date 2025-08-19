package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class StrToInt implements Instruction {
    // heap[37]:4 <- int(heap[0xc6]:?)
    @Override
    public Address exec(Interpreter i) {
        final StringBuilder digits = new StringBuilder();
        int pointer = 0xc6;
        while (true) {
            final int ord = i.heap(pointer).read(1);
            final char c = (char)(ord & 0x7f);
            // The original code skips *leading* spaces, but this should also work.
            if (c == ' ') continue;
            if (c >= '0' && c <= '9') {
                digits.append(c);
            } else {
                break;
            }
            pointer++;
        }
        final int value = Integer.parseInt(digits.toString());
        i.heap(0x37).write(value, 4);
        return i.getIP().incr();
    }
}
