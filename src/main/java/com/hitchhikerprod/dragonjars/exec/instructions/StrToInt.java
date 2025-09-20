package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class StrToInt implements Instruction {
    // heap[37]:4 <- int(heap[0xc6]:?)
    @Override
    public Address exec(Interpreter i) {
        final StringBuilder digits = new StringBuilder();
        int pointer = Heap.INPUT_STRING;
        // skip leading spaces
        while ((Heap.get(pointer).read(1) & 0x7f) == 0x2a) {
            pointer++;
        }
        while (true) {
            final int ord = Heap.get(pointer).read(1);
            final char c = (char)(ord & 0x7f);
            if (c >= '0' && c <= '9') {
                digits.append(c);
            } else {
                break;
            }
            pointer++;
        }
        if (digits.isEmpty()) digits.append('0');
        final int value = Integer.parseInt(digits.toString());
        Heap.get(0x37).write(value, 4);
        return i.getIP().incr();
    }
}
