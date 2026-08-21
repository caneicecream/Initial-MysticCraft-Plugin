package com.canopycreations.mysticcraft.lore;

/**
 * The Progenitors: three unique titles, one holder each per server, ever.
 *
 * These are the mythic figures at the root of every supernatural bloodline.
 * They are not a "rank" you grind toward - they are claimed once, by the
 * first player to meet each condition, and then they are gone. Everyone who
 * comes after inherits a world those three people shaped.
 *
 * Each progenitor is deliberately given real, asymmetric power AND a real
 * weakness, so that holding the title makes you a target as much as a
 * threat. A server where one player is untouchable isn't fun; a server
 * where three players are feared, hunted, and negotiated with is.
 */
public enum Progenitor {

    THE_IMMORTAL(
            "The Immortal",
            "§e",
            """
            §7Before there were vampires, there was a witch who wanted to never die.

            §7He convinced the greatest spellcaster of his age to craft an elixir of
            §7immortality, swearing they would drink it together and share eternity.
            §7On the day it was finished, he stole it — and drank it with the woman he
            §7actually loved.

            §7What he became was not a vampire. He does not burn in sunlight. He does
            §7not hunger for blood. He simply does not die, and he has had two thousand
            §7years to get very good at getting what he wants.

            §7The witch he betrayed spent the rest of her life ensuring he would regret it.
            """,
            "Cannot be permanently killed by stake, fire, or decapitation — only desiccation "
                    + "by a witch can put them down, and even then not forever. Immune to sunlight without "
                    + "a ring. Can read the surface thoughts of nearby players.",
            "Cannot be healed by vampire blood. Every witch alive can sense roughly where they are. "
                    + "The Cure, if it is ever brewed, works on them and only them.",
            "Akaios",
            "the Unburied",
            "Ancient Greek. From the same root as \u1f00\u03ba\u03ae\u03c1\u03b1\u03c4\u03bf\u03c2 (akeratos) - "
                    + "'undiminished, unspoiled, whole.' A name given to a child expected to be flawless. "
                    + "The irony was not lost on the woman he left at the altar."),

    THE_ORIGINAL_WITCH(
            "The Original Witch",
            "§d",
            """
            §7She did not want to make monsters. She wanted her children to survive.

            §7Her youngest was killed by wolves under a full moon, and grief made her
            §7reckless. She took the old immortality spell — the one that had already
            §7ruined one life — and she rewrote it. She drew on the sun for life and on
            §7the oldest tree in the world for permanence, and she made her family
            §7something that could not be killed by any wolf.

            §7Nature does not permit that kind of theft without collecting. For every
            §7strength she gave them, it carved out a weakness. Sunlight. Fire. A stake
            §7from the very tree she had drawn her power from. And a thirst that would
            §7make her children into exactly the thing she had been trying to protect
            §7them from.

            §7Her mentor had warned her it would unleash a plague upon the world.
            §7It did.
            """,
            "The only player who can perform the Immortality Rite (turning a player directly "
                    + "into a vampire without the death-and-transition process). Spells cost no herbs. "
                    + "Can forge Daylight Rings freely. Can strip a vampire's abilities temporarily.",
            "All her power is bound to the White Oak — if the server's White Oak is ever "
                    + "destroyed, she loses progenitor status permanently. Cannot be a vampire or werewolf.",
            "Eldr\u00fan Skugga",
            "the Ash-Mother",
            "Old Norse. Eldr (fire) + r\u00fan (secret, hidden knowledge, rune) - 'the fire-secret.' "
                    + "Skugga (shadow) was not a birth name but what the village called her afterward, "
                    + "when they realised what she had made and could not unmake. She burned her old life "
                    + "to build a new one out of the ashes, and every vampire since is what rose from it."),

    THE_FIRST_WOLF(
            "The First Wolf",
            "§6",
            """
            §7She was born too powerful, and her own people feared what she would
            §7become. Seven tribes came together and put her in the ground.

            §7They were not fast enough. With whatever she had left, she reached up and
            §7bound every one of them to the moon — not just the ones holding the knives,
            §7but their children, and their children's children, out to the last
            §7generation. Seven tribes became seven bloodlines. Seven bloodlines became
            §7every wolf that has ever turned.

            §7The curse she wrote was cruel in a specific way: it sleeps. You can carry
            §7it your whole life and never know. It only wakes when you take a life —
            §7so every wolf that has ever existed became one by killing someone first.

            §7She is the reason the moon has a hold on anyone at all.
            """,
            "Can shift at will, any night, with no full moon required and no loss of control. "
                    + "Immune to wolfsbane and silver. Their venom kills vampires in half the usual time. "
                    + "Can sense every latent carrier on the server precisely, by name.",
            "Cannot be cured or turned into anything else — permanently locked as a werewolf. "
                    + "Every vampire can sense them within 100 blocks. Takes double damage from witches.",
            "Lupercus",
            "of the Seven Knives",
            "Latin. From lupus (wolf) - the root beneath Lupercalia, the old festival where the tribes "
                    + "ran the boundaries of their land dressed in wolfskin to keep the wild out. "
                    + "They named the rite after the thing they buried. Seven knives went into her; "
                    + "seven bloodlines came back out.");

    private final String title;
    private final String colorCode;
    private final String lore;
    private final String powers;
    private final String weaknesses;
    private final String historicName;
    private final String epithet;
    private final String nameOrigin;

    Progenitor(String title, String colorCode, String lore, String powers, String weaknesses,
               String historicName, String epithet, String nameOrigin) {
        this.title = title;
        this.colorCode = colorCode;
        this.lore = lore;
        this.powers = powers;
        this.weaknesses = weaknesses;
        this.historicName = historicName;
        this.epithet = epithet;
        this.nameOrigin = nameOrigin;
    }

    public String getHistoricName() {
        return historicName;
    }

    public String getEpithet() {
        return epithet;
    }

    public String getNameOrigin() {
        return nameOrigin;
    }

    public String getFullHistoricName() {
        return colorCode + historicName + " §7" + epithet;
    }

    public String getTitle() {
        return title;
    }

    public String getColorCode() {
        return colorCode;
    }

    public String getLore() {
        return lore;
    }

    public String getPowers() {
        return powers;
    }

    public String getWeaknesses() {
        return weaknesses;
    }

    public String getFormattedTitle() {
        return colorCode + "§l" + title;
    }

    public static Progenitor fromString(String input) {
        if (input == null) return null;
        for (Progenitor p : values()) {
            if (p.name().equalsIgnoreCase(input) || p.title.equalsIgnoreCase(input)) return p;
        }
        return null;
    }
}
