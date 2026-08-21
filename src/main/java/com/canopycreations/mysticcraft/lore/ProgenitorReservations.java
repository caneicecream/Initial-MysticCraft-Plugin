package com.canopycreations.mysticcraft.lore;

import com.canopycreations.mysticcraft.MysticCraft;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * Ties specific players to specific Progenitor titles.
 *
 * The three founding names of this server are written into the mythology
 * itself: when the reserved player claims their title, they aren't just
 * taking a role - the server recognises them as that historic figure
 * returned, and announces it that way. Anyone else who somehow meets the
 * condition first is blocked, so the story can't be taken from them by a
 * lucky stranger on week one.
 *
 * Configured under `progenitors.reserved` in config.yml. Leave a title
 * unreserved (blank) and it becomes first-come, first-served as normal.
 */
public class ProgenitorReservations {

    private final MysticCraft plugin;

    public ProgenitorReservations(MysticCraft plugin) {
        this.plugin = plugin;
    }

    /** Returns the username reserved for this title, or null if it's open to anyone. */
    public String getReservedFor(Progenitor progenitor) {
        String name = plugin.getConfig().getString("progenitors.reserved." + progenitor.name(), "");
        return (name == null || name.isBlank()) ? null : name;
    }

    /** True if this player is the one the title is written for (or it's unreserved). */
    public boolean mayClaim(Player player, Progenitor progenitor) {
        String reserved = getReservedFor(progenitor);
        if (reserved == null) return true;
        return reserved.equalsIgnoreCase(player.getName());
    }

    /** True if this specific player is the named heir to this title. */
    public boolean isNamedHeir(Player player, Progenitor progenitor) {
        String reserved = getReservedFor(progenitor);
        return reserved != null && reserved.equalsIgnoreCase(player.getName());
    }

    /**
     * The recognition text shown when a named heir claims their title -
     * heavier than a normal claim, because this is the moment the server's
     * founding mythology attaches to a real person.
     */
    public String[] recognitionLines(Player player, Progenitor progenitor) {
        return new String[]{
                "",
                "§8§m                                                        ",
                "§7The name comes back to you before you have time to wonder why.",
                "",
                "  " + progenitor.getFullHistoricName(),
                "",
                "§8" + progenitor.getNameOrigin(),
                "",
                "§7You are " + progenitor.getFormattedTitle() + "§7, and you always were.",
                "§8§m                                                        ",
                ""
        };
    }

    /** Message shown to someone who meets the condition but isn't the named heir. */
    public String blockedMessage(Progenitor progenitor) {
        return "§8You feel something vast turn over in its sleep, recognise you as a stranger, "
                + "and settle again. §7" + progenitor.getFormattedTitle() + " §8is not yours to become.";
    }

    /** Human-readable summary for /lore and admin output. */
    public String describeReservation(Progenitor progenitor) {
        String reserved = getReservedFor(progenitor);
        return reserved == null
                ? "§8unclaimed — open to whoever earns it"
                : "§8awaiting §7" + reserved;
    }
}
