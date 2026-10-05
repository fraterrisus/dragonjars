package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class RecurseOverInventory implements Instruction {
    private record RecursionState(
            Interpreter interpreter,
            int chunkId,
            int functionPointer,
            int itemBaseAddress,
            int slotId
    ) {
        public RecursionState copy(int offset) {
            return new RecursionState(
                    this.interpreter,
                    this.chunkId,
                    this.functionPointer,
                    this.itemBaseAddress + (0x17 * offset),
                    this.slotId + offset
            );
        }

        private boolean before() {
            Heap.get(Heap.SELECTED_ITEM).write(slotId);
            final int flag = this.interpreter.memory().read(Interpreter.PARTY_SEGMENT, itemBaseAddress + 0x0b, 1);
            return (flag == 0);
        }

        private void after() {
            if (this.interpreter.getCarryFlag()) return;
            if (this.slotId == 11) {
                Heap.get(Heap.SELECTED_ITEM).write(12);
                this.interpreter.setCarryFlag(false);
                return;
            }
            final RecursionState newState = this.copy(1);
            if (newState.before()) this.interpreter.setCarryFlag(false);
            else this.interpreter.reenter(chunkId, functionPointer, newState::after);
        }
    }

    @Override
    public Address exec(Interpreter i) {
        i.setWidth(false); // 0x4237
        i.setAH(0x00);
        final Address ip = i.getIP();
        final Address nextIP = ip.incr(OPCODE + ADDRESS);
        final int chunkId = ip.chunkId(i.memory());
        final int functionPointer = i.memory().read(ip.incr(), 2);
        final int marchingOrder = Heap.get(Heap.SELECTED_PC).read();
        final int pcBaseAddress = Heap.get(Heap.MARCHING_ORDER + marchingOrder).read() << 8;

        final RecursionState state0 = new RecursionState(i, chunkId, functionPointer, pcBaseAddress + 0xec, 0);
        if (state0.before()) {
            i.setCarryFlag(false);
        } else {
            i.reenter(chunkId, functionPointer, state0::after);
        }
        return nextIP;
    }
}
