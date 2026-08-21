package com.canopycreations.mysticcraft.util;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Every supernatural act should be visible to bystanders, not just to the
 * person doing it. If a vampire compels someone in a crowded square, the
 * crowd should see something happen - that's what makes secrecy a real
 * social problem instead of a chat-window formality.
 */
public final class Fx {

    private Fx() {}

    // ------------------------------------------------------------------
    // Vampire
    // ------------------------------------------------------------------
    public static void compulsion(Player caster, Player target) {
        World w = caster.getWorld();
        Location mid = caster.getEyeLocation().clone().add(target.getEyeLocation()).multiply(0.5);

        // A thin thread of dark particles drawn between their eyes.
        Vector step = target.getEyeLocation().toVector()
                .subtract(caster.getEyeLocation().toVector()).multiply(1.0 / 12.0);
        Location p = caster.getEyeLocation().clone();
        for (int i = 0; i < 12; i++) {
            w.spawnParticle(Particle.DUST, p, 2, 0.02, 0.02, 0.02, 0,
                    new Particle.DustOptions(Color.fromRGB(80, 0, 0), 0.8f));
            p.add(step);
        }
        w.spawnParticle(Particle.SMOKE, target.getEyeLocation(), 12, 0.2, 0.2, 0.2, 0.01);
        w.playSound(mid, Sound.ENTITY_ENDERMAN_STARE, 0.7f, 1.6f);

        // The target's world lurches.
        target.playSound(target.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.4f, 1.8f);
    }

    public static void feeding(Player vampire, Location at) {
        World w = vampire.getWorld();
        w.spawnParticle(Particle.DUST, at.clone().add(0, 1.2, 0), 25, 0.25, 0.25, 0.25, 0,
                new Particle.DustOptions(Color.fromRGB(140, 0, 0), 1.2f));
        w.playSound(at, Sound.ENTITY_GENERIC_DRINK, 1.0f, 0.55f);
        w.playSound(at, Sound.ENTITY_PLAYER_HURT, 0.5f, 0.7f);
    }

    public static void sunlightBurn(Player vampire) {
        World w = vampire.getWorld();
        Location l = vampire.getLocation().add(0, 1, 0);
        w.spawnParticle(Particle.FLAME, l, 10, 0.3, 0.6, 0.3, 0.01);
        w.spawnParticle(Particle.LARGE_SMOKE, l, 6, 0.25, 0.5, 0.25, 0.02);
        w.playSound(l, Sound.ENTITY_GENERIC_BURN, 0.5f, 1.3f);
    }

    public static void bloodShared(Player vampire, Player human) {
        World w = vampire.getWorld();
        Location mid = vampire.getLocation().clone().add(human.getLocation()).multiply(0.5).add(0, 1, 0);
        w.spawnParticle(Particle.DUST, mid, 40, 0.4, 0.5, 0.4, 0,
                new Particle.DustOptions(Color.fromRGB(120, 0, 20), 1.4f));
        w.playSound(mid, Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 0.6f);
    }

    public static void transitionComplete(Player player) {
        World w = player.getWorld();
        Location l = player.getLocation();
        w.spawnParticle(Particle.DUST, l.clone().add(0, 1, 0), 80, 0.6, 1.0, 0.6, 0,
                new Particle.DustOptions(Color.fromRGB(90, 0, 0), 2.0f));
        w.playSound(l, Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.8f);
        w.playSound(l, Sound.ITEM_TOTEM_USE, 0.8f, 0.7f);
    }

    // ------------------------------------------------------------------
    // Werewolf
    // ------------------------------------------------------------------
    public static void howl(Player wolf) {
        World w = wolf.getWorld();
        Location l = wolf.getLocation();
        w.playSound(l, Sound.ENTITY_WOLF_HOWL, 2.0f, 0.55f);
        // Everyone within earshot hears it, wherever they are.
        for (Player other : w.getPlayers()) {
            if (other.equals(wolf)) continue;
            double d = other.getLocation().distance(l);
            if (d > 120) continue;
            float vol = (float) Math.max(0.15, 1.0 - (d / 120.0));
            other.playSound(other.getLocation(), Sound.ENTITY_WOLF_HOWL, vol, 0.55f);
            if (d > 40) other.sendActionBar("§8A howl, somewhere out there.");
        }
    }

    public static void transformation(Player wolf) {
        World w = wolf.getWorld();
        Location l = wolf.getLocation().add(0, 1, 0);
        w.spawnParticle(Particle.DUST, l, 60, 0.5, 0.8, 0.5, 0,
                new Particle.DustOptions(Color.fromRGB(120, 80, 30), 1.6f));
        w.spawnParticle(Particle.CRIT, l, 30, 0.4, 0.6, 0.4, 0.3);
        w.playSound(l, Sound.ENTITY_RAVAGER_ROAR, 1.2f, 0.7f);
        w.playSound(l, Sound.BLOCK_BONE_BLOCK_BREAK, 1.4f, 0.5f);
    }

    public static void venomBite(Player wolf, Player victim) {
        World w = victim.getWorld();
        Location l = victim.getLocation().add(0, 1, 0);
        w.spawnParticle(Particle.DUST, l, 20, 0.3, 0.4, 0.3, 0,
                new Particle.DustOptions(Color.fromRGB(60, 140, 40), 1.1f));
        w.playSound(l, Sound.ENTITY_WOLF_GROWL, 1.0f, 0.6f);
    }

