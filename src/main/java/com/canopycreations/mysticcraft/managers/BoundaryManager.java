package com.canopycreations.mysticcraft.managers;

import com.canopycreations.mysticcraft.MysticCraft;
import com.canopycreations.mysticcraft.races.Race;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BoundaryManager {

    private record Boundary(Location center, double radiusSquared, long expiresAtMillis) {}

    private final MysticCraft plugin;
    private final List<Boundary> boundaries = new ArrayList<>();

    public BoundaryManager(MysticCraft plugin) {
        this.plugin = plugin;
    }

    public void register(Location center, double radius, long expiresAtMillis) {
        boundaries.add(new Boundary(center, radius * radius, expiresAtMillis));
    }

    /** Called every tick (from the movement listener) - pushes vampires back out of any active boundary. */
    public void enforce(Player player) {
        if (plugin.getRaceManager().getRace(player) != Race.VAMPIRE) return;

        Iterator<Boundary> it = boundaries.iterator();
        while (it.hasNext()) {
            Boundary b = it.next();
            if (System.currentTimeMillis() > b.expiresAtMillis()) {
                it.remove();
                continue;
            }
            if (!b.center().getWorld().equals(player.getWorld())) continue;
            if (player.getLocation().distanceSquared(b.center()) <= b.radiusSquared()) {
                org.bukkit.util.Vector away = player.getLocation().toVector()
                        .subtract(b.center().toVector()).normalize().multiply(1.2);
                player.setVelocity(away);
                player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 40, 0));
                player.sendMessage("§5An invisible wall repels you - a witch's boundary spell.");
            }
        }
    }
}
