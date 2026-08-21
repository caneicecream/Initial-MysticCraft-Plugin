package com.canopycreations.mysticcraft.lore;

/**
 * The Seven Werewolf Bloodlines.
 *
 * LORE: Roughly two thousand years ago, a witch of extraordinary power was
 * murdered by the very tribes that feared her. With her dying breath she
 * bound her killers - and all their descendants - to the moon itself. Every
 * witch present at her death became the first of a bloodline, and their
 * children's children inherited the curse whether they wanted it or not.
 *
 * Each bloodline carries its own distinct strain of venom. All seven are
 * lethal to vampires, but they differ in how they kill: some burn fast and
 * hot, others linger for days. A vampire who knows their bloodlines can
 * sometimes tell who bit them from how the fever takes hold.
 *
 * Mechanically, a werewolf's bloodline determines their venom's damage
 * profile and gives packs a real reason to care about ancestry.
 */
public enum Bloodline {

    CRESCENT("Crescent",
            "The oldest and proudest of the packs, said to have warred with the North Atlantic "
                    + "since the night the curse was cast. Crescent wolves run in tight family units and "
                    + "remember every debt.",
            3.5, 55, "§6"),

    NORTH_ATLANTIC("North Atlantic",
            "Sea-cold and patient. They crossed water when the other packs would not, and their "
                    + "venom works slow - a bite from a North Atlantic wolf can take days to finish what "
                    + "it started.",
            2.0, 110, "§3"),

    HOLLOW_BORN("Hollow-Born",
            "Descendants of those closest to the witch when she died. Their venom is the most "
                    + "virulent of all seven, but the bloodline is thin and few remain who can prove the "
                    + "lineage.",
            5.0, 40, "§5"),

    ASHWOOD("Ashwood",
            "Forest-dwellers who learned to survive the change by chaining themselves beneath the "
                    + "roots of old trees. Their venom carries a searing heat.",
            4.0, 50, "§2"),

    STONE_RIDGE("Stone Ridge",
            "Mountain wolves. Hard, territorial, and the most likely to shelter humans during a "
                    + "full moon rather than risk killing them.",
            2.5, 75, "§7"),

    RIVERBORNE("Riverborne",
            "They followed the waterways and scattered widest of all seven. The most common "
                    + "bloodline - and the most likely to appear in someone who never suspected it.",
            3.0, 65, "§b"),

    EMBERFELL("Emberfell",
            "Nearly wiped out in the old wars with the vampires. What's left of them is vengeful, "
                    + "and their venom burns hottest and fastest of any surviving line.",
            6.0, 30, "§c");

    private final String displayName;
    private final String lore;
    private final double venomDamagePerSecond;
    private final int venomDurationSeconds;
    private final String colorCode;

    Bloodline(String displayName, String lore, double venomDamagePerSecond, int venomDurationSeconds, String colorCode) {
        this.displayName = displayName;
        this.lore = lore;
        this.venomDamagePerSecond = venomDamagePerSecond;
        this.venomDurationSeconds = venomDurationSeconds;
        this.colorCode = colorCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getLore() {
        return lore;
    }

    public double getVenomDamagePerSecond() {
        return venomDamagePerSecond;
    }

    public int getVenomDurationSeconds() {
        return venomDurationSeconds;
    }

    public String getColorCode() {
        return colorCode;
    }

    public String getFormattedName() {
        return colorCode + displayName;
    }

    public static Bloodline fromString(String input) {
        if (input == null) return null;
        for (Bloodline b : values()) {
            if (b.name().equalsIgnoreCase(input) || b.displayName.equalsIgnoreCase(input)) return b;
        }
        return null;
    }
}
