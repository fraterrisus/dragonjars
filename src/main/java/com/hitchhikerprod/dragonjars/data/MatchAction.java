package com.hitchhikerprod.dragonjars.data;

import java.util.ArrayList;
import java.util.List;

public class MatchAction extends Action {
    public MatchAction(int header, int specialId, int eventId) {
        super(header, specialId, eventId);
    }

    @Override
    public String toString() {
        final List<String> fields = new ArrayList<>();
        switch (header) {
            case 0xfd -> {
                fields.add("Special");
                fields.add(String.format("specialId=%02x", specialId & 0xff));
                fields.add(String.format("eventId=%02x", eventId & 0xff));
            }
            case 0xfe -> {
                fields.add("Always");
                fields.add(String.format("eventId=%02x", eventId & 0xff));
            }
            case 0xff -> fields.add("Never");
            default -> {
                fields.add("Unknown");
                fields.add(String.format("specialId=%02x", specialId & 0xff));
                fields.add(String.format("eventId=%02x", eventId & 0xff));
            }
        }
        return "Match<" + String.join(", ", fields) + ">";
    }

}
