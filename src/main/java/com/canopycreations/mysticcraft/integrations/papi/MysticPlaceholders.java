package com.canopycreations.mysticcraft.integrations.papi;

import com.canopycreations.mysticcraft.MysticCraft;
import com.canopycreations.mysticcraft.clans.Clan;
import com.canopycreations.mysticcraft.data.PlayerData;
import com.canopycreations.mysticcraft.lore.Progenitor;
import com.canopycreations.mysticcraft.quests.Questline;
import com.canopycreations.mysticcraft.races.Race;
import com.canopycreations.mysticcraft.world.Landmark;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Exposes MysticCraft's state as placeholders.
 *
 * This is deliberately the ONLY integration written for the HUD and GUI
 * plugins. HUDEngine renders boss-bar HUDs from placeholders, CommandPanels
 * fills GUIs from placeholders, and EssentialsChat can use them for chat
 * prefixes - so one expansion serves all three rather than writing three
 * bespoke bridges that would all need maintaining separately.
 *
 * Everything degrades to a sensible neutral value rather than erroring, so a
 * HUD referencing a vampire-only placeholder on a werewolf just shows a dash
 * instead of breaking the whole bar.
 */
public class MysticPlaceholders extends PlaceholderExpansion {

    private final MysticCraft plugin;

    public MysticPlaceholders(MysticCraft plugin) {
        this.plugin = plugin;
    }

    @Override public @NotNull String getIdentifier() { return "mysticcraft"; }
    @Override public @NotNull String getAuthor()     { return "CanopyCreations"; }
    @Override public @NotNull String getVersion()    { return plugin.getDescription().getVersion(); }
    @Override public boolean persist()               { return true; }

