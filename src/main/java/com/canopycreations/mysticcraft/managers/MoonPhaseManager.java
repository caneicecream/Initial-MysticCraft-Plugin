package com.canopycreations.mysticcraft.managers;

import com.canopycreations.mysticcraft.MysticCraft;
import com.canopycreations.mysticcraft.data.PlayerData;
import com.canopycreations.mysticcraft.races.Race;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Minecraft's day cycle naturally cycles through 8 moon phases every 8 days
 * (full_time / 24000 % 8). Phase 0 is a full moon. We use that as the trigger
 * for involuntary werewolf transformations, matching TVD lore where a
 * triggered werewolf has no choice but to shift on the full moon.
 */
public class MoonPhaseManager {

    private final MysticCraft plugin;

    public MoonPhaseManager(MysticCraft plugin) {
        this.plugin = plugin;
    }

    public boolean isFullMoon(World world) {
        long day = world.getFullTime() / 24000L;
        int phase = (int) (day % 8);
        return phase == 0;
    }

    /**
     * Runs on a timer (see MysticCraft#startTasks). Forces any triggered
     * werewolf standing under an open sky at night during a full moon into
     * wolf form, with a painful first-shift warning the very first time.
     */
    public void tick() {
        if (!plugin.getConfig().getBoolean("werewolf.forced-shift-on-full-moon", true)) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = plugin.getRaceManager().getData(player);
            if (data.getRace() != Race.WEREWOLF || !data.hasTriggeredCurse()) continue;
            if (data.isShifted()) continue;

            World world = player.getWorld();
            boolean night = world.getTime() >= 13000 && world.getTime() <= 23000;
            if (night && isFullMoon(world)) {
                forceShift(player, data);
            }
        }
    }

    private void forceShift(Player player, PlayerData data) {
        boolean firstShift = plugin.getConfig().getBoolean("werewolf.first-shift-trauma", true)
                && data.getLastShiftMillis() == 0L;

        data.setShifted(true);
        data.setLastShiftMillis(System.currentTimeMillis());
        plugin.getRaceManager().refreshAttributes(player);
        plugin.getDataStore().save(data);

        if (firstShift) {
            player.sendMessage("§6§lYour bones are breaking. The change takes you for the first time - there is no fighting it.");
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
        } else {
            player.sendMessage("§6The full moon calls. You feel the shift take hold - you have no control tonight.");
        }
        Bukkit.broadcastMessage("§6" + player.getName() + " §7has been forced into wolf form by the full moon!");
    }

    /**
     * Called at dawn to revert any werewolves still in forced wolf form.
     */
    public void revertAtDawn() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = plugin.getRaceManager().getData(player);
            if (data.getRace() != Race.WEREWOLF || !data.isShifted()) continue;

            World world = player.getWorld();
            boolean dawn = world.getTime() >= 0 && world.getTime() <= 500;
            if (dawn) {
                data.setShifted(false);
                plugin.getRaceManager().refreshAttributes(player);
                plugin.getDataStore().save(data);
                player.sendMessage("§6The sun rises. You return to human form, exhausted.");
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 1200, 0));
            }
        }
    }
}
