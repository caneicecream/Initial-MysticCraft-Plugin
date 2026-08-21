package com.canopycreations.mysticcraft.lore;

/**
 * Lore fragments are the discoverable unit of the codex.
 *
 * DESIGN: /lore starts almost entirely locked. Pages fill in as players
 * actually do things - survive a full moon, get bitten, find a buried
 * journal, talk to someone who remembers. Reading the mythology becomes a
 * reward for playing rather than a wall of text on day one, and players end
 * up holding *different* pieces, which gives them something real to trade.
 *
 * Each fragment names how it's found, so the discovery hooks live next to
 * the text they unlock.
 */
public enum LoreFragment {

    // ---- Universal / early -------------------------------------------------
    THE_BALANCE("On the Balance", Trigger.FIRST_JOIN,
            """
            §7There were rules once. Not laws - laws are written by people, and
            §7people can argue with them. Rules. The kind the world keeps whether
            §7you agree or not.

            §7Nothing lives forever. Nothing comes back. What dies stays dead.

            §7Every single thing you are about to meet exists because somebody
            §7decided those rules were negotiable.
            """),

    SOMETHING_IN_THE_DARK("Something in the Dark", Trigger.NIGHT_SURVIVED,
            """
            §7You start to notice it after a few nights out here.

            §7Not the mobs - everyone knows about the mobs. Something else. Tracks
            §7that are too big and the wrong shape. Cattle opened up and not eaten.
            §7People in the villages who won't meet your eye after dark and won't
            §7say why.

            §8Someone in this world knows what's out there. Find them.
            """),

    // ---- Vampire track -----------------------------------------------------
    THE_THIRST("The Thirst", Trigger.WITNESSED_FEEDING,
            """
            §7You saw it happen. Someone you'd have sworn was a person, with their
            §7mouth at someone else's throat, and the look on their face afterward
            §7was not shame. It was §orelief§7.

            §7They are not sick. They are not cursed in the way the stories say.
            §7They are §fdead§7, and something is running them anyway, and the only
            §7thing that quiets it is blood.
            """),

    THE_SUN_AND_THE_RING("The Sun and the Ring", Trigger.SAW_VAMPIRE_BURN,
            """
            §7Daylight does not kill them quickly. That's the part the stories get
            §7wrong. It takes its time.

            §7Which is why the rings exist. A witch can bind a piece of the old
            §7protection into metal - lapis and silver and a name spoken over it -
            §7and a vampire who wears one can walk at noon like anyone else.

            §8There are only so many. Most were made for people who are long dead.
            §8Ask a witch what she'd want for one.
            """),

    THE_SWITCH("The Humanity Switch", Trigger.HUMANITY_DROPPED,
            """
            §7Here is the thing nobody warns you about.

            §7A vampire feels everything a person feels, except more - grief,
            §7guilt, love, all of it turned up until it's unbearable. And they can
            §7turn it §ooff§7. Flip it like a light. The guilt stops.

            §7So does everything else.

            §8Coming back on is harder. Some of them never manage it.
            """),

    // ---- Werewolf track ----------------------------------------------------
    THE_SLEEPING_CURSE("The Sleeping Curse", Trigger.NEAR_TRANSFORMATION,
            """
            §7You were close enough to hear the bones.

            §7That's the part that stays with people - not the size of it, not the
            §7teeth. The sound a person makes turning into something that isn't a
            §7person, and the fact that they were screaming the whole time.

            §7Nobody chooses it. That's the cruelty. The curse sleeps in the blood
            §7for generations and only wakes when its carrier takes a life.

            §8Which means every wolf that has ever existed killed someone first.
            """),

    SEVEN_KNIVES("Seven Knives", Trigger.FOUND_WOLF_SHRINE,
            """
            §7Scratched into the stone here, in a hand that was shaking:

            §8  "seven of us held her down
            §8   seven of us are holding her still
            §8   she said the moon would remember our names
            §8   she said our children would remember
            §8   she was right about both"

            §7Below it, seven marks. One of them has been scratched out so hard
            §7the stone is gouged through.
            """),

