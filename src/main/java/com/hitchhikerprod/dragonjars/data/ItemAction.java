package com.hitchhikerprod.dragonjars.data;

import java.util.ArrayList;
import java.util.List;

public class ItemAction extends Action {
    final int itemIndex;

    public ItemAction(int itemIndex, int specialId, int eventId) {
        super(0x80, specialId, eventId);
        this.itemIndex = itemIndex;
    }

    public int getItemIndex() {
        return itemIndex;
    }

    @Override
    public String toString() {
        final List<String> fields = new ArrayList<>();
        fields.add(String.format("itemIndex=%02x", itemIndex & 0xff));
        fields.add(String.format("specialId=%02x", specialId & 0xff));
        fields.add(String.format("eventId=%02x", eventId & 0xff));
        return "Item<" + String.join(", ", fields) + ">";
    }
}
