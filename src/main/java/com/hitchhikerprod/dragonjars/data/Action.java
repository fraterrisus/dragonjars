package com.hitchhikerprod.dragonjars.data;

import java.util.ArrayList;
import java.util.List;

public class Action {
    final int header;
    final int specialId;
    final int eventId;

    public Action(int header, int specialId, int eventId) {
        this.header = header;
        this.specialId = specialId;
        this.eventId = eventId;
    }

    public int header() {
        return header;
    }

    public int special() {
        return specialId;
    }

    public int event() {
        return eventId;
    }

    @Override
    public String toString() {
        final List<String> fields = new ArrayList<>();
        fields.add(String.format("header=%02x", header & 0xff));
        fields.add(String.format("specialId=%02x", specialId & 0xff));
        fields.add(String.format("eventId=%02x", eventId & 0xff));
        return "Event<" + String.join(", ", fields) + ">";
    }
}
