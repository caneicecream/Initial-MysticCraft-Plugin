package com.canopycreations.mysticcraft.managers;

import com.canopycreations.mysticcraft.MysticCraft;
import com.canopycreations.mysticcraft.data.PlayerData;
import com.canopycreations.mysticcraft.races.Race;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

/**
 * Handles turning players into (or out of) a supernatural race, and keeps
 * their passive attributes (attack damage, movement speed) in sync with
 * their current race + state (shifted, humanity level, ring equipped, etc).
 */
public class RaceManager {

    private final MysticCraft plugin;

    public RaceManager(MysticCraft plugin) {
        this.plugin = plugin;
    }

    public Race getRace(Player player) {
        return plugin.getDataStore().get(player.getUniqueId()).getRace();
    }

    public PlayerData getData(Player player) {
        return plugin.getDataStore().get(player.getUniqueId());
    }

    /**
     * Turns a player into the given race, applying lore-appropriate onboarding
     * (e.g. vampires start at full humanity, werewolves are marked as not yet
     * having triggered the curse until their first kill).
     */
    public void setRace(Player player, Race race, boolean announce) {
        PlayerData data = getData(player);
        Race previous = data.getRace();
        data.setRace(race);

        if (race == Race.VAMPIRE) {
            data.setHumanity(plugin.getConfig().getInt("vampire.humanity.start", 100));
        }
        if (race == Race.WEREWOLF) {
            data.setTriggeredCurse(false);
            data.setShifted(false);
        }

        refreshAttributes(player);
        plugin.getDataStore().save(data);

        if (announce && plugin.getConfig().getBoolean("general.announce-turnings", true)) {
            Bukkit.broadcastMessage(race.getColorCode() + player.getName() + " §7has been turned into a "
                    + race.getFormattedName() + "§7!");
        }

        player.sendMessage("§7[§dMysticCraft§7] You are now a " + race.getFormattedName()
                + (previous != race ? " §7(was " + previous.getFormattedName() + "§7)" : ""));

        Bukkit.getPluginManager().callEvent(new com.canopycreations.mysticcraft.events.RaceChangeEvent(player, previous, race));
    }

    public boolean canSwitchRace(Player player) {
        if (!plugin.getConfig().getBoolean("general.allow-race-switching", true)) {
            return false;
        }
        PlayerData data = getData(player);
        long cooldownMillis = plugin.getConfig().getLong("general.switch-cooldown-hours", 168) * 3600_000L;
        return System.currentTimeMillis() - data.getLastRaceSwitchMillis() >= cooldownMillis;
    }

    /**
     * Reapplies movement speed / attack damage attribute modifiers based on
     * current race and state. Call this on join, race change, shift, and
     * ring equip/unequip.
     */
    public void refreshAttributes(Player player) {
        PlayerData data = getData(player);
        Race race = data.getRace();

        AttributeInstance speedAttr = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        AttributeInstance damageAttr = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);

        // Reset to vanilla baselines first
        double baseSpeed = 0.1;
        if (speedAttr != null) speedAttr.setBaseValue(baseSpeed);

        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.STRENGTH);
        player.removePotionEffect(PotionEffectType.REGENERATION);

        switch (race) {
            case VAMPIRE -> {
                int speedAmp = plugin.getConfig().getInt("vampire.speed-amplifier", 1);
                int strAmp = plugin.getConfig().getInt("vampire.strength-amplifier", 1);
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, speedAmp, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, strAmp, true, false));
            }
            case WEREWOLF -> {
                if (data.isShifted()) {
                    int speedAmp = plugin.getConfig().getInt("werewolf.speed-amplifier", 1);
                    int strAmp = plugin.getConfig().getInt("werewolf.strength-amplifier", 2);
                    int regenAmp = plugin.getConfig().getInt("werewolf.regen-amplifier", 1);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, speedAmp, true, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, strAmp, true, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, regenAmp, true, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false));
                }
                // Unshifted werewolves are mechanically human until the moon calls them.
            }
            case WITCH -> {
                // Witches get no passive combat buffs - their power is spellcasting.
            }
            case HUMAN -> {
                // No supernatural effects.
            }
        }
    }

    public void unload(Player player) {
        plugin.getDataStore().unload(player.getUniqueId());
    }
}