    public static void venomTick(Player victim) {
        victim.getWorld().spawnParticle(Particle.ITEM_SLIME,
                victim.getLocation().add(0, 1, 0), 6, 0.3, 0.4, 0.3, 0.01);
    }

    // ------------------------------------------------------------------
    // Witch
    // ------------------------------------------------------------------
    public static void castingBuildup(Player witch) {
        World w = witch.getWorld();
        Location l = witch.getLocation().add(0, 1, 0);
        w.spawnParticle(Particle.WITCH, l, 15, 0.4, 0.5, 0.4, 0.02);
        w.playSound(l, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.6f, 1.4f);
    }

    public static void spellRelease(Player witch, Color color) {
        World w = witch.getWorld();
        Location l = witch.getEyeLocation();
        // A ring expanding outward from the caster.
        for (int i = 0; i < 24; i++) {
            double angle = (Math.PI * 2 / 24) * i;
            Location ring = witch.getLocation().add(Math.cos(angle) * 1.4, 0.4, Math.sin(angle) * 1.4);
            w.spawnParticle(Particle.DUST, ring, 2, 0.05, 0.05, 0.05, 0,
                    new Particle.DustOptions(color, 1.2f));
            witch.getLocation().subtract(Math.cos(angle) * 1.4, 0.4, Math.sin(angle) * 1.4);
        }
        w.playSound(l, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.9f, 1.1f);
    }

    public static void spellBeam(Player witch, Player target, Color color) {
        World w = witch.getWorld();
        Vector step = target.getEyeLocation().toVector()
                .subtract(witch.getEyeLocation().toVector()).multiply(1.0 / 20.0);
        Location p = witch.getEyeLocation().clone();
        for (int i = 0; i < 20; i++) {
            w.spawnParticle(Particle.DUST, p, 2, 0.03, 0.03, 0.03, 0,
                    new Particle.DustOptions(color, 1.0f));
            p.add(step);
        }
    }

    public static void backlash(Player witch) {
        World w = witch.getWorld();
        Location l = witch.getLocation().add(0, 1, 0);
        w.spawnParticle(Particle.DUST, l, 40, 0.5, 0.6, 0.5, 0,
                new Particle.DustOptions(Color.fromRGB(160, 0, 0), 1.5f));
        w.playSound(l, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 0.6f);
    }

    public static void boundaryWall(Location center, double radius) {
        World w = center.getWorld();
        for (int i = 0; i < 60; i++) {
            double angle = (Math.PI * 2 / 60) * i;
            for (double y = 0; y <= 3; y += 1.0) {
                Location p = center.clone().add(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
                w.spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(140, 60, 180), 1.0f));
            }
        }
    }

    public static void ringForged(Player witch, Player vampire) {
        World w = witch.getWorld();
        Location l = vampire.getLocation().add(0, 1, 0);
        w.spawnParticle(Particle.END_ROD, l, 30, 0.4, 0.5, 0.4, 0.05);
        w.playSound(l, Sound.BLOCK_ANVIL_USE, 0.8f, 1.4f);
        w.playSound(l, Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.2f);
    }

    // ------------------------------------------------------------------
    // Discovery / lore
    // ------------------------------------------------------------------
    public static void discovery(Player player) {
        World w = player.getWorld();
        Location l = player.getLocation().add(0, 1.5, 0);
        w.spawnParticle(Particle.ENCHANT, l, 25, 0.5, 0.5, 0.5, 0.5);
        player.playSound(l, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.3f);
    }

    public static void progenitorAwakening(Player player) {
        World w = player.getWorld();
        Location l = player.getLocation();
        // Note: Particle.FLASH requires a Color data argument in 26.2, so we use
        // EXPLOSION_EMITTER here instead - same visual weight, no data type needed.
        w.spawnParticle(Particle.EXPLOSION_EMITTER, l.clone().add(0, 1, 0), 2);
        w.spawnParticle(Particle.END_ROD, l.clone().add(0, 1, 0), 120, 0.8, 1.5, 0.8, 0.15);
        w.playSound(l, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.6f);
        // Everyone on the server feels it, wherever they are.
        for (Player other : org.bukkit.Bukkit.getOnlinePlayers()) {
            other.playSound(other.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 0.5f);
        }
    }

    // ------------------------------------------------------------------
    // Territory
    // ------------------------------------------------------------------
    public static void totemPlanted(Location at, Color color) {
        World w = at.getWorld();
        for (double y = 0; y < 4; y += 0.2) {
            w.spawnParticle(Particle.DUST, at.clone().add(0.5, y, 0.5), 2, 0.1, 0, 0.1, 0,
                    new Particle.DustOptions(color, 1.3f));
        }
        w.playSound(at, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.8f);
    }

    public static void totemAmbient(Location at, Color color) {
        at.getWorld().spawnParticle(Particle.DUST, at.clone().add(0.5, 1.2, 0.5), 3, 0.25, 0.4, 0.25, 0,
                new Particle.DustOptions(color, 1.0f));
    }
}
