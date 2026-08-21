package com.canopycreations.mysticcraft.listeners;

import com.canopycreations.mysticcraft.MysticCraft;
import com.canopycreations.mysticcraft.clans.Clan;
import com.canopycreations.mysticcraft.util.Fx;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * Territory as a physical object.
 *
 * You don't claim ground by typing - you plant a totem and everyone can see
 * it standing there. It glows in your clan's colour. It marks the chunk. And
 * critically: an enemy can walk up and break it, which takes the land.
 *
 * That turns territory from a database entry into something with a location,
 * a defence problem, and a raid objective.
 */
public class TotemListener implements Listener {

    private final MysticCraft plugin;
    private final NamespacedKey key;

    public TotemListener(MysticCraft plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "clan_totem");
    }

    /** The craftable/grantable totem item. */
    public ItemStack createTotem() {
        ItemStack item = new ItemStack(Material.LODESTONE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6Clan Totem");
        meta.setLore(List.of(
                "§7Plant this to claim the ground you're standing on.",
                "§7It will be visible. It will be a target.",
                "§8Break an enemy's totem to take their land."
        ));
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "totem");
        item.setItemMeta(meta);
        return item;
    }

    private boolean isTotemItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return "totem".equals(item.getItemMeta().getPersistentDataContainer()
                .get(key, PersistentDataType.STRING));
    }

    // ------------------------------------------------------------------
    // Planting = claiming
    // ------------------------------------------------------------------
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlant(BlockPlaceEvent event) {
        if (!isTotemItem(event.getItemInHand())) return;

        Player player = event.getPlayer();
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());

        if (clan == null) {
            event.setCancelled(true);
            player.sendActionBar("§cYou have no clan to claim ground for.");
            return;
        }
        if (!clan.isElder(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendActionBar("§cOnly elders can plant a totem.");
            return;
        }

        Block block = event.getBlock();
        if (plugin.getLandmarkManager().isUnclaimable(block.getLocation())) {
            event.setCancelled(true);
            player.sendActionBar("§8This ground belongs to the town, not to you.");
            return;
        }
        Clan existing = plugin.getClanManager().getChunkOwner(block.getChunk());
        if (existing != null) {
            event.setCancelled(true);
            player.sendActionBar("§cThis ground already belongs to " + existing.getFormattedName() + "§c.");
            return;
        }

        int base = plugin.getConfig().getInt("clans.base-claim-limit", 10);
        int per = plugin.getConfig().getInt("clans.claims-per-member", 5);
        if (clan.getClaimCount() >= clan.getClaimLimit(base, per)) {
            event.setCancelled(true);
            player.sendActionBar("§cYour clan can't hold any more ground. Recruit.");
            return;
        }

        plugin.getClanManager().claimChunk(clan, block.getChunk());
        // Mark the block itself so we know which totem holds which chunk.
        block.getWorld().getBlockAt(block.getLocation()).setType(Material.LODESTONE);

        Fx.totemPlanted(block.getLocation(), clanColor(clan));

        Bukkit.broadcastMessage(clan.getFormattedName() + " §7has planted a totem and taken ground.");
        player.sendTitle("", clan.getFormattedName() + " §7claims this place", 5, 40, 10);
    }

    // ------------------------------------------------------------------
    // Breaking = taking the land
    // ------------------------------------------------------------------
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBreakTotem(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.LODESTONE) return;

        Clan owner = plugin.getClanManager().getChunkOwner(block.getChunk());
        if (owner == null) return;

        Player player = event.getPlayer();
        Clan breakerClan = plugin.getClanManager().getPlayerClan(player.getUniqueId());

        // Your own totem: you're just picking it back up.
        if (breakerClan != null && breakerClan.getName().equalsIgnoreCase(owner.getName())) {
            plugin.getClanManager().unclaimChunk(owner, block.getChunk());
            player.sendActionBar("§7You pull your totem out of the ground.");
            block.getWorld().dropItemNaturally(block.getLocation(), createTotem());
            event.setDropItems(false);
            return;
        }

        // Someone else's totem: this is a raid.
        plugin.getClanManager().unclaimChunk(owner, block.getChunk());
        event.setDropItems(false);

        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.2f, 0.6f);
        Fx.totemPlanted(block.getLocation(), Color.fromRGB(180, 30, 30));

        Bukkit.broadcastMessage("§4" + player.getName() + " §7has torn down a totem belonging to "
                + owner.getFormattedName() + "§7.");

        // Everyone in the losing clan feels it, wherever they are.
        owner.getMembers().forEach(uuid -> {
            Player member = Bukkit.getPlayer(uuid);
            if (member != null) {
                member.sendTitle("§4Ground lost", "§7Someone pulled your totem at "
                        + block.getX() + ", " + block.getZ(), 10, 60, 20);
                member.playSound(member.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.5f);
            }
        });
    }

    // ------------------------------------------------------------------
    // Ambient glow so territory is visible while you walk it
    // ------------------------------------------------------------------
    public void tickTotemAmbience() {
        for (Clan clan : plugin.getClanManager().getAllClans()) {
            Color color = clanColor(clan);
            for (String chunkKey : clan.getClaimedChunks()) {
                String[] parts = chunkKey.split(":");
                if (parts.length != 3) continue;
                var world = Bukkit.getWorld(parts[0]);
                if (world == null) continue;
                try {
                    int cx = Integer.parseInt(parts[1]);
                    int cz = Integer.parseInt(parts[2]);
                    // Only render for chunks someone is actually near.
                    if (!world.isChunkLoaded(cx, cz)) continue;
                    Location center = new Location(world, cx * 16 + 8, 0, cz * 16 + 8);
                    center.setY(world.getHighestBlockYAt(center) + 1);
                    boolean anyoneNear = world.getPlayers().stream()
                            .anyMatch(p -> p.getLocation().distance(center) < 40);
                    if (anyoneNear) Fx.totemAmbient(center, color);
                } catch (NumberFormatException ignored) {
                    // malformed chunk key, skip
                }
            }
        }
    }

    private Color clanColor(Clan clan) {
        return switch (clan.getKind()) {
            case COURT -> Color.fromRGB(150, 20, 20);
            case PACK -> Color.fromRGB(200, 140, 40);
            case COVEN -> Color.fromRGB(140, 60, 190);
            case ORDER -> Color.fromRGB(230, 230, 230);
            default -> Color.fromRGB(120, 120, 120);
        };
    }
}
