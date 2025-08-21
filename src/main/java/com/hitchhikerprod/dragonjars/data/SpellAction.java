package com.hitchhikerprod.dragonjars.data;

import java.util.ArrayList;
import java.util.List;

public class SpellAction extends Action {
    final String spellName;

    public SpellAction(int header, int specialId, int eventId) {
        super(header, specialId, eventId);
        this.spellName = Lists.SPELL_NAMES[header / 8][header % 8];
    }

    @Override
    public String toString() {
        final List<String> fields = new ArrayList<>();
        fields.add(spellName);
        fields.add(String.format("specialId=%02x", specialId & 0xff));
        fields.add(String.format("eventId=%02x", eventId & 0xff));
        return "Spell<" + String.join(", ", fields) + ">";
    }
}
