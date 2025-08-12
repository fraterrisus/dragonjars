package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.ALU;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class CmpAXHeap implements Instruction {
    // Perform subtraction, set the flags the same way, but don't set the result
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int heapIndex = i.readByte(ip.incr(1));
        final ALU.Result result;
        if (i.isWide()) {
            result = ALU.subWord(i.getAX(), i.getHeapBytes(heapIndex, 2));
            System.out.format("  cmp ax=%04x heap[imm=%02x]=%04x -> zf:%d sf:%d cf:%d\n",
                    i.getAX(), i.readByte(ip.incr(1)), i.getHeapBytes(heapIndex, 2),
                    result.zero() ? 1 : 0, result.sign() ? 1 : 0, result.carry() ? 0 : 1);
        } else {
            result = ALU.subByte(i.getAL(), i.getHeapBytes(heapIndex, 1));
            System.out.format("  cmp al=%02x heap[imm=%02x]=%02x -> zf:%d sf:%d cf:%d\n",
                    i.getAL(), i.readByte(ip.incr(1)), i.getHeapBytes(heapIndex, 1),
                    result.zero() ? 1 : 0, result.sign() ? 1 : 0, result.carry() ? 0 : 1);
        }
        // I don't know why the assembly code flips CF before writing it, but it does
        i.setCarryFlag(!result.carry());
        i.setSignFlag(result.sign());
        i.setZeroFlag(result.zero());
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
