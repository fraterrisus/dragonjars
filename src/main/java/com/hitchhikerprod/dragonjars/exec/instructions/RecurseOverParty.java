package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

import java.util.function.Supplier;

public class RecurseOverParty implements Instruction {
    private record RecursionState (
            Interpreter interpreter,
            Address oldIP,
            int functionPointer,
            int oldSelectedPC,
            int charId
    ) {
        public RecursionState copy(int offset) {
            return new RecursionState(
                    this.interpreter,
                    this.oldIP,
                    this.functionPointer,
                    this.oldSelectedPC,
                    this.charId + offset
            );
        }

        private void after() {
            final Heap.Access selectedPC = Heap.get(Heap.SELECTED_PC);
            final Heap.Access partySize = Heap.get(Heap.PARTY_SIZE);
            if (charId < partySize.read()) {
                selectedPC.write(charId);
                final RecursionState newState = this.copy(1);
                this.interpreter.reenter(
                        this.oldIP.chunkId(this.interpreter.memory()),
                        this.functionPointer,
                        newState::after
                );
            } else {
                selectedPC.write(this.oldSelectedPC);
            }
        }
    }

    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        i.setWidth(false);
        i.setAH(0x00);
        final int funcPtr = i.memory().read(ip.incr(1), 2);
        final Heap.Access selectedPC = Heap.get(Heap.SELECTED_PC);
        final Heap.Access partySize = Heap.get(Heap.PARTY_SIZE);
        if (partySize.read() != 0) {
            final int oldSelectedPC = selectedPC.read();
            final RecursionState newState = new RecursionState(i, ip, funcPtr, oldSelectedPC, 0);
            newState.after();
        }
        return ip.incr(OPCODE + ADDRESS);
    }
}
