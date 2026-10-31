package com.hitchhikerprod.dragonjars.exec;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.ui.AppPreferences;
import javafx.application.Platform;

import java.util.List;
import java.util.function.Consumer;

record Patch(int chunkId, int ip, Consumer<Interpreter> fn) {

    private static final List<Patch> PATCHES = List.of(
            // Debugging a threading conflict.
//            new Patch(0x003, 0x06b5, (i) -> {
//                System.out.format("[03:06b5] ax %04x bx %04x\n", i.getAX(), i.getBX());
//                final ModifiableChunk dataSegment = i.memory().unsafeGetSegment(i.getDS()).chunk();
//                System.out.format("[%02x:04c6] %04x %04x %04x %04x\n", i.getDS(),
//                        dataSegment.getWord(0x04c6),
//                        dataSegment.getWord(0x04c8),
//                        dataSegment.getWord(0x04ca),
//                        dataSegment.getWord(0x04cc));
//            }),

            // Open the paragraph window when the game prompts you to do so. Some of these are hardcoded in strings, so
            // I have to hardcode them here as well.
            new Patch(0x008, 0x02f1, (i) -> openParagraph(i, i.getAX())),
            new Patch(0x046 + 0x08, 0x03aa, (i) -> openParagraph(i, 30)),
            new Patch(0x046 + 0x12, 0x10ff, (i) -> {
//                final Address flag = new Address(i.memory().lookupChunkId(0x046 + 0x12), 0x1105);
                final Chunk chunk = i.memory().unsafeGetChunk(0x046 + 0x12);
                final int bitsplit = chunk.read(0x1105, 1);
                final int offset = chunk.read(0x1106, 1);
                if (Heap.readFlag(bitsplit, offset) > 0) openParagraph(i, 138);
            }),
            new Patch(0x046 + 0x18, 0x06c5, (i) -> openParagraph(i, 80)),
            new Patch(0x046 + 0x1e, 0x065b, (i) -> openParagraph(i, 91)),
            new Patch(0x046 + 0x24, 0x041b, (i) -> openParagraph(i, 42)),

            // Confirmed that the Steam data still needs this patch
            new Patch(0x003, 0x0905, Patch::bugfixCastActionAvDvMod),

            // Confirmed that the Steam data still needs this patch
            new Patch(0x003, 0x0d68, Patch::selectDamageDie),

            // Instrumentation points that gather combat data for the CombatLog window.
            new Patch(0x003, 0x0000, (i) -> i.setCombatData(new CombatData(i))),
            new Patch(0x003, 0x00e1, (i) -> i.setCombatData(null)),
            new Patch(0x003, 0x006c, (i) -> i.combatData().ifPresent(c -> c.decodeInitiative())),
            new Patch(0x012, 0x0097, (i) -> i.combatData().ifPresent(c -> c.getCombatants())),
            new Patch(0x003, 0x00df, (i) -> i.combatData().ifPresent(c -> c.endCombat())),
            new Patch(0x003, 0x078c, (i) -> i.combatData().ifPresent(c -> c.applyDamageToPC())),
            new Patch(0x003, 0x07d3, (i) -> i.combatData().ifPresent(c -> c.applyDamageToMonster())),
            new Patch(0x003, 0x0b00, (i) -> i.combatData().ifPresent(c -> c.partyTurn())),
            new Patch(0x003, 0x0b98, (i) -> i.combatData().ifPresent(c -> c.partyEquip())),
            new Patch(0x003, 0x0c06, (i) -> i.combatData().ifPresent(c -> c.partyMove(i.getAL()))),
            new Patch(0x003, 0x0c2e, (i) -> i.combatData().ifPresent(c -> c.partySpell())),
            new Patch(0x003, 0x0cdc, (i) -> i.combatData().ifPresent(c -> c.partyAttackTarget())),
            new Patch(0x003, 0x0e66, (i) -> i.combatData().ifPresent(c -> c.partyAttackBlocked())),
            new Patch(0x003, 0x0d3d, (i) -> i.combatData().ifPresent(c -> Heap.get(0x7b).write(0xff))),
            new Patch(0x003, 0x0d41, (i) -> i.combatData().ifPresent(c -> c.partyAttackHits())),
            new Patch(0x003, 0x0d81, (i) -> i.combatData().ifPresent(c -> c.partyDamageBonus(i.getAL()))),
            new Patch(0x003, 0x0d94, (i) -> i.combatData().ifPresent(c -> c.partyDamageMighty(i.getAL()))),
            new Patch(0x003, 0x0dd5, (i) -> i.combatData().ifPresent(c -> c.partyDamage())),
            new Patch(0x003, 0x0fb8, (i) -> i.combatData().ifPresent(c -> c.monsterTurn())),
            new Patch(0x003, 0x0ffc, (i) -> i.combatData().ifPresent(c -> c.monsterConfidence(i.getAL()))),
            new Patch(0x003, 0x1023, (i) -> i.combatData().ifPresent(c -> c.monsterBravery(i.getBL()))),
            new Patch(0x003, 0x108f, (i) -> i.combatData().ifPresent(c -> c.monsterAction(i.getAL()))),
            new Patch(0x003, 0x10e2, (i) -> i.combatData().ifPresent(c -> c.monsterFlees(true))),
            new Patch(0x003, 0x10f2, (i) -> i.combatData().ifPresent(c -> c.monsterFlees(false))),
            new Patch(0x003, 0x13d6, (i) -> i.combatData().ifPresent(c -> c.monsterRearms())),
            new Patch(0x003, 0x1113, (i) -> i.combatData().ifPresent(c -> c.monsterBlocks())),
            new Patch(0x003, 0x1196, (i) -> i.combatData().ifPresent(c -> c.monsterCalls(true))),
            new Patch(0x003, 0x11a3, (i) -> i.combatData().ifPresent(c -> c.monsterCalls(false))),
            new Patch(0x003, 0x11f8, (i) -> i.combatData().ifPresent(c -> c.monsterAttackHits())),
            new Patch(0x003, 0x127f, (i) -> i.combatData().ifPresent(c -> c.monsterDamage())),
            new Patch(0x003, 0x1305, (i) -> i.combatData().ifPresent(c -> c.monsterAttackBlocked())),
            new Patch(0x003, 0x1326, (i) -> i.combatData().ifPresent(c -> c.monsterAttackTarget())),
            new Patch(0x003, 0x1379, (i) -> i.combatData().ifPresent(c -> Heap.get(0x7b).write(0xff))),
            new Patch(0x003, 0x142e, (i) -> i.combatData().ifPresent(c -> c.monsterAdvances(true))),
            new Patch(0x003, 0x1453, (i) -> i.combatData().ifPresent(c -> c.monsterAdvances(false))),
            new Patch(0x003, 0x14c3, (i) -> i.combatData().ifPresent(c -> c.monsterSpell())),
            new Patch(0x003, 0x1508, (i) -> i.combatData().ifPresent(c -> c.monsterBreath())),
            new Patch(0x006, 0x03e4, (i) -> i.combatData().ifPresent(c -> c.partyHeal())),
            new Patch(0x006, 0x0631, (i) -> i.combatData().ifPresent(c -> c.decodeSpellDamage())),
            new Patch(0x006, 0x0678, (i) -> i.combatData().ifPresent(c -> Heap.get(0x7b).write(0xff))),
            new Patch(0x006, 0x067d, (i) -> i.combatData().ifPresent(c -> c.partySpellTarget())),
            new Patch(0x006, 0x0795, (i) -> i.combatData().ifPresent(c -> c.decodeSpellDamage())),
            new Patch(0x006, 0x07ca, (i) -> i.combatData().ifPresent(c -> c.monsterSpellHits()))
    );

