package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class DecodeStringCS implements Instruction {
    // See chunk 0x0f adr 0x0102 pauseGame():
    // @0x107 ins 78 decodeString seems to write "The game is paused" to the 313e string
    // @0x115 inx 89 readKeySwitch starts by running printString313e
    @Override
    public Address exec(Interpreter i) {
        return Instructions.decodeString(i, i.getIP().incr(OPCODE));
    }
}
