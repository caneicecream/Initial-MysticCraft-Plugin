package com.canopycreations.mysticcraft.quests;

import com.canopycreations.mysticcraft.races.Race;

import java.util.List;

/**
 * Questlines exist to solve a specific problem: a player who joins a
 * supernatural server and is told "you're a vampire now" has no idea what to
 * actually DO. A grind server answers that with resource goals. This answers
 * it with a story that happens to teach every mechanic on the way through.
 *
 * Each step names a real gameplay action. Completing one delivers a piece of
 * in-world narration rather than a "quest complete" popup, so the progression
 * reads as someone's story instead of a checklist.
 */
public enum Questline {

    // ------------------------------------------------------------------
    THE_NEWLY_DEAD("The Newly Dead", Race.VAMPIRE, "§4",
            "You woke up dead and hungry. Someone has to teach you the rules before you break one that matters.",
            List.of(
                    new QuestStep("Feed",
                            "Feed on a living thing. Sneak and take what you need.",
                            Objective.FEED_ONCE, 1,
                            "§7It's easier than you expected. That's the part you'll think about later."),

                    new QuestStep("Daylight",
                            "Find shelter before dawn, or find someone who can make you a ring.",
                            Objective.SURVIVE_DAWN, 1,
                            "§7You made it to shade. Somewhere out there a witch knows how to make this "
                                    + "problem go away permanently. She will want something for it."),

                    new QuestStep("Restraint",
                            "Keep your humanity above 70 for a full day.",
                            Objective.MAINTAIN_HUMANITY, 1,
                            "§7You held on. Plenty don't - and the ones who don't will tell you it's "
                                    + "freedom right up until they can't find their way back."),

                    new QuestStep("The Old Blood",
                            "Find a vampire older than you and speak with them.",
                            Objective.MEET_ELDER_VAMPIRE, 1,
                            "§7They looked at you like something they'd already read the ending of. "
                                    + "§8You are not the first of anything."),

                    new QuestStep("A Debt",
                            "Turn someone. Share your blood, and let them choose.",
                            Objective.TURN_SOMEONE, 1,
                            "§4Now you understand why the one who turned you looked the way they did. "
                                    + "§7You have made something that will outlive most of the world, "
                                    + "and it is partly yours.")
            )),

    // ------------------------------------------------------------------
    THE_WAKING("The Waking", Race.WEREWOLF, "§6",
            "You killed someone and something inside you woke up. Now you find out what you are.",
            List.of(
                    new QuestStep("Know Your Blood",
                            "Learn which of the seven lines you come from.",
                            Objective.LEARN_BLOODLINE, 1,
                            "§7Now you know whose knife it was. §8Seven tribes held her down. "
                                    + "One of them was yours."),

                    new QuestStep("The First Moon",
                            "Survive a forced transformation.",
                            Objective.SURVIVE_FULL_MOON, 1,
                            "§6You remember almost none of it. What you remember, you'd rather not. "
                                    + "§7There will be another one."),

                    new QuestStep("Chains",
                            "Survive three transformations without killing a player.",
                            Objective.MOONS_WITHOUT_KILL, 3,
                            "§7Three moons and nobody died. That's not luck - that's work, and "
                                    + "most wolves never bother to do it."),

                    new QuestStep("Pack",
                            "Join or found a pack.",
                            Objective.JOIN_PACK, 1,
                            "§6Wolves are not solitary. Whatever she did to your ancestors, she "
                                    + "made sure of that much - you are worse alone and you know it."),

                    new QuestStep("The Old Enemy",
                            "Bite a vampire and let the venom do its work.",
                            Objective.VENOM_A_VAMPIRE, 1,
                            "§6So that's what you're for. §7A thousand years of being hunted, and "
                                    + "the answer was in your teeth the whole time.")
            )),

