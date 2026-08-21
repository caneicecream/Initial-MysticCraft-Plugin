package com.canopycreations.mysticcraft.listeners;

import com.canopycreations.mysticcraft.MysticCraft;
import com.canopycreations.mysticcraft.lore.LoreFragment;
import com.canopycreations.mysticcraft.managers.LorekeeperManager;
import com.canopycreations.mysticcraft.quests.Questline;
import com.canopycreations.mysticcraft.races.Race;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * The connective tissue: turns things players already do into codex
 * discoveries and quest progress, so neither system needs the player to
 * know it exists before it starts working on them.
 */
public class DiscoveryListener implements Listener {

    private final MysticCraft plugin;

    public DiscoveryListener(MysticCraft plugin) {
        this.plugin = plugin;
    }

    /** Right-clicking a Lorekeeper NPC. */
    @EventHandler(ignoreCancelled = true)
    public void onTalkToKeeper(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        LorekeeperManager.Role role = plugin.getLorekeeperManager().getRole(event.getRightClicked());
        if (role == null) return;

        event.setCancelled(true);
        plugin.getLorekeeperManager().speakTo(event.getPlayer(), role);
    }

    /**
     * Runs on the slow tick. Handles the passive, ambient discoveries -
     * surviving nights, standing near the White Oak, carrying vervain -
     * that shouldn't require an explicit action to notice.
     */
    public void tickAmbientDiscovery(Player player) {
        var data = plugin.getRaceManager().getData(player);
        Race race = data.getRace();

        // Everyone: seed the opening fragment.
        plugin.getCodexManager().discover(player, LoreFragment.THE_BALANCE);

        // Surviving the night out in the open.
        if (!player.getWorld().isDayTime()
                && player.getLocation().getBlock().getLightFromSky() >= 12) {
            plugin.getCodexManager().discover(player, LoreFragment.SOMETHING_IN_THE_DARK);
        }

        // Humans carrying vervain complete their protection step.
        if (race == Race.HUMAN) {
            for (var stack : player.getInventory().getContents()) {
                if (plugin.getMysticItems().hasTag(stack,
                        com.canopycreations.mysticcraft.items.MysticItems.TAG_VERVAIN)) {
                    plugin.getQuestManager().progress(player, Questline.Objective.CARRY_VERVAIN);
                    break;
                }
            }
            for (var stack : player.getInventory().getContents()) {
                if (plugin.getMysticItems().hasTag(stack,
                            com.canopycreations.mysticcraft.items.MysticItems.TAG_STAKE)
                        || plugin.getMysticItems().hasTag(stack,
                            com.canopycreations.mysticcraft.items.MysticItems.TAG_WOLFSBANE)
                        || plugin.getMysticItems().hasTag(stack,
                            com.canopycreations.mysticcraft.items.MysticItems.TAG_SILVER_WEAPON)) {
                    plugin.getQuestManager().progress(player, Questline.Objective.ARM_YOURSELF);
                    break;
                }
            }
        }

        // Vampires who make it to shade after sunrise.
        if (race == Race.VAMPIRE && player.getWorld().isDayTime()
                && player.getLocation().getBlock().getLightFromSky() < 12) {
            plugin.getQuestManager().progress(player, Questline.Objective.SURVIVE_DAWN);
        }

        // Restraint: high humanity is its own quest step.
        if (race == Race.VAMPIRE && data.getHumanity() >= 70) {
            plugin.getQuestManager().progress(player, Questline.Objective.MAINTAIN_HUMANITY);
        }

        // Werewolves who know their line.
        if (race == Race.WEREWOLF && data.getBloodline() != null) {
            plugin.getQuestManager().progress(player, Questline.Objective.LEARN_BLOODLINE);
        }

        // Standing near a White Oak (large oak / dark oak log clusters flagged by config).
        if (isNearWhiteOak(player)) {
            plugin.getCodexManager().discover(player, LoreFragment.THE_ASH_MOTHER);
        }

        // Clan-based quest steps.
        var clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan != null) {
            switch (clan.getKind()) {
                case PACK -> plugin.getQuestManager().progress(player, Questline.Objective.JOIN_PACK);
                case ORDER -> plugin.getQuestManager().progress(player, Questline.Objective.JOIN_ORDER);
                case COVEN -> {
                    if (clan.getClaimCount() > 0) {
                        plugin.getQuestManager().progress(player, Questline.Objective.COVEN_TERRITORY);
                    }
                }
                default -> { }
            }
        }

        // Being near a progenitor is its own revelation.
        for (Player other : player.getWorld().getPlayers()) {
            if (other.equals(player)) continue;
            if (other.getLocation().distance(player.getLocation()) > 20) continue;
            if (plugin.getProgenitorManager().isProgenitor(other)) {
                plugin.getCodexManager().discover(player, LoreFragment.THE_UNBURIED);
                break;
            }
        }
    }

    private boolean isNearWhiteOak(Player player) {
        if (!plugin.getConfig().getBoolean("lore.white-oak-discovery-enabled", true)) return false;
        int radius = 4;
        var base = player.getLocation().getBlock();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 4; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Material m = base.getRelative(x, y, z).getType();
                    if (m == Material.PALE_OAK_LOG || m == Material.PALE_OAK_WOOD) return true;
                }
            }
        }
        return false;
    }
}
