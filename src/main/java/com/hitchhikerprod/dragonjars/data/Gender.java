package com.hitchhikerprod.dragonjars.data;

import java.util.Arrays;
import java.util.Optional;

public enum Gender {
    MALE(0, "he/him"),
    FEMALE(1, "she/her"),
    NEUTRAL(2, "it"),
    NONE(3, "-");

    final int index;
    final String pronouns;

    Gender(int index, String pronouns) {
        this.index = index;
        this.pronouns = pronouns;
    }

    public String getPronouns() {
        return pronouns;
    }

    public static Optional<Gender> of(int index) {
        return Arrays.stream(Gender.values()).filter(g -> g.index == index).findFirst();
    }
}
