package com.canopycreations.mysticcraft.listeners;

import com.canopycreations.mysticcraft.MysticCraft;
import com.canopycreations.mysticcraft.data.PlayerData;
import com.canopycreations.mysticcraft.items.MysticItems;
import com.canopycreations.mysticcraft.races.Race;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WerewolfListener implements Listener {

    private final MysticCraft plugin;

    /** Tracks players currently suffering werewolf bite toxin: uuid -> expiry (millis). */
    private final Map<UUID, Long> toxinExpiry = new HashMap<>();
    /** Which bloodline's venom is in them - determines damage profile. */
    private final Map<UUID, com.canopycreations.mysticcraft.lore.Bloodline> toxinSource = new HashMap<>();

    public WerewolfListener(MysticCraft plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onWolfsbaneHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        PlayerData data = plugin.getRaceManager().getData(victim);
        if (data.getRace() != Race.WEREWOLF) return;

        ItemStack weapon = null;
        if (event.getDamager() instanceof LivingEntity damager && damager.getEquipment() != null) {
            weapon = damager.getEquipment().getItemInMainHand();
        }
        if (weapon == null || !plugin.getMysticItems().hasTag(weapon, MysticItems.TAG_WOLFSBANE)) return;

        double dmg = plugin.getConfig().getDouble("werewolf.wolfsbane-damage", 8.0);
        int weaken = plugin.getConfig().getInt("werewolf.wolfsbane-weaken-seconds", 15);
        event.setDamage(dmg);
        victim.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, weaken * 20, 2));
        victim.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, weaken * 20, 1));
        victim.sendMessage("§2Wolfsbane courses through you, burning like acid.");
    }

    /** A shifted werewolf's melee attacks inject toxin - lethal to vampires over time. */
    @EventHandler(ignoreCancelled = true)
    public void onBite(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        PlayerData attackerData = plugin.getRaceManager().getData(attacker);
        PlayerData victimData = plugin.getRaceManager().getData(victim);

        if (attackerData.getRace() != Race.WEREWOLF || !attackerData.isShifted()) return;
        if (victimData.getRace() != Race.VAMPIRE) return;

        com.canopycreations.mysticcraft.lore.Bloodline line = attackerData.getBloodline();
        int durationSeconds = line != null
                ? line.getVenomDurationSeconds()
                : plugin.getConfig().getInt("werewolf.bite-toxin-duration-seconds", 60);

        toxinExpiry.put(victim.getUniqueId(), System.currentTimeMillis() + durationSeconds * 1000L);
        if (line != null) toxinSource.put(victim.getUniqueId(), line);
        victim.sendMessage("§6You've been bitten! The venom is already spreading - find a werewolf's blood to cure it.");
        plugin.getQuestManager().progress(attacker, com.canopycreations.mysticcraft.quests.Questline.Objective.VENOM_A_VAMPIRE);
        com.canopycreations.mysticcraft.util.Fx.venomBite(attacker, victim);
        if (line != null) {
            victim.sendMessage("§8The fever has a particular character to it. Someone who knows the bloodlines might recognise it.");
        }
    }

    /** Called once per second by MysticCraft's tick task. */
    public void tickToxin(Player player) {
        Long expiry = toxinExpiry.get(player.getUniqueId());
        if (expiry == null) return;

        if (System.currentTimeMillis() > expiry) {
            toxinExpiry.remove(player.getUniqueId());
            toxinSource.remove(player.getUniqueId());
            player.sendMessage("§6The werewolf venom has run its course... you survived.");
            plugin.getCodexManager().discover(player, com.canopycreations.mysticcraft.lore.LoreFragment.THE_VENOM);
            return;
        }

        com.canopycreations.mysticcraft.lore.Bloodline line = toxinSource.get(player.getUniqueId());
        double dmg = line != null
                ? line.getVenomDamagePerSecond()
                : plugin.getConfig().getDouble("werewolf.bite-toxin-vampire-damage-per-second", 3.0);
        player.damage(dmg);
        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 40, 0));
        player.sendActionBar("§6Werewolf venom burns in your veins...");
        com.canopycreations.mysticcraft.util.Fx.venomTick(player);
    }

    /** A werewolf's blood cures the vampire bite toxin - drink via /vampire feed on a werewolf, or admin cure. */
    public void cureToxin(Player player) {
        toxinSource.remove(player.getUniqueId());
        if (toxinExpiry.remove(player.getUniqueId()) != null) {
            player.sendMessage("§aThe werewolf blood burns through your system - the venom is cured.");
        }
    }

    public boolean isPoisoned(Player player) {
        return toxinExpiry.containsKey(player.getUniqueId());
    }
}
