package com.hitchhikerprod.dragonjars.data;

import java.util.ArrayList;
import java.util.List;

public class MonsterGroup {
    private final Chunk data;
    private int offset;

    private int confidence;
    private int unknownMiddle;
    private int range;
    private int groupSize;
    private int monsterIndex;

    public MonsterGroup(Chunk data) {
        this.data = data;
    }

    public MonsterGroup decode(int offset) {
        this.offset = offset;
        final int bits = data.getUnsignedByte(offset) |
            (data.getUnsignedByte(offset + 1) << 8) |
            (data.getUnsignedByte(offset + 2) << 16);

        confidence = (bits >> 18) & 0x1f;
        if (((bits >> 24) & 0x1) > 0) { confidence *= -1; }

        unknownMiddle = (bits >> 14) & 0x0f;
        range         = (bits >> 10) & 0x0f;
        groupSize     = (bits >>  5) & 0x1f;
        monsterIndex  = (bits)       & 0x1f;

        return this;
    }

    public int getMonsterIndex() {
        return monsterIndex;
    }

    public int getOffset() {
        return offset;
    }

    @Override
    public String toString() {
        final List<String> strings = new ArrayList<>();

        strings.add(String.format("morale:0x%02x", confidence));
        strings.add(String.format("unk:0x%02x", unknownMiddle));

        if (range == 0) {
            strings.add("rng:rnd");
        } else {
            strings.add(String.format("rng:%d'", range * 10));
        }

        if (groupSize == 0) {
            strings.add("#rnd");
        } else {
            strings.add(String.format("#%d", groupSize));
        }

        if (monsterIndex == 0) {
            strings.add("monster:rnd");
        } else {
            strings.add(String.format("monster:0x%02x", monsterIndex - 1));
        }

        return "  " + String.join(", ", strings);
    }
}
