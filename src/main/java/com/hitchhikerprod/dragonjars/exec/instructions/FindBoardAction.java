package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Action;
import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.Item;
import com.hitchhikerprod.dragonjars.data.ItemAction;
import com.hitchhikerprod.dragonjars.data.MapData;
import com.hitchhikerprod.dragonjars.data.MatchAction;
import com.hitchhikerprod.dragonjars.data.PartyLocation;
import com.hitchhikerprod.dragonjars.data.SkillAction;
import com.hitchhikerprod.dragonjars.data.SpellAction;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import com.hitchhikerprod.dragonjars.exec.Memory;

import java.util.Objects;

public class FindBoardAction implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        // The assembly for this is a lot more complicated; this is a major candidate for bug fixes.
        final Address nextIP = i.getIP().incr();
        if (Heap.get(Heap.BOARD_ID).read() != Heap.get(Heap.DECODED_BOARD_ID).read()) return nextIP;

        final PartyLocation loc = Heap.getPartyLocation();
        final MapData.Square square = i.mapDecoder().getSquare(loc.pos());

        final int marchingOrder = Heap.get(Heap.SELECTED_PC).read();
        final int pcBaseAddress = Heap.get(Heap.MARCHING_ORDER + marchingOrder).read() << 8;
        final int itemNumber = Heap.get(Heap.SELECTED_ITEM).read();
        final int itemBaseAddress = pcBaseAddress + Memory.PC_INVENTORY + (itemNumber * 0x17);
        final Chunk partyData = i.memory().getSegment(Interpreter.PARTY_SEGMENT);
        final int itemUsed = Heap.get(0x88).read();
        final int skillUsed = Heap.get(0x85).read();

        Action matchingAction = null;
        for (Action action : i.mapDecoder().getActions()) {

            switch (action) {
                case ItemAction a -> {
                    if (itemUsed == 0) continue;
                    if (square.specialId() != action.special()) continue;
                    final Item matchItem = i.mapDecoder().getItem(a.getItemIndex());
                    final Item usedItem = new Item(partyData).decode(itemBaseAddress);
                    if (usedItem.equals(matchItem)) matchingAction = action;
                }
                case SkillAction a -> {
                    if (square.specialId() != action.special()) continue;
                    if (a.header() == skillUsed) matchingAction = action;
                }
                case SpellAction a -> {
                    if (square.specialId() != action.special()) continue;
                    if (a.header() == skillUsed) matchingAction = action;
                }
                case MatchAction a -> {
                    if (a.header() == 0xfe) matchingAction = action;
                    if (square.specialId() != action.special()) continue;
                    if (a.header() == 0xfd) matchingAction = action;
                }
                default -> throw new RuntimeException();
            }
            if (Objects.nonNull(matchingAction)) break;
        }

        if (Objects.nonNull(matchingAction)) {
            final Address target = new Address(
                    Heap.get(Heap.BOARD_1_SEGIDX).read(),
                    i.mapDecoder().getEventPointer(matchingAction.event() + 1)
            );
            i.reenter(target, () -> { i.setCarryFlag(true); return nextIP; });
            return null;
        } else {
            i.setCarryFlag(false);
            return nextIP;
        }
    }
}
