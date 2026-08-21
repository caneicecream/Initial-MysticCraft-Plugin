package com.canopycreations.mysticcraft.listeners;

import com.canopycreations.mysticcraft.MysticCraft;
import com.canopycreations.mysticcraft.data.PlayerData;
import com.canopycreations.mysticcraft.items.MysticItems;
import com.canopycreations.mysticcraft.races.Race;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class VampireListener implements Listener {

    private final MysticCraft plugin;

    public VampireListener(MysticCraft plugin) {
        this.plugin = plugin;
    }

    /** Called every second by MysticCraft's tick task for all online vampires. */
    public void tickSunlight(Player player, PlayerData data) {
        if (data.getRace() != Race.VAMPIRE) return;
        if (data.isDaylightRingEquipped()) return;

        boolean daytime = player.getWorld().isDayTime();
        boolean exposed = player.getLocation().getBlock().getLightFromSky() >= 14 && player.getWorld().getEnvironment() == org.bukkit.World.Environment.NORMAL;

        if (daytime && exposed) {
            double damage = plugin.getConfig().getDouble("vampire.sunlight-damage-per-second", 4.0);
            player.damage(damage);
            com.canopycreations.mysticcraft.util.Fx.sunlightBurn(player);
            player.sendActionBar("§4You're burning in the sunlight! Find shade or wear your ring.");
            plugin.getCodexManager().discover(player, com.canopycreations.mysticcraft.lore.LoreFragment.THE_SUN_AND_THE_RING);
            for (Player near : player.getWorld().getPlayers()) {
                if (!near.equals(player) && near.getLocation().distance(player.getLocation()) <= 16) {
                    plugin.getCodexManager().discover(near, com.canopycreations.mysticcraft.lore.LoreFragment.THE_SUN_AND_THE_RING);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVervainOrStakeOrFire(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        PlayerData data = plugin.getRaceManager().getData(victim);
        MysticItems items = plugin.getMysticItems();

        ItemStack weapon = null;
        if (event.getDamager() instanceof LivingEntity damager) {
            weapon = damager.getEquipment() != null ? damager.getEquipment().getItemInMainHand() : null;
        }
        if (weapon == null) return;

        if (data.getRace() == Race.VAMPIRE) {
            if (items.hasTag(weapon, MysticItems.TAG_STAKE) && plugin.getConfig().getBoolean("vampire.stake-instakill", true)) {
                victim.setHealth(0);
                victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PLAYER_DEATH, 1f, 0.7f);
                victim.sendMessage("§4A stake through the heart. It's over.");
                return;
            }
            if (plugin.getConfig().getBoolean("vampire.decapitation-instakill", true)
                    && weapon.getType().name().endsWith("_AXE")) {
                victim.setHealth(0);
                victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PLAYER_DEATH, 1f, 0.5f);
                victim.sendMessage("§4Decapitation. There's no coming back from that.");
                return;
            }
            if (items.hasTag(weapon, MysticItems.TAG_VERVAIN)) {
                double dmg = plugin.getConfig().getDouble("vampire.vervain-damage", 6.0);
                int weaken = plugin.getConfig().getInt("vampire.vervain-weaken-seconds", 8);
                event.setDamage(dmg);
                victim.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, weaken * 20, 2));
                victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, weaken * 20, 2));
                victim.sendMessage("§dVervain burns through your veins!");
            }
        }

        if (data.getRace() == Race.WEREWOLF) {
            if (items.hasTag(weapon, MysticItems.TAG_SILVER_WEAPON)) {
                double bonus = plugin.getConfig().getDouble("werewolf.silver-bonus-damage", 6.0);
                event.setDamage(event.getDamage() + bonus);
                victim.sendMessage("§fSilver sears your flesh!");
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFireDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        PlayerData data = plugin.getRaceManager().getData(victim);
        if (data.getRace() != Race.VAMPIRE) return;
        if (!plugin.getConfig().getBoolean("vampire.fire-instakill", true)) return;

        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.FIRE || cause == EntityDamageEvent.DamageCause.FIRE_TICK
                || cause == EntityDamageEvent.DamageCause.LAVA) {
            victim.setHealth(0);
            victim.sendMessage("§4Fire consumes you completely. Vampires do not survive the flame.");
        }
    }

    /** Sneak + right-click another living entity with an empty hand to feed. */
    @EventHandler(ignoreCancelled = true)
    public void onFeed(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player vampire = event.getPlayer();
        PlayerData data = plugin.getRaceManager().getData(vampire);
        if (data.getRace() != Race.VAMPIRE) return;
        if (!vampire.isSneaking()) return;
        if (vampire.getInventory().getItemInMainHand().getType() != Material.AIR) return;
        if (!(event.getRightClicked() instanceof LivingEntity victim)) return;
        if (victim.getLocation().distance(vampire.getLocation()) > 3) return;

        event.setCancelled(true);

        // Both parties sneaking + target is a human player = blood-sharing ritual (turning), not feeding.
        if (victim instanceof Player targetPlayer && targetPlayer.isSneaking()
                && plugin.getRaceManager().getRace(targetPlayer) == Race.HUMAN) {
            shareBlood(vampire, targetPlayer);
            return;
        }

        double healHearts = plugin.getConfig().getDouble("vampire.blood-heal-hearts", 4.0);
        int humanityCost = plugin.getConfig().getInt("vampire.blood-humanity-cost", 2);

        double maxHealth = vampire.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        vampire.setHealth(Math.min(maxHealth, vampire.getHealth() + healHearts * 2));
        com.canopycreations.mysticcraft.util.Fx.feeding(vampire, victim.getLocation());
        vampire.sendMessage("§4You feed, and feel the strength return to you.");
        plugin.getQuestManager().progress(vampire, com.canopycreations.mysticcraft.quests.Questline.Objective.FEED_ONCE);
        plugin.getCodexManager().discover(vampire, com.canopycreations.mysticcraft.lore.LoreFragment.THE_THIRST);

        if (victim instanceof Player victimPlayer) {
            victimPlayer.damage(2.0);
            victimPlayer.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 0));
            victimPlayer.sendMessage("§4Something just fed on you...");
            plugin.getCodexManager().discover(victimPlayer, com.canopycreations.mysticcraft.lore.LoreFragment.THE_THIRST);

            // If this vampire is mid-transition, feeding on a human completes it.
            if (data.isTransitioning() && plugin.getRaceManager().getRace(victimPlayer) == Race.HUMAN) {
                completeTransition(vampire, data);
            }
        } else {
            victim.damage(2.0);
        }

        if (!data.isTransitioning()) {
            plugin.getHumanityManager().adjustHumanity(vampire, -humanityCost);
        }
    }

    private void shareBlood(Player vampire, Player human) {
        int windowMinutes = plugin.getConfig().getInt("vampire.turning.blood-window-minutes", 60);
        PlayerData humanData = plugin.getRaceManager().getData(human);
        humanData.setHasVampireBloodInSystem(true);
        humanData.setVampireBloodExpiresAtMillis(System.currentTimeMillis() + windowMinutes * 60_000L);
        plugin.getDataStore().save(humanData);

        com.canopycreations.mysticcraft.util.Fx.bloodShared(vampire, human);
        vampire.sendMessage("§4You share your blood. Their fate is their own now.");
        plugin.getQuestManager().progress(vampire, com.canopycreations.mysticcraft.quests.Questline.Objective.TURN_SOMEONE);
        human.sendMessage("§4§lYou taste blood not your own.");
        human.sendMessage("§4A strange power lingers in your veins. If you die in the next " + windowMinutes
                + " minutes, you won't stay dead - but you'll have to fight to survive what comes after.");
    }

    private void completeTransition(Player vampire, PlayerData data) {
        data.setTransitioning(false);
        int startHumanity = plugin.getConfig().getInt("vampire.humanity.start", 100);
        data.setHumanity(startHumanity);
        plugin.getDataStore().save(data);

        com.canopycreations.mysticcraft.util.Fx.transitionComplete(vampire);
        vampire.sendTitle("§4§lThe transition is complete", "§7You are a vampire now, fully and truly.", 10, 60, 20);
        org.bukkit.Bukkit.broadcastMessage("§4" + vampire.getName() + " §7has completed the turning. A new vampire walks among us.");

        plugin.getOriginalsManager().tryClaimOriginal(vampire);
    }
}
