package com.hitchhikerprod.dragonjars.exec;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.Item;
import com.hitchhikerprod.dragonjars.data.Lists;
import com.hitchhikerprod.dragonjars.data.StringDecoder;
import com.hitchhikerprod.dragonjars.data.WeaponDamage;
import com.hitchhikerprod.dragonjars.exec.instructions.DecodeStringFrom;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CombatData {
    private static final int MONSTER_HP = 0x0278;
    private static final int MONSTER_INIT = 0x02dc;
    private static final int MONSTER_ACTION = 0x030e;
    private static final int MONSTER_AV = 0x0340;
    private static final int MONSTER_DV = 0x0372;
    private static final int MONSTER_GROUP_ID = 0x03a4;

    private static final int GROUP_DATA_POINTERS = 0x04c6;
    private static final int GROUP_DEX = 0x01;
    private static final int GROUP_AV = 0x06;
    private static final int GROUP_DIST = 0x09;
    private static final int GROUP_SIZE = 0x0a;
    private static final int GROUP_FLAGS = 0x0e; // 0x80: is undead
    private static final int GROUP_DISARM = 0x25; // 0: immune to Disarm
    private static final int GROUP_SPEED = 0x21;
    private static final int GROUP_AV_MOD = 0x27;
    private static final int GROUP_DV_MOD = 0x28;
    private static final int GROUP_NAME = 0x29;

    private static final int PC_ACTION = 0x04ce;
    private static final int PC_TARGET = 0x04d5;
    private static final int PC_WEAPON = 0x04dc;
    private static final int PC_SPELL = 0x04e3;
    private static final int PC_INIT = 0x04ea;

    private Interpreter i;

    public CombatData(Interpreter i) {
        this.i = i;
    }

    private enum WhoseTurn { IDLE, PARTY, ENEMIES }

    private record Opponent(int groupId, int hp, int status, int initiative) { }

    private StringBuffer sb;

    private WhoseTurn whoseTurn = WhoseTurn.IDLE;
    private int pcId;
    private int pcBaseAddress;
    private String pcName;

    private int monsterId;
    private int monsterGroupId;
    private int monsterBaseAddress;
    private int confidence;
    private int bravery;

    public void turnDone() {
        System.out.println(sb.toString());
        whoseTurn = WhoseTurn.IDLE;
    }

    public void partyTurn() {
        if (whoseTurn != WhoseTurn.IDLE) System.err.println("Turn is not IDLE");
        whoseTurn = WhoseTurn.PARTY;
        sb = new StringBuffer();

        pcId = Heap.get(Heap.SELECTED_PC).read();
        pcBaseAddress = Heap.get(Heap.MARCHING_ORDER + pcId).read() << 8;

        final Address pcNameAddress = new Address(Interpreter.PARTY_SEGMENT, pcBaseAddress);
        pcName = StringDecoder.decodeString(i.memory().readList(pcNameAddress, 12).stream()
                .filter(b -> b != 0).map(b -> (int)b).toList());
        sb.append(pcName);

        final int pcStatus = i.memory().read(Interpreter.PARTY_SEGMENT, pcBaseAddress + Memory.PC_STATUS, 1);
        if ((pcStatus & 0x81) > 0) {
            sb.append(" is incapacitated");
            return;
        }

        final int combatCodeSegment = i.memory().lookupChunkId(0x03);
        final int action = i.memory().read(combatCodeSegment, 0x04ce + pcId, 1);
        sb.append(" ").append(decodePartyAction(action));
    }

    public void partyAdvances() {
        sb.append(" advances the party 10'");
    }

    public void partyFlees() {
        sb.append(" flees");
    }

    public void partyEquip() {
        final int slotId = Heap.get(Heap.SELECTED_ITEM).read();
        final int itemAddress = pcBaseAddress + Memory.PC_INVENTORY + (0x17 * slotId);
        final Item item = new Item(i.memory().getSegment(Interpreter.PARTY_SEGMENT)).decode(itemAddress);
        sb.append(" equips the ").append(item.getName());
    }

    public void partyMove(int targetSlot) {
        sb.append(" moves to slot ").append(targetSlot);
    }

    public void partySpell() {
        final boolean isItem = Heap.get(0x89).read() == 0;
        if (isItem) {
            final int slotId = Heap.get(Heap.SELECTED_ITEM).read();
            final int itemAddress = pcBaseAddress + Memory.PC_INVENTORY + (0x17 * slotId);
            final Item item = new Item(i.memory().getSegment(Interpreter.PARTY_SEGMENT)).decode(itemAddress);
            sb.append(" the ").append(item.getName()).append(" to cast");
        }
        final int spellId = Heap.get(0x85).read();
        final String spellName = Lists.SPELL_NAMES[spellId / 0x08][spellId % 0x08];
        final int power = Heap.get(0x86).read(2);
        sb.append(" ").append(spellName).append(" (").append(power).append("pow)");
    }

    public void partyHeal() {
        // the selected PC is currently the target, not the caster
        final int pcBaseAddress = Heap.getPCBaseAddress();
        final Address pcNameAddress = new Address(Interpreter.PARTY_SEGMENT, pcBaseAddress);
        final String pcName = StringDecoder.decodeString(i.memory().readList(pcNameAddress, 12).stream()
                .filter(b -> b != 0).map(b -> (int)b).toList());

        final int spellId = Heap.get(0x85).read();
        final int spellCodeSegment = i.memory().lookupChunkId(0x06);
        final WeaponDamage healDie = new WeaponDamage((byte)i.memory().read(spellCodeSegment, 0x032d + spellId, 1));
        final int amount = Heap.get(0x5d).read(2);

        sb.append(String.format("\n... heals %s for %s = %d", pcName, healDie, amount));
    }

    public void partySpellDamage() {
        final boolean variablePower = Heap.get(0x47).read() != 0;
        final int powerCost = Heap.get(0x86).read(2);
        final WeaponDamage damageDie = new WeaponDamage((byte)Heap.get(0x7c).read(1));
        final int totalDamage = Heap.get(0x61).read(2);
        sb.append("\n... for ");
        if (variablePower) sb.append(powerCost).append(" x ");
        sb.append(damageDie).append(" = ").append(totalDamage).append(" damage each");
    }

    public void partySpellTarget() {
        final int combatCodeSegment = i.memory().lookupChunkId(0x03);

        final int targetId = Heap.get(0x84).read();
        final int targetGroupId = Heap.get(0x48).read();
        final int targetBaseAddress = i.memory().read(combatCodeSegment, GROUP_DATA_POINTERS + (2 * targetGroupId), 2);
        i.stringDecoder().decodeString(i.memory().getSegment(combatCodeSegment), targetBaseAddress + GROUP_NAME);
        final String targetName = StringDecoder.decodeString(
                DecodeStringFrom.pluralize(i.stringDecoder().getDecodedChars(), false));

        sb.append(String.format("\n... %s(gp%d,id%d)", targetName, targetGroupId + 1, targetId));

        final int attackerInt = i.memory().read(Interpreter.PARTY_SEGMENT, pcBaseAddress + Memory.PC_INT_CURRENT, 1);
        final int magicSkill = Heap.get(0x79).read();
        final int defenderDV = Heap.get(0x7a).read();
        final int attackRoll = Heap.get(0x7b).read();

        sb.append(String.format(" DV%+d target=%d {vs} 1d16", defenderDV, 6 + defenderDV));

        if (attackRoll == 0xff) { // we forced this to detect crit hits/misses
            sb.append(i.getCarryFlag() ? "=1" : "=16");
            sb.append(", automatic ");
        } else {
            final int bonus = (attackerInt / 4) + magicSkill;
            sb.append(String.format("%+d=%d (IQ%+d Sk%+d), ",
                    bonus, 19 + bonus - attackRoll, attackerInt / 4, magicSkill));
        }
        sb.append(i.getCarryFlag() ? "miss (half-damage)" : "hit");
    }

    public void partyAttackTarget() {
        final int combatCodeSegment = i.memory().lookupChunkId(0x03);

        mightyDamage = 0;
        bonusDamage = 0;

        final int targetId = Heap.get(0x84).read();
        final int targetGroupId = i.memory().read(combatCodeSegment, MONSTER_GROUP_ID + targetId, 1);
        final int targetBaseAddress = i.memory().read(combatCodeSegment, GROUP_DATA_POINTERS + (2 * targetGroupId), 2);
        i.stringDecoder().decodeString(i.memory().getSegment(combatCodeSegment), targetBaseAddress + GROUP_NAME);
        final String targetName = StringDecoder.decodeString(
                DecodeStringFrom.pluralize(i.stringDecoder().getDecodedChars(), false));

        sb.append(String.format("\n... %s(gp%d", targetName, targetGroupId + 1));

        if (i.getAL() == 0) {
            sb.append("), no targets remaining");
        } else if (i.getCarryFlag()) {
            sb.append("), out of range");
        } else {
            final int defenderDV = i.memory().read(combatCodeSegment, MONSTER_DV + targetId, 1);
            sb.append(String.format(",id%d) DV%+d target=%d", targetId, defenderDV, 6 + defenderDV));
        }
    }

    public void partyAttackBlocked() {
        sb.append(", blocked");
    }

    public void partyAttackHits() {
        sb.append("\n  ... 1d16");

        final int attackerAV = i.memory().read(Interpreter.PARTY_SEGMENT, pcBaseAddress + Memory.PC_AV, 1);
        final int weaponSkill = Heap.get(0x79).read();
        final int attackRoll = Heap.get(0x7b).read();

        if (attackRoll == 0xff) { // we forced this to detect crit hits/misses
            sb.append(i.getCarryFlag() ? "=1" : "=16");
            sb.append(", automatic ");
        } else {
            final int bonus = attackerAV + weaponSkill;
            sb.append(String.format("%+d=%d (AV%+d Sk%+d), ",
                    bonus, 19 + bonus - attackRoll, attackerAV, weaponSkill));
        }
        sb.append(i.getCarryFlag() ? "miss" : "hit");
    }

    private int mightyDamage;
    private int bonusDamage;

    public void partyDamageMighty(int mightyDamage) {
        this.mightyDamage = mightyDamage;
    }

    public void partyDamageBonus(int bonusDamage) {
        this.bonusDamage = bonusDamage;
    }

    public void partyDamage() {
        final int numHits = Heap.get(0x7e).read();
        final WeaponDamage damageDie = new WeaponDamage((byte)Heap.get(0x7c).read());
        final int totalDamage = Heap.get(0x5d).read(2);

        sb.append("\n  ... ");
        sb.append(numHits).append(" hit");
        if (numHits > 1) sb.append("s");
        sb.append(" for ").append(damageDie);
        if (mightyDamage > 0) sb.append(" +1d4 (mighty)");
        if (bonusDamage > 0) sb.append(" +").append(bonusDamage).append(" (attr)");
        sb.append(" = ").append(totalDamage);
    }

    public void monsterTurn() {
        if (whoseTurn != WhoseTurn.IDLE) System.err.println("Turn is not IDLE");
        whoseTurn = WhoseTurn.ENEMIES;
        sb = new StringBuffer();

        monsterId = Heap.get(0x77).read();
        monsterGroupId = Heap.get(0x78).read();

        final int combatCodeSegment = i.memory().lookupChunkId(0x03);
        monsterBaseAddress = i.memory().read(combatCodeSegment, GROUP_DATA_POINTERS + (2 * monsterGroupId), 2);
//        final int groupSize = i.memory().read(combatCodeSegment, monsterBaseAddress + GROUP_SIZE, 1);
        i.stringDecoder().decodeString(i.memory().getSegment(combatCodeSegment), monsterBaseAddress + GROUP_NAME);
        final String monsterName = StringDecoder.decodeString(
                DecodeStringFrom.pluralize(i.stringDecoder().getDecodedChars(), false));
        sb.append(String.format("%s (%d,%d)", monsterName, monsterGroupId, monsterId));

        final int partySize = Heap.get(Heap.PARTY_SIZE).read();
        if (partySize == 0) {
            sb.append(" has nothing to do");
        }
    }

    public void monsterConfidence(int conf) {
        if (whoseTurn != WhoseTurn.ENEMIES) System.err.println("Turn is not ENEMIES");
        this.confidence = conf;
    }

    public void monsterBravery(int bravery) {
        if (whoseTurn != WhoseTurn.ENEMIES) System.err.println("Turn is not ENEMIES");
        this.bravery = bravery;
    }

    public void monsterAction(int action) {
        if (whoseTurn != WhoseTurn.ENEMIES) System.err.println("Turn is not ENEMIES");
        // System.out.print(" " + decodeMonsterAction(action));
    }

    public void monsterFlees(boolean successfully) {
        final int chance = Heap.get(0x45).read(1);
        final int odds;
        if (chance == 0xff) odds = 100;
        else odds = 100 - chance;
        sb.append(" tries to flee (").append(odds).append("%) and ");
        if (successfully) sb.append("succeeds");
        else sb.append("fails");
    }

    private String decodePartyAction(int action) {
        if ((action & 0x80) > 0) return "casts";
        return switch (action) {
            case 0 -> "attacks";
            case 1 -> "mighty attacks";
            case 2 -> "disarms";
            case 4 -> "equips";
            case 5 -> "blocks";
            case 6 -> "dodges";
            case 8 -> "moves ahead";
            case 9 -> "moves behind";
            case 10 -> "flees";
            case 11 -> "passes";
            case 12 -> "uses";
            case 13 -> "advances the party";
            default -> "does something unexpected";
        };
    }

    private String decodeMonsterBravery() {
        return switch (bravery) {
            case 0xc0 -> "HALP";
            case 0x80 -> "Edgy";
            case 0x40 -> "Okay";
            default -> "Good";
        };
    }

    private String decodeMonsterAction(int action) {
        return switch(action) {
            case 0,1,2,3,4 -> "attacks";
            case 5 -> "blocks";
            case 6 -> "dodges";
            case 7 -> "flees";
            case 8 -> "casts";
            case 9 -> "breathes";
            case 10 -> "calls for help";
            default -> "passes";
        };
    }

    public void getCombatants() {
        final int combatSegmentId = i.getSegmentForChunk(0x03, Frob.IN_USE);
        final Chunk monsterData = i.memory().getSegment(combatSegmentId);
        System.out.println("Live enemies:");
        for (int groupId = 0; groupId < 4; groupId++) {
            final int groupOffset = monsterData.read(GROUP_DATA_POINTERS + (2 * groupId), 2);
            final int groupSize = monsterData.read(groupOffset + GROUP_SIZE, 1);
            if (groupSize == 0) continue;

            i.stringDecoder().decodeString(monsterData, groupOffset + GROUP_NAME);
            final String groupName = StringDecoder.decodeString(
                    DecodeStringFrom.pluralize(i.stringDecoder().getDecodedChars(), groupSize > 1));

            final int groupDistance = monsterData.read(groupOffset + GROUP_DIST, 1) * 10;
            final int groupDEX = monsterData.read(groupOffset + GROUP_DEX, 1);
            final int groupAV = monsterData.read(groupOffset + GROUP_AV, 1);
            final int groupAVmod = monsterData.read(groupOffset + GROUP_AV_MOD, 1);
            final int groupDVmod = monsterData.read(groupOffset + GROUP_DV_MOD, 1);
            final int groupSpd = Integer.max(1, monsterData.read(groupOffset + GROUP_SPEED, 1)) * 10;

            System.out.format("  %d %s (%02d',%02d'): AV%d DV%d",
                    groupSize, groupName, groupDistance, groupSpd,
                    (groupDEX / 4) + groupAV + groupAVmod, (groupDEX / 4) + groupDVmod);
            if ((0x08 & monsterData.read(groupOffset + GROUP_FLAGS, 1)) > 0) { System.out.print(", undead"); }
            if (monsterData.read(groupOffset + GROUP_DISARM, 1) == 0) { System.out.print(", can't be disarmed"); }

            final List<Opponent> groupMembers = new ArrayList<>();
            int monsterId = 0;
            while (monsterId < 50 && groupMembers.size() < groupSize) {
                final int group = i.memory().read(combatSegmentId, MONSTER_GROUP_ID + monsterId, 1);
                if (group == groupId) {
                    final int hp = i.memory().read(combatSegmentId, MONSTER_HP + (2 * monsterId), 2);
                    final int status = i.memory().read(combatSegmentId, MONSTER_ACTION + monsterId, 1);
                    groupMembers.add(new Opponent(group, hp, status, 0));
                }
                monsterId++;
            }

            System.out.print("    HP: ");
            System.out.println(groupMembers.stream()
                    .map(opp -> String.format("%d%s", opp.hp(), (opp.status() & 0x80) > 0 ? "'" : ""))
                    .collect(Collectors.joining(", ")));
        }
    }

//    private void decodeMonsterAction() {
//        final int combatSegmentId = getSegmentForChunk(0x03, Frob.IN_USE);
//        System.out.println(switch (getAL()) {
//            case 0,1,2,3,4 -> { // 1:no armor, 2:stun only, 3:1/4, 4:health only
//                final int params = memory().read(combatSegmentId, Heap.get(0x68).read(2) + 1, 2);
//                final int attPerRnd = 1 + memory().read(combatSegmentId, Heap.get(0x41).read(2) + 0x26, 1);
//                final WeaponDamage dmg = new WeaponDamage((byte)(params & 0xff));
//                final int range = Integer.max(1, (params & 0xf000) >> 12) * 10;
//                yield("attacks(" + attPerRnd + "," + dmg + "," + range + "')");
//            }
//            case 5 -> "blocks";
//            case 6 -> "dodges";
//            case 7 -> "flees";
//            case 8 -> "casts";
//            case 9 -> {
//                final int params = memory().read(combatSegmentId, Heap.get(0x68).read(2) + 1, 2);
//                final WeaponDamage dmg = new WeaponDamage((byte)(params & 0xff));
//                final int range = Integer.max(1, (params & 0xf000) >> 12) * 10;
//                yield("breathes(" + dmg + "," + range + "')");
//            }
//            case 10 -> "calls for help";
//            default -> "passes";
//        });
//    }

    private void decodeInitiative() {
        final int combatSegmentId = i.getSegmentForChunk(0x03, Frob.IN_USE);
        final Chunk monsterData = i.memory().getSegment(combatSegmentId);
        final Chunk partyData = i.memory().getSegment(Interpreter.PARTY_SEGMENT);

        final List<Opponent> opponents = new ArrayList<>();
        final List<String> groupNames = new ArrayList<>();
        for (int groupId = 0; groupId < 4; groupId++) {
            final int groupOffset = monsterData.read(GROUP_DATA_POINTERS + (2 * groupId), 2);
            final int groupSize = monsterData.read(groupOffset + GROUP_SIZE, 1);
            if (groupSize == 0) {
                groupNames.add(null);
                continue;
            }

            i.stringDecoder().decodeString(monsterData, groupOffset + GROUP_NAME);
            final String groupName = StringDecoder.decodeString(
                    DecodeStringFrom.pluralize(i.stringDecoder().getDecodedChars(), groupSize > 1));

            groupNames.add(groupName);

            int monsterId = 0;
            int groupMembers = 0;
            while (monsterId < 50 && groupMembers < groupSize) {
                final int group = i.memory().read(combatSegmentId, 0x03a4 + monsterId, 1);
                if (group == groupId) {
                    final int hp = i.memory().read(combatSegmentId, 0x0278 + (2 * monsterId), 2);
                    final int status = i.memory().read(combatSegmentId, 0x030e + monsterId, 1);
                    final int initiative = i.memory().read(combatSegmentId, 0x02dc + monsterId, 1);
                    opponents.add(new Opponent(group, hp, status, initiative));
                    groupMembers++;
                }
                monsterId++;
            }
        }

        final List<Integer> partyInitiatives = new ArrayList<>();
        for (int pcid = 0; pcid < Heap.get(Heap.PARTY_SIZE).read(); pcid++) {
            partyInitiatives.add(i.memory().read(combatSegmentId, PC_INIT + pcid, 1));
        }

        final int maxInit = Integer.max(
                opponents.stream().map(Opponent::initiative).reduce(Integer::max).orElse(0),
                partyInitiatives.stream().max(Integer::compareTo).orElse(0)
        );
        System.out.println("Initiative order:");
        for (int init = maxInit; init > 0; init--) {
            final int finalInit = init;
            final List<String> combatants = new ArrayList<>();
            opponents.stream()
                    .filter(opp -> opp.initiative() == finalInit)
                    .map(opp -> String.format("%s(%d,0x%02x)", groupNames.get(opp.groupId()), opp.hp(), opp.status()))
                    .forEach(combatants::add);
            for (int i = 0; i < partyInitiatives.size(); i++) {
                if (partyInitiatives.get(i) != init) continue;
                final int partyMemberOffset = Heap.get(Heap.MARCHING_ORDER + i).read() << 8;
                final String name = StringDecoder.decodeString(
                        partyData.getBytes(partyMemberOffset, 12).stream()
                                .filter(b -> b != 0).map(b -> (int)b).toList());
                combatants.add(name);
            }
            if (!combatants.isEmpty()) System.out.println("  " + init + ": " + String.join(", ", combatants));
        }
    }}
