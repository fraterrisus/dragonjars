package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.ALU;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class CmpBLHeap implements Instruction {
    // See CmpAXHeap for comments
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int heapIndex = i.readByte(ip.incr(1));
        final ALU.Result result = ALU.subByte(i.getBL(), i.getHeapBytes(heapIndex, 1));
        i.setCarryFlag(!result.carry());
        i.setSignFlag(result.sign());
        i.setZeroFlag(result.zero());
        System.out.format("  cmp bl=%02x heap[imm=%02x]=%02x -> zf:%d sf:%d cf:%d\n",
                i.getBL(), i.readByte(ip.incr(1)), i.getHeapBytes(heapIndex, 1),
                result.zero() ? 1 : 0, result.sign() ? 1 : 0, result.carry() ? 0 : 1);
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