    THE_VENOM("On Venom", Trigger.SURVIVED_WOLF_BITE,
            """
            §7A wolf's bite kills vampires. Not quickly, and not cleanly.

            §7Each of the seven lines carries its own strain, and they don't kill
            §7the same way. Some burn through in half a minute. Others take days,
            §7and the dying know exactly how long they have the whole time.

            §8A vampire who has studied the bloodlines can sometimes name their
            §8killer from the fever alone. Small comfort.
            """),

    // ---- Witch track -------------------------------------------------------
    THE_PRICE("What Magic Costs", Trigger.FIRST_SPELL_CAST,
            """
            §7Magic is not free and it is not yours.

            §7You are borrowing - from the earth, from the dead, from whatever is
            §7still listening. Small workings, the world doesn't notice. Take more
            §7than you're owed and the balance comes out of you instead.

            §8Every witch learns this. Most of them learn it the hard way.
            """),

    CHANNELING("On Channeling", Trigger.CHANNELED_WITH_WITCH,
            """
            §7Alone, a witch is limited by her own body. Together, they aren't.

            §7Standing in a circle, drawing through each other, a coven can do
            §7things no single caster could survive attempting. This is why covens
            §7exist, and why the other races are so interested in breaking them up.

            §8Push it too far and it doesn't fail gracefully. It comes back through
            §8the circle.
            """),

    THE_ASH_MOTHER("The Ash-Mother", Trigger.FOUND_WHITE_OAK,
            """
            §7There is a tree older than the story about it.

            §7A witch came here with a dead child and a spell she had no business
            §7rewriting. She took the sun for life and this tree for permanence,
            §7and she made her family into something that could not be killed by
            §7any wolf.

            §7Nature settled the account immediately. For every strength: a
            §7weakness. Sunlight. Fire. A stake cut from this very trunk.

            §8Her mentor told her it would unleash a plague on the world.
            §8It did. You're standing in it.
            """),

    // ---- Deep lore / rare --------------------------------------------------
    THE_UNBURIED("The Unburied", Trigger.MET_PROGENITOR,
            """
            §7There is one older than the vampires. Older than the wolves.

            §7He was a witch who wanted to never die, and he was clever enough to
            §7talk the finest spellcaster of the age into brewing it for him - as a
            §7wedding gift, for the two of them, to share.

            §7He drank it with someone else.

            §7What he is now isn't a vampire. Sun doesn't touch him. Blood doesn't
            §7interest him. He has had two thousand years of practice at getting
            §7what he wants and no reason at all to hurry.

            §8Three times someone has managed to put him in the ground.
            §8Three times the ground gave him back.
            """),

    THE_CURE("On the Cure", Trigger.CODEX_MOSTLY_COMPLETE,
            """
            §7She made one. That's the part almost nobody knows.

            §7When she understood what he'd done, she didn't just imprison him -
            §7she brewed a cure and left it where he could see it. One dose.
            §7Take it and be mortal and die like everything else, or stay what
            §7you are forever and rot.

            §7He chose to rot.

            §8The cure was never used. Which means, in principle, it is still
            §8somewhere. People have spent lifetimes on less.
            """);

    /** How a fragment gets discovered. The listener layer maps these to real events. */
    public enum Trigger {
        FIRST_JOIN,
        NIGHT_SURVIVED,
        WITNESSED_FEEDING,
        SAW_VAMPIRE_BURN,
        HUMANITY_DROPPED,
        NEAR_TRANSFORMATION,
        FOUND_WOLF_SHRINE,
        SURVIVED_WOLF_BITE,
        FIRST_SPELL_CAST,
        CHANNELED_WITH_WITCH,
        FOUND_WHITE_OAK,
        MET_PROGENITOR,
        CODEX_MOSTLY_COMPLETE
    }

    private final String title;
    private final Trigger trigger;
    private final String text;

    LoreFragment(String title, Trigger trigger, String text) {
        this.title = title;
        this.trigger = trigger;
        this.text = text;
    }

    public String getTitle() { return title; }
    public Trigger getTrigger() { return trigger; }
    public String getText() { return text; }

    public static LoreFragment fromString(String s) {
        if (s == null) return null;
        for (LoreFragment f : values()) {
            if (f.name().equalsIgnoreCase(s) || f.title.equalsIgnoreCase(s)) return f;
        }
        return null;
    }
}
