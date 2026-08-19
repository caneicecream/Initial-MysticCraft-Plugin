package com.canopycreations.mysticcraft.races;

/**
 * The supernatural races available on MysticCraft, modeled after
 * The Vampire Diaries mythos.
 */
public enum Race {
    HUMAN("Human", "§f"),
    VAMPIRE("Vampire", "§4"),
    WEREWOLF("Werewolf", "§6"),
    WITCH("Witch", "§5");

    private final String displayName;
    private final String colorCode;

    Race(String displayName, String colorCode) {
        this.displayName = displayName;
        this.colorCode = colorCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorCode() {
        return colorCode;
    }

    public String getFormattedName() {
        return colorCode + displayName;
    }

    public static Race fromString(String input) {
        for (Race race : values()) {
            if (race.name().equalsIgnoreCase(input) || race.displayName.equalsIgnoreCase(input)) {
                return race;
            }
        }
        return null;
    }
}
