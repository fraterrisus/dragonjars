package com.hitchhikerprod.dragonjars.data;

import java.util.ArrayList;
import java.util.List;

public class SkillAction extends Action {
    final String skillName;

    public SkillAction(int header, int specialId, int eventId) {
        super(header, specialId, eventId);
        this.skillName = Lists.REQUIREMENTS[header - 0x8c];
    }

    @Override
    public String toString() {
        final List<String> fields = new ArrayList<>();
        fields.add(skillName);
        fields.add(String.format("specialId=%02x", specialId & 0xff));
        fields.add(String.format("eventId=%02x", eventId & 0xff));
        return "Skill<" + String.join(", ", fields) + ">";
    }
}