    // ------------------------------------------------------------------
    THE_BORROWED("The Borrowed", Race.WITCH, "§5",
            "Every spell you cast is taken from something that expects to be repaid. Learn the terms.",
            List.of(
                    new QuestStep("First Working",
                            "Cast your first spell.",
                            Objective.CAST_SPELLS, 1,
                            "§5It worked, and it cost you almost nothing. §8That's how it starts."),

                    new QuestStep("The Circle",
                            "Channel alongside another witch.",
                            Objective.CHANNEL_WITH_WITCH, 1,
                            "§5You felt the other one in it with you - your limits pushed out to "
                                    + "meet theirs. §7This is why covens exist, and why everyone else "
                                    + "wants them broken up."),

                    new QuestStep("Overreach",
                            "Take backlash from overchanneling and survive it.",
                            Objective.SURVIVE_BACKLASH, 1,
                            "§4That was the balance collecting. §7You were warned. Everyone is "
                                    + "warned. It never seems to make a difference."),

                    new QuestStep("Consecrated Ground",
                            "Found or join a coven and claim territory.",
                            Objective.COVEN_TERRITORY, 1,
                            "§5Ground a coven holds pushes back against the dead. §7Now you have "
                                    + "somewhere they can't follow you."),

                    new QuestStep("The Ring",
                            "Forge a Daylight Ring for a vampire.",
                            Objective.FORGE_RING, 1,
                            "§5You just handed the sun to something that should fear it. "
                                    + "§7Whatever they promised you had better be worth it.")
            )),

    // ------------------------------------------------------------------
    THE_ONES_WHO_STAYED("The Ones Who Stayed", Race.HUMAN, "§f",
            "You have no gifts. You have daylight, numbers, and the fact that everything hunting you still needs you alive.",
            List.of(
                    new QuestStep("Notice",
                            "Discover three lore fragments.",
                            Objective.DISCOVER_FRAGMENTS, 3,
                            "§7You've started paying attention. Most people never do - it's easier "
                                    + "to decide the tracks were a bear."),

                    new QuestStep("Vervain",
                            "Obtain vervain and keep it on you.",
                            Objective.CARRY_VERVAIN, 1,
                            "§dWith that in your pocket your mind is your own. §7A vampire can look "
                                    + "you in the eye and tell you to forget, and you simply won't."),

                    new QuestStep("Armed",
                            "Obtain a stake, wolfsbane, or a silver blade.",
                            Objective.ARM_YOURSELF, 1,
                            "§7Now you can end something that has walked this world for centuries. "
                                    + "§8That is not nothing. That is the whole reason they're careful "
                                    + "around you."),

                    new QuestStep("An Order",
                            "Found or join an Order and claim ground.",
                            Objective.JOIN_ORDER, 1,
                            "§fVervain in the walls. Wolfsbane in the air. §7They can still come, "
                                    + "but it will cost them."),

                    new QuestStep("Choose",
                            "Survive thirty days as a human without being turned.",
                            Objective.SURVIVE_AS_HUMAN, 30,
                            "§fThirty days in a world that has every reason to eat you, and you're "
                                    + "still here, and still yourself. §7Some of them find that "
                                    + "genuinely unsettling.")
            ));

    /** The measurable thing a step is waiting on. Mapped to real events in QuestManager. */
    public enum Objective {
        FEED_ONCE, SURVIVE_DAWN, MAINTAIN_HUMANITY, MEET_ELDER_VAMPIRE, TURN_SOMEONE,
        LEARN_BLOODLINE, SURVIVE_FULL_MOON, MOONS_WITHOUT_KILL, JOIN_PACK, VENOM_A_VAMPIRE,
        CAST_SPELLS, CHANNEL_WITH_WITCH, SURVIVE_BACKLASH, COVEN_TERRITORY, FORGE_RING,
        DISCOVER_FRAGMENTS, CARRY_VERVAIN, ARM_YOURSELF, JOIN_ORDER, SURVIVE_AS_HUMAN
    }

    public record QuestStep(String name, String instruction, Objective objective, int required, String completionText) {}

    private final String title;
    private final Race race;
    private final String colorCode;
    private final String premise;
    private final List<QuestStep> steps;

    Questline(String title, Race race, String colorCode, String premise, List<QuestStep> steps) {
        this.title = title;
        this.race = race;
        this.colorCode = colorCode;
        this.premise = premise;
        this.steps = steps;
    }

    public String getTitle() { return title; }
    public Race getRace() { return race; }
    public String getColorCode() { return colorCode; }
    public String getPremise() { return premise; }
    public List<QuestStep> getSteps() { return steps; }
    public String getFormattedTitle() { return colorCode + title; }

    public static Questline forRace(Race race) {
        for (Questline q : values()) {
            if (q.race == race) return q;
        }
        return null;
    }
}
