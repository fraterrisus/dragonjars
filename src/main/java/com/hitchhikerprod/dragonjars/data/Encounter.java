package com.hitchhikerprod.dragonjars.data;

import java.util.ArrayList;
import java.util.List;

public class Encounter {
    private final Chunk data;

    private int offset;
    private List<MonsterGroup> groups;
    private int taglineIdx;
    private String tagline;

    public Encounter(Chunk data) {
        this.data = data;
    }

    public Encounter decode(int offset) {
        this.offset = offset;

        final int b0 = data.getUnsignedByte(offset);
        final int numGroups = ((b0 & 0x00c0) >> 6) + 1;
        taglineIdx = b0 & 0x003f;

        this.groups = new ArrayList<>();
        for (int i = 0; i < numGroups; i++) {
            final int groupOffset = offset + 1 + (i * 3);
            groups.add(new MonsterGroup(data).decode(groupOffset));
        }

        return this;
    }

    public List<MonsterGroup> getGroups() {
        return groups;
    }

    public int getOffset() {
        return offset;
    }

    public int getTaglineIndex() {
        return taglineIdx;
    }

    public void setTagline(String tagline) { this.tagline = tagline; }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("  ").append(tagline).append("\n");
        for (MonsterGroup mg : groups) {
            sb.append("    ").append(mg).append("\n");
        }
        return sb.toString();
    }
}