    public static void runPatches(Interpreter i, int chunkId, int ip) {
        for (Patch patch : PATCHES) {
            if (chunkId == patch.chunkId() && ip == patch.ip())
                patch.fn().accept(i);
        }
    }

    private static void openParagraph(Interpreter i, int id) {
        final AppPreferences prefs = AppPreferences.getInstance();
        if (prefs.autoOpenParagraphsProperty().get()) {
            Platform.runLater(() -> i.app().openParagraphsWindow(id));
        }
    }

    private static void selectDamageDie(Interpreter i) {
        // BUGFIX
        // The game engine never even looks at the secondary damage die; it has no code to check if the target monster
        // group is greater than 10' away, much less any code to fetch byte 09 of the item data.
        final AppPreferences prefs = AppPreferences.getInstance();
        if (!prefs.fixSecondaryDamageProperty().get()) return;

        // Read the target group's range from their data array
        final int targetGroupId = (Heap.get(0x83).read(1)) & 0x7f;
        final int targetGroupDataPtr = i.memory().read(i.getDS(), 0x04c6 + (2 * targetGroupId), 2);
        final int targetGroupRange = i.memory().read(i.getDS(), targetGroupDataPtr + 0x09, 1);

        // Set the item offset byte of the "read damage die" instruction at {03:0d68}
        // It should be 0x08 for "primary" damage and 0x09 for "secondary" damage, but only if the target is more than
        // 10' away and the weapon even has a secondary damage die
        int itemOffset = 0x08;
        if (targetGroupRange > 1) {
            final int marchingOrder = Heap.get(Heap.SELECTED_PC).read();
            final int pcBaseAddress = Heap.get(Heap.MARCHING_ORDER + marchingOrder).read() << 8;
            final int slotId = Heap.get(Heap.SELECTED_ITEM).read();
            final int itemBaseAddress = pcBaseAddress + Memory.PC_INVENTORY + (0x17 * slotId);
            final int itemWord = i.memory().read(
                    Interpreter.PARTY_SEGMENT,
                    itemBaseAddress + 0x09,
                   i.width()
            );
            if (itemWord != 0) {
                itemOffset = 0x09;
            }
        }
        i.memory().write(i.getDS(), 0x0d69, 1, itemOffset);
    }

    private static void bugfixCastActionAvDvMod(Interpreter i) {
        // BUGFIX
        // When calculating temporary AV/DV/AC values in a combat round, the game inexplicably doesn't check for
        // spell-casting actions (0x80 | spellId) before referencing the action-to-AV array at [0966], so it pulls
        // an AV mod from way off in the middle of the code. Confirmed this in an emulator. Action 0x0f is a no-op
        // with no AV or DV mods, so it seems a safe replacement.
        final AppPreferences prefs = AppPreferences.getInstance();
        if (!prefs.casterAVBugfixProperty().get()) return;

        final int action = i.getAL();
        if ((action & 0x80) > 0) i.setAL(0x0f);
    }
}
