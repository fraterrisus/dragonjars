package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

import java.util.List;

import static com.hitchhikerprod.dragonjars.exec.Interpreter.PARTY_SEGMENT;

public class IndirectCharItem implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final int selectedItem = Heap.get(Heap.SELECTED_ITEM).read();
        final int marchingOrder = Heap.get(Heap.SELECTED_PC).read();
        final int pcBaseAddress = Heap.get(Heap.MARCHING_ORDER + marchingOrder).read() << 8;
        final int itemBaseAddress = pcBaseAddress + 0xec + (0x17 * selectedItem);
        final Address itemPointer = new Address(PARTY_SEGMENT, itemBaseAddress + 0x0b);
        final List<Integer> string = Instructions.getStringFromMemory(i, itemPointer);
        i.addToString313e(string);
        // Instructions.indirectFunction(i, itemPointer);
        return i.getIP().incr();
    }
}