    @Override
    public String onRequest(OfflinePlayer offline, @NotNull String params) {
        if (offline == null) return "";
        Player player = offline.getPlayer();
        if (player == null) return "";

        PlayerData data = plugin.getRaceManager().getData(player);
        Race race = data.getRace();

        return switch (params.toLowerCase()) {

            // ---------------- Race ----------------
            case "race"          -> race.getDisplayName();
            case "race_colored"  -> race.getFormattedName();
            case "race_color"    -> race.getColorCode();
            case "race_lower"    -> race.name().toLowerCase();
            case "is_vampire"    -> bool(race == Race.VAMPIRE);
            case "is_werewolf"   -> bool(race == Race.WEREWOLF);
            case "is_witch"      -> bool(race == Race.WITCH);
            case "is_human"      -> bool(race == Race.HUMAN);

            // ---------------- Vampire ----------------
            case "humanity"      -> race == Race.VAMPIRE ? String.valueOf(data.getHumanity()) : "—";
            case "humanity_bar"  -> race == Race.VAMPIRE ? bar(data.getHumanity(), 100, 10, "§4", "§8") : "";
            case "humanity_pct"  -> race == Race.VAMPIRE ? data.getHumanity() + "%" : "—";
            case "is_ripper"     -> bool(race == Race.VAMPIRE
                    && data.getHumanity() <= plugin.getConfig().getInt("vampire.humanity.low-threshold", 30));
            case "has_ring"      -> bool(data.isDaylightRingEquipped());
            case "ring_status"   -> data.isDaylightRingEquipped() ? "§6Protected" : "§4Exposed";
            case "is_transitioning" -> bool(data.isTransitioning());
            case "transition_minutes" -> {
                if (!data.isTransitioning()) yield "—";
                long ms = data.getTransitionDeadlineMillis() - System.currentTimeMillis();
                yield String.valueOf(Math.max(0, ms / 60000L));
            }
            case "is_original"   -> bool(data.isOriginalVampire());
            case "burning"       -> bool(race == Race.VAMPIRE && !data.isDaylightRingEquipped()
                    && player.getWorld().isDayTime()
                    && player.getLocation().getBlock().getLightFromSky() >= 14);

            // ---------------- Werewolf ----------------
            case "bloodline"     -> data.getBloodline() == null ? "—" : data.getBloodline().getDisplayName();
            case "bloodline_colored" -> data.getBloodline() == null ? "—" : data.getBloodline().getFormattedName();
            case "curse_triggered"   -> bool(data.hasTriggeredCurse());
            case "is_shifted"    -> bool(data.isShifted());
            case "shift_status"  -> data.isShifted() ? "§6Shifted" : "§7Human form";
            case "venom_damage"  -> data.getBloodline() == null ? "—"
                    : String.valueOf(data.getBloodline().getVenomDamagePerSecond());

            // ---------------- Moon ----------------
            case "full_moon"     -> bool(plugin.getMoonPhaseManager().isFullMoon(player.getWorld()));
            case "moon_status"   -> plugin.getMoonPhaseManager().isFullMoon(player.getWorld())
                    ? "§6§lFULL MOON" : "§7";
            case "is_night"      -> bool(!player.getWorld().isDayTime());

            // ---------------- Witch ----------------
            case "spell_cooldown" -> race == Race.WITCH
                    ? String.valueOf(plugin.getSpellManager().cooldownRemainingSeconds(data)) : "—";
            case "spell_ready"   -> bool(race == Race.WITCH
                    && plugin.getSpellManager().cooldownRemainingSeconds(data) == 0);
            case "spells_cast"   -> String.valueOf(data.getSpellsCastToday());

            // ---------------- Progenitor ----------------
            case "progenitor" -> {
                Progenitor p = plugin.getProgenitorManager().getProgenitorOf(player.getUniqueId());
                yield p == null ? "" : p.getTitle();
            }
            case "progenitor_colored" -> {
                Progenitor p = plugin.getProgenitorManager().getProgenitorOf(player.getUniqueId());
                yield p == null ? "" : p.getFormattedTitle();
            }
            case "progenitor_name" -> {
                Progenitor p = plugin.getProgenitorManager().getProgenitorOf(player.getUniqueId());
                yield p == null ? "" : p.getHistoricName();
            }
            case "is_progenitor" -> bool(plugin.getProgenitorManager().isProgenitor(player));

            // ---------------- Clan ----------------
            case "clan" -> {
                Clan c = plugin.getClanManager().getPlayerClan(player.getUniqueId());
                yield c == null ? "" : c.getName();
            }
            case "clan_colored" -> {
                Clan c = plugin.getClanManager().getPlayerClan(player.getUniqueId());
                yield c == null ? "" : c.getFormattedName();
            }
            case "clan_kind" -> {
                Clan c = plugin.getClanManager().getPlayerClan(player.getUniqueId());
                yield c == null ? "" : c.getKind().getLabel();
            }
            case "clan_members" -> {
                Clan c = plugin.getClanManager().getPlayerClan(player.getUniqueId());
                yield c == null ? "0" : String.valueOf(c.getMemberCount());
            }
            case "clan_land" -> {
                Clan c = plugin.getClanManager().getPlayerClan(player.getUniqueId());
                yield c == null ? "0" : String.valueOf(c.getClaimCount());
            }
            case "clan_rank" -> {
                Clan c = plugin.getClanManager().getPlayerClan(player.getUniqueId());
                if (c == null) yield "";
                yield c.isLeader(player.getUniqueId()) ? "Leader"
                        : c.isElder(player.getUniqueId()) ? "Elder" : "Member";
            }

            // ---------------- Territory you're standing on ----------------
            case "territory" -> {
                Clan owner = plugin.getClanManager().getChunkOwner(player.getLocation().getChunk());
                yield owner == null ? "The Wilds" : owner.getName();
            }
            case "territory_colored" -> {
                Clan owner = plugin.getClanManager().getChunkOwner(player.getLocation().getChunk());
                yield owner == null ? "§8The Wilds" : owner.getFormattedName();
            }
            case "landmark" -> {
                Landmark l = plugin.getLandmarkManager().landmarkAt(player.getLocation());
                yield l == null ? "" : l.getDisplayName();
            }
            case "landmark_colored" -> {
                Landmark l = plugin.getLandmarkManager().landmarkAt(player.getLocation());
                yield l == null ? "" : l.getFormattedName();
            }

            // ---------------- Codex & quests ----------------
            case "codex_found" -> String.valueOf(plugin.getCodexManager().discoveredCount(player));
            case "codex_total" -> String.valueOf(plugin.getCodexManager().totalCount());
            case "codex_progress" -> plugin.getCodexManager().discoveredCount(player)
                    + "/" + plugin.getCodexManager().totalCount();
            case "quest_line" -> {
                Questline q = plugin.getQuestManager().getActiveLine(player);
                yield q == null ? "" : q.getTitle();
            }
            case "quest_step" -> {
                Questline q = plugin.getQuestManager().getActiveLine(player);
                if (q == null) yield "";
                int step = plugin.getQuestManager().getStep(player, q);
                yield step >= q.getSteps().size() ? "Complete" : q.getSteps().get(step).name();
            }
            case "quest_instruction" -> {
                Questline q = plugin.getQuestManager().getActiveLine(player);
                if (q == null) yield "";
                int step = plugin.getQuestManager().getStep(player, q);
                yield step >= q.getSteps().size() ? "" : q.getSteps().get(step).instruction();
            }

            default -> null;   // null tells PlaceholderAPI the placeholder is unknown
        };
    }

    private String bool(boolean b) {
        return b ? "true" : "false";
    }

    /** A simple text bar, for HUDs that want a visual meter. */
    private String bar(int value, int max, int width, String filled, String empty) {
        int n = Math.round((value / (float) max) * width);
        StringBuilder sb = new StringBuilder(filled);
        for (int i = 0; i < width; i++) {
            if (i == n) sb.append(empty);
            sb.append('|');
        }
        return sb.toString();
    }
}
