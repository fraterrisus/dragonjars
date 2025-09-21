package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

import java.util.List;

import static com.hitchhikerprod.dragonjars.exec.Interpreter.PARTY_SEGMENT;

public class IndirectCharName implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final int marchingOrder = Heap.get(Heap.SELECTED_PC).read();
        final int pcBaseAddress = Heap.get(Heap.MARCHING_ORDER + marchingOrder).read() << 8;
        final Address namePointer = new Address(PARTY_SEGMENT, pcBaseAddress);
        final List<Integer> string = Instructions.getStringFromMemory(i, namePointer);
        i.addToStringBuffer(string);
        // Instructions.indirectFunction(i, namePointer);
        return i.getIP().incr();
    }
}
