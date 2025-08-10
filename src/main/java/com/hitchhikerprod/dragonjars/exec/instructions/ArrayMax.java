package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.ALU;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class ArrayMax implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int dataAddress = i.readWord(ip.incr(1));
        int maxValue = 0x00;
        int maxIndex = 0;
        final int offset_hi = i.getBX(true) & 0x0000ff00;
        int offset_lo = i.getBL();
        while (true) {
            offset_lo = ALU.decByte(offset_lo);
            if (offset_lo == 0xff) break;
            final int value = i.readByte(i.getDS(), dataAddress + (offset_hi | offset_lo));
            if (value > maxValue) {
                maxValue = value;
                maxIndex = offset_lo;
            }
        }
        i.setAL(maxValue);
        i.setBL(maxIndex);
        return ip.incr(OPCODE + ADDRESS);
    }
}
