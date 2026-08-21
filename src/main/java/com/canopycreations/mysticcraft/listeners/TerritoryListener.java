package com.canopycreations.mysticcraft.listeners;

import com.canopycreations.mysticcraft.MysticCraft;
import com.canopycreations.mysticcraft.clans.Clan;
import com.canopycreations.mysticcraft.data.PlayerData;
import com.canopycreations.mysticcraft.races.Race;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Territory protection plus - and this is the whole reason for building the
 * clan system natively rather than bridging to an external factions plugin -
 * race-flavored territory effects.
 *
 * Ground isn't just "claimed" or "unclaimed" here. A Coven's land actively
 * suppresses vampires. A Pack's land is hostile to anyone who isn't kin,
 * and dangerous on a full moon. A Court's land drains the living. That
 * turns every territory war into a race war with real tactical texture.
 */
public class TerritoryListener implements Listener {

    private final MysticCraft plugin;
    private final Map<UUID, String> lastChunk = new HashMap<>();

    public TerritoryListener(MysticCraft plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------
    // Territory entry announcements
    // ------------------------------------------------------------------
    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        Chunk to = event.getTo().getChunk();
        Chunk from = event.getFrom().getChunk();
        if (to.getX() == from.getX() && to.getZ() == from.getZ()
                && to.getWorld().equals(from.getWorld())) return;

        Player player = event.getPlayer();
        Clan owner = plugin.getClanManager().getChunkOwner(to);
        String key = owner == null ? "wilds" : owner.getName();

        if (key.equals(lastChunk.get(player.getUniqueId()))) return;
        lastChunk.put(player.getUniqueId(), key);

        if (owner == null) {
            player.sendActionBar("§8The Wilds §7— unclaimed");
        } else {
            Clan playerClan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
            boolean own = playerClan != null && playerClan.getName().equalsIgnoreCase(owner.getName());
            String relation = own ? "§ayour ground"
                    : owner.isEnemy(playerClan == null ? "" : playerClan.getName()) ? "§chostile"
                    : owner.isAlly(playerClan == null ? "" : playerClan.getName()) ? "§aallied"
                    : "§7neutral";
            player.sendActionBar(owner.getFormattedName() + " §8" + owner.getKind().getLabel() + " §7— " + relation);
        }
    }

    // ------------------------------------------------------------------
    // Build protection
    // ------------------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!canBuild(event.getPlayer(), event.getBlock().getChunk())) {
            event.setCancelled(true);
            event.getPlayer().sendActionBar("§cThis ground isn't yours.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!canBuild(event.getPlayer(), event.getBlock().getChunk())) {
            event.setCancelled(true);
            event.getPlayer().sendActionBar("§cThis ground isn't yours.");
        }
    }

    private boolean canBuild(Player player, Chunk chunk) {
        if (player.hasPermission("mysticcraft.admin")) return true;
        Clan owner = plugin.getClanManager().getChunkOwner(chunk);
        if (owner == null) return true;
        Clan playerClan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (playerClan == null) return false;
        if (playerClan.getName().equalsIgnoreCase(owner.getName())) return true;
        return owner.isAlly(playerClan.getName());
    }

    // ------------------------------------------------------------------
    // Race-flavored territory effects - called every 5s from the tick loop
    // ------------------------------------------------------------------
    public void tickTerritoryEffects() {
        if (!plugin.getConfig().getBoolean("clans.territory-effects-enabled", true)) return;

        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            Clan owner = plugin.getClanManager().getChunkOwner(player.getLocation().getChunk());
            if (owner == null) continue;

            Clan playerClan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
            boolean friendly = playerClan != null
                    && (playerClan.getName().equalsIgnoreCase(owner.getName())
                        || owner.isAlly(playerClan.getName()));
            if (friendly) {
                applyHomeGroundBonus(player, owner);
                continue;
            }

            applyHostileGroundPenalty(player, owner);
        }
    }

    /** Standing on your own clan's ground gives a small edge - worth defending. */
    private void applyHomeGroundBonus(Player player, Clan owner) {
        PlayerData data = plugin.getRaceManager().getData(player);
        switch (owner.getKind()) {
            case COVEN -> {
                if (data.getRace() == Race.WITCH) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 120, 0, true, false));
                }
            }
            case PACK -> {
                if (data.getRace() == Race.WEREWOLF) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 120, 0, true, false));
                }
            }
            case COURT -> {
                if (data.getRace() == Race.VAMPIRE) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 120, 0, true, false));
                }
            }
            case ORDER -> {
                if (data.getRace() == Race.HUMAN) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 120, 0, true, false));
                }
            }
            default -> { /* MIXED clans get no racial home bonus */ }
        }
    }

    /** Enemy ground pushes back based on what kind of clan holds it. */
    private void applyHostileGroundPenalty(Player player, Clan owner) {
        PlayerData data = plugin.getRaceManager().getData(player);

        switch (owner.getKind()) {
            case COVEN -> {
                // Consecrated ground - witches suppress the undead here.
                if (data.getRace() == Race.VAMPIRE) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 120, 0, true, false));
                    player.sendActionBar("§5Consecrated ground — your strength is being drawn out of you.");
                }
            }
            case PACK -> {
                // Wolves mark their territory; vampires feel it, and it worsens under a full moon.
                if (data.getRace() == Race.VAMPIRE) {
                    boolean fullMoon = plugin.getMoonPhaseManager().isFullMoon(player.getWorld());
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 120, fullMoon ? 1 : 0, true, false));
                    player.sendActionBar(fullMoon
                            ? "§6The wolves are close and the moon is full. Leave."
                            : "§6You are being watched. This is pack ground.");
                }
            }
            case COURT -> {
                // A vampire court's ground is exsanguinating to the living.
                if (data.getRace() == Race.HUMAN || data.getRace() == Race.WITCH) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 120, 0, true, false));
                    player.sendActionBar("§4Something here is hungry.");
                }
            }
            case ORDER -> {
                // Human strongholds are laced with vervain and wolfsbane.
                if (data.getRace() == Race.VAMPIRE) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 120, 0, true, false));
                    player.sendActionBar("§fVervain in the air. This place was built against you.");
                } else if (data.getRace() == Race.WEREWOLF) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 120, 0, true, false));
                    player.sendActionBar("§fWolfsbane. They knew you'd come.");
                }
            }
            default -> { /* MIXED clans hold neutral ground */ }
        }
    }

    // ------------------------------------------------------------------
    // Clan PvP protection (own clan + allies)
    // ------------------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClanPvp(EntityDamageByEntityEvent event) {
        if (!plugin.getConfig().getBoolean("clans.friendly-fire-disabled", true)) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        if (plugin.getLandmarkManager().isSanctuary(victim.getLocation())) {
            event.setCancelled(true);
            if (event.getDamager() instanceof Player a) {
                a.sendActionBar("§6Not in here. Nobody swings first on neutral ground.");
            }
            return;
        }

        Clan vClan = plugin.getClanManager().getPlayerClan(victim.getUniqueId());
        Clan aClan = plugin.getClanManager().getPlayerClan(attacker.getUniqueId());
        if (vClan == null || aClan == null) return;

        if (vClan.getName().equalsIgnoreCase(aClan.getName()) || vClan.isAlly(aClan.getName())) {
            event.setCancelled(true);
            attacker.sendActionBar("§7You can't strike your own.");
        }
    }

    public void clearCache(UUID uuid) {
        lastChunk.remove(uuid);
    }
}
