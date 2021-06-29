package fr.openent.competences.enums;

public enum LevelCycle {

    CYCLE3(3),
    CYCLE4(4);

    private final Integer value;

    LevelCycle (Integer s) {
        value = s;
    }

    public Integer getValue() {
        return value;
    }


}
