package com.canopycreations.mysticcraft.managers;

import com.canopycreations.mysticcraft.MysticCraft;
import com.canopycreations.mysticcraft.data.PlayerData;
import com.canopycreations.mysticcraft.races.Race;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

/**
 * The humanity meter is TVD's core vampire mechanic: high humanity keeps a
 * vampire's emotions (and morality) in check; low humanity flips the
 * "humanity switch" toward a ripper state - stronger, but with a chance of
 * uncontrollable rage.
 */
public class HumanityManager {

    private final MysticCraft plugin;
    private final Random random = new Random();

    public HumanityManager(MysticCraft plugin) {
        this.plugin = plugin;
    }

    public void adjustHumanity(Player player, int delta) {
        PlayerData data = plugin.getRaceManager().getData(player);
        if (data.getRace() != Race.VAMPIRE) return;

        int before = data.getHumanity();
        data.setHumanity(before + delta);
        int after = data.getHumanity();

        int lowThreshold = plugin.getConfig().getInt("vampire.humanity.low-threshold", 30);
        if (before > lowThreshold && after <= lowThreshold) {
            player.sendMessage("§4Your humanity is slipping. You feel the ripper taking hold...");
            plugin.getCodexManager().discover(player, com.canopycreations.mysticcraft.lore.LoreFragment.THE_SWITCH);
        }
        if (after == 0) {
            player.sendMessage("§4§lYou have switched off your humanity. Nothing holds you back now.");
            Bukkit.broadcastMessage("§4" + player.getName() + " has gone full ripper - humanity switch OFF.");

            // Reaching zero humanity and surviving it is what makes The Immortal.
            if (plugin.getConfig().getBoolean("progenitors.enabled", true)
                    && plugin.getConfig().getBoolean("progenitors.immortal-requires-humanity-zero", true)) {
                plugin.getProgenitorManager().claim(player,
                        com.canopycreations.mysticcraft.lore.Progenitor.THE_IMMORTAL);
            }
        }
        plugin.getDataStore().save(data);
    }

    /**
     * Called periodically for vampires below the low-humanity threshold -
     * gives a small chance of an uncontrollable rage effect (Nausea + Strength
     * spike) representing losing control.
     */
    public void tickRipperCheck(Player player) {
        PlayerData data = plugin.getRaceManager().getData(player);
        if (data.getRace() != Race.VAMPIRE) return;

        int lowThreshold = plugin.getConfig().getInt("vampire.humanity.low-threshold", 30);
        if (data.getHumanity() > lowThreshold) return;

        int chance = plugin.getConfig().getInt("vampire.humanity.ripper-rage-chance-percent", 5);
        if (random.nextInt(100) < chance) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 2));
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 0));
            player.sendMessage("§4The ripper takes over for a moment...");
        }
    }

    public boolean isRipper(Player player) {
        PlayerData data = plugin.getRaceManager().getData(player);
        return data.getRace() == Race.VAMPIRE
                && data.getHumanity() <= plugin.getConfig().getInt("vampire.humanity.low-threshold", 30);
    }
}
