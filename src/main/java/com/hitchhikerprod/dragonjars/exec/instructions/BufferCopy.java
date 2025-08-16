package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class BufferCopy implements Instruction {
    // Copies a 0x700-byte buffer between a segment (at address AX) and a buffer
    // in the code segment located at 0xd1b0. If BL & 0x80 == 0 we read the buffer
    // and write the segment; otherwise, the other way around.
    @Override
    public Address exec(Interpreter i) {
        final boolean toSegment = (i.getBL() & 0x80) == 0;
        final int segmentId = i.getDS();
        final int index = i.getAX(true);
        for (int offset = 0; offset < 0x700; offset++) {
            if (toSegment) {
                i.memory().write(segmentId, index + offset, 1, i.readBufferD1B0(offset));
            } else {
                i.writeBufferD1B0(offset, i.memory().read(segmentId, index + offset, 1));
            }
        }
        return i.getIP().incr(OPCODE);
    }
}

/*
0x490c  push si
0x490d  mov es, word [mp.ds]
0x4911  mov di, word [mp.ax]
0x4915  mov si, 0xd1b0
0x4918  test byte [mp.bx], 0x80
  // ZF=1 if (a & b) == 0
0x491d  je 0x4925
  // Branch if ZF is true
0x491f  xchg di, si
0x4921  push es
0x4922  push ds
0x4923  pop es
0x4924  pop ds
0x4925  mov cs, 0x380
0x4928  rep movsw word es:[di], word ds:[si]
  // For legacy mode, move word from address DS:SI to ES:DI
0x492a  push cs
0x492b  pop ds
0x492c  pop si
0x492d  jump &mp.restore_es
 */