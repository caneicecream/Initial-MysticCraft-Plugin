package com.canopycreations.mysticcraft.world;

import com.canopycreations.mysticcraft.races.Race;

/**
 * The town of Ashfall.
 *
 * A deliberate parallel to the small-town setting of the source material
 * rather than a copy of it - the names are ours, and each one is tied to
 * the Ash-Mother's story so the geography and the mythology explain each
 * other. (Named for her: she burned her old life down here, and the town
 * grew up in the ash.)
 *
 * The point of this system is NOT decoration. Every landmark does something
 * mechanically - it's a spawn seat for a clan, a place a ritual only works,
 * a thing that can be destroyed to strip a progenitor of their power. A
 * pretty building nobody has a reason to visit is scenery; a building that
 * changes what you can do is a map.
 */
public enum Landmark {

    TOWN_SQUARE("Ashfall Square", "§f", null,
            "The middle of everything. A clock that stopped a long time ago and nobody has fixed.",
            """
            §7Every town this size has one. A patch of grass, a war memorial
            §7nobody reads, and a clock tower that stopped at 3:47 on a night
            §7the records are strangely quiet about.

            §8Neutral ground by old agreement. Everyone breaks it eventually.
            """,
            Role.NEUTRAL_GROUND),

    THE_KETTLE("The Kettle", "§6", null,
            "The only bar in town. Everyone drinks here, which is exactly the problem.",
            """
            §7Wood floors, bad lighting, a menu that hasn't changed in thirty
            §7years. The owner doesn't ask questions and doesn't want answers.

            §7You will end up sitting three feet from something that would kill
            §7you if the room were emptier. So will everyone else. That's the
            §7arrangement.

            §8No violence inside. It has held for a century. Mostly.
            """,
            Role.SANCTUARY),

    THE_BOARDING_HOUSE("The Old Boarding House", "§4", Race.VAMPIRE,
            "Big, dark, and older than the town. The vampires have held it for generations.",
            """
            §7Built in 1861 by a family that stopped aging and never explained
            §7it. The deed has changed hands eleven times and always to someone
            §7with the same handwriting.

            §7The cellar goes down further than the foundation should allow.

            §8Vampire ground. A Court founded here holds it as its seat.
            """,
            Role.COURT_SEAT),

    LOCKRIDGE_MANOR("Lockridge Manor", "§6", Race.WEREWOLF,
            "The oldest family in the county, and the reason the woods aren't safe.",
            """
            §7They've been here since before the town had a name. Respectable.
            §7Civic-minded. Mayors, three generations running.

            §7There are chains bolted into the bedrock under the east wing and
            §7the family has never once explained them to a contractor.

            §8Werewolf ground. A Pack founded here holds it as its seat.
            """,
            Role.PACK_SEAT),

    THE_HEDGE_HOUSE("The Hedge House", "§5", Race.WITCH,
            "A small house at the end of a road that isn't on any map.",
            """
            §7Herbs drying in every window. Salt across the thresholds. A garden
            §7growing things that shouldn't survive this far north.

            §7The women who live here have lived here a very long time, and the
            §7town has learned not to bother them about it.

            §8Witch ground. Spells cast here cost fewer herbs.
            """,
            Role.COVEN_SEAT),

    THE_BURNED_CHURCH("St. Ansel's Ruin", "§8", null,
            "They burned witches here. The stone remembers.",
            """
            §7What's left is a foundation, part of a wall, and a bell that fell
            §7through the floor and was never dug out.

            §7A hundred people were locked inside and the doors were held shut
            §7from outside. The town has never put up a marker. Ask anyone over
            §7sixty and they'll change the subject.

            §5Witch magic runs stronger on this ground. It is not a gift.
            §8Something is still down there, underneath.
            """,
            Role.CONSECRATED),

    THE_TOMB("The Tomb", "§8", null,
            "Beneath the church. Sealed for a reason that was written down and then destroyed.",
            """
            §7A witch sealed it. Whatever's inside can't leave, and for a long
            §7time nobody could get in either.

            §7The seal is old now. Old things loosen.

            §8Something in here has been waiting a very long time to be let out.
            """,
            Role.SEALED),

    WICKER_BRIDGE("Wicker Bridge", "§b", null,
            "More people have drowned here than the river accounts for.",
            """
            §7Wooden, narrow, and structurally fine according to every survey
            §7the county has ever run.

            §7Cars go off it anyway. Always at night. Always with more people in
            §7the water than got in the car.

            §8Running water. Some old things don't cross it easily.
            """,
            Role.WARDED),

    THE_WHITE_OAK("The White Oak", "§f", null,
            "A pale tree older than the story about it. The Ash-Mother drew on it.",
            """
            §7It shouldn't still be alive. It shouldn't have been alive then.

            §7A witch stood under it a thousand years ago with a dead child and
            §7a spell she had no business rewriting, and she took permanence out
            §7of this trunk to give to her family.

            §7Every vampire that exists is downstream of what happened here.

            §4Wood cut from this tree kills them permanently.
            §c§lIf this tree is destroyed, The Original Witch loses her power.
            """,
            Role.WHITE_OAK),

    THE_QUARRY("The Quarry", "§7", null,
            "Flooded, fenced off, and where the town puts things it wants to stop thinking about.",
            """
            §7Stopped operating in the fifties. Filled with groundwater within a
            §7year. The fence went up after the third body.

            §8Deep, cold, and nobody looks too hard at what surfaces.
            """,
            Role.NEUTRAL_GROUND),

    THE_OLD_CEMETERY("The Old Cemetery", "§8", null,
            "Half the headstones have the same six surnames. Some of those people are still walking around.",
            """
            §7Founders' plots at the top of the hill, everyone else below.

            §7Read the dates carefully. There are families here who buried a
            §7son in 1864 and a grandson in 1901 who look identical in the
            §7photographs.

            §5A witch drawing on the dead finds them close to hand here.
            """,
            Role.CONSECRATED);

    /** What a landmark actually DOES. This is the part that matters. */
    public enum Role {
        NEUTRAL_GROUND,  // no clan may claim this chunk
        SANCTUARY,       // PvP disabled inside
        COURT_SEAT,      // vampire clan seat; bonus on home ground
        PACK_SEAT,       // werewolf clan seat
        COVEN_SEAT,      // witch clan seat; cheaper spells
        CONSECRATED,     // witch power amplified, vampires suppressed
        SEALED,          // locked content - opens via server event
        WARDED,          // running water; vampires slowed crossing
        WHITE_OAK        // destroying it strips The Original Witch
    }

    private final String displayName;
    private final String colorCode;
    private final Race affinity;
    private final String shortDescription;
    private final String lore;
    private final Role role;

    Landmark(String displayName, String colorCode, Race affinity,
             String shortDescription, String lore, Role role) {
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.affinity = affinity;
        this.shortDescription = shortDescription;
        this.lore = lore;
        this.role = role;
    }

    public String getDisplayName() { return displayName; }
    public String getColorCode() { return colorCode; }
    public Race getAffinity() { return affinity; }
    public String getShortDescription() { return shortDescription; }
    public String getLore() { return lore; }
    public Role getRole() { return role; }
    public String getFormattedName() { return colorCode + displayName; }

    public static Landmark fromString(String s) {
        if (s == null) return null;
        for (Landmark l : values()) {
            if (l.name().equalsIgnoreCase(s) || l.displayName.equalsIgnoreCase(s)) return l;
        }
        return null;
    }
}
