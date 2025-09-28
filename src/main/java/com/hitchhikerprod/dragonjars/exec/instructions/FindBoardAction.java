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

import java.util.List;

public class FindBoardAction implements Instruction {
    Interpreter i;
    List<Action> actions;
    int actionIndex;

    @Override
    public Address exec(Interpreter i) {
        // The assembly for this is a lot more complicated; this is a major candidate for bug fixes.
        final Address nextIP = i.getIP().incr();
        if (Heap.get(Heap.BOARD_ID).read() != Heap.get(Heap.DECODED_BOARD_ID).read()) return nextIP;
        this.i = i;
        this.actions = i.mapDecoder().getActions();
        this.actionIndex = 0;
//        System.out.println("FindBoardActions");
        return searchForActions(nextIP);
    }

    private Address searchForActions(Address nextIP) {
        final PartyLocation loc = Heap.getPartyLocation();
        final MapData.Square square = i.mapDecoder().getSquare(loc.pos());
//        System.out.println("  square: " + square);

        final int marchingOrder = Heap.get(Heap.SELECTED_PC).read();
        final int pcBaseAddress = Heap.get(Heap.MARCHING_ORDER + marchingOrder).read() << 8;
        final int itemNumber = Heap.get(Heap.SELECTED_ITEM).read();
        final int itemBaseAddress = pcBaseAddress + Memory.PC_INVENTORY + (itemNumber * 0x17);
        final Chunk partyData = i.memory().getSegment(Interpreter.PARTY_SEGMENT);
        final int itemUsed = Heap.get(0x88).read(); // 0x40: item used, 0x0f: item index (== h[07]?)
        final int skillUsed = Heap.get(0x85).read();

        while (actionIndex < actions.size()) {
            final Action action = actions.get(actionIndex++);
//            System.out.println("  action: " + action);

            boolean found = false;

            switch (action) {
                case ItemAction a -> {
                    if (itemUsed == 0) continue;
                    if (!matchingSpecial(action, square)) continue;
                    if (a.getItemIndex() == 0xff) {
                        found = true;
                    } else {
                        final Item matchItem = i.mapDecoder().getItem(a.getItemIndex());
                        final Item usedItem = new Item(partyData).decode(itemBaseAddress);
                        if (usedItem.equals(matchItem)) found = true;
                    }
                }
                case SkillAction a -> {
                    if (!matchingSpecial(action, square)) continue;
                    if (a.header() == skillUsed) found = true;
                }
                case SpellAction a -> {
                    if (!matchingSpecial(action, square)) continue;
                    if (a.header() == skillUsed) found = true;
                }
                case MatchAction a -> {
                    if (a.header() == 0xfe) {
                        found = true;
                    } else {
                        if (!matchingSpecial(action, square)) continue;
                        if (a.header() == 0xfd) found = true;
                    }
                }
                default -> throw new RuntimeException();
            }

            if (found) {
//                System.out.println("  match found");
                final int eventPointer = i.mapDecoder().getEventPointer(action.event() + 1);
                if (eventPointer != 0) { // disabled dynamically
                    final Address target = new Address(Heap.get(Heap.BOARD_1_SEGIDX).read(), eventPointer);
                    i.reenter(target, () -> {
//                        System.out.println("  return; carry = " + i.getCarryFlag());
                        if (i.getCarryFlag()) return nextIP;
                        else return searchForActions(nextIP);
                    });
                    return null;
                }
            }
        }

        i.setCarryFlag(false);
        return nextIP;
    }

    private boolean matchingSpecial(Action action, MapData.Square square) {
        return (action.special() == 0 || action.special() == square.specialId());
    }
}
