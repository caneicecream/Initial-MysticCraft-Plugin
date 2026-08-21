package com.canopycreations.mysticcraft.listeners;

import com.canopycreations.mysticcraft.MysticCraft;
import com.canopycreations.mysticcraft.data.PlayerData;
import com.canopycreations.mysticcraft.items.MysticItems;
import com.canopycreations.mysticcraft.lore.LoreFragment;
import com.canopycreations.mysticcraft.races.Race;
import com.canopycreations.mysticcraft.util.Fx;
import org.bukkit.Color;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Physical rituals. No commands.
 *
 * Everything here is something a player DOES with their body and their
 * hands, and that bystanders can watch happen:
 *
 *   - Witches cast by holding a Grimoire Wand and performing a gesture.
 *     Where you're looking and whether you're sneaking selects the spell.
 *   - Vampires compel by making eye contact at close range and holding it.
 *   - Werewolves shift by howling at the sky at night.
 *   - Lore books are read by right-clicking them, like any book.
 *
 * The commands still exist as an accessibility fallback, but nothing in the
 * intended play loop requires typing.
 */
public class RitualListener implements Listener {

    private final MysticCraft plugin;

    /** Tracks a vampire holding eye contact: target uuid -> ticks held. */
    private final Map<UUID, Integer> eyeContactTicks = new HashMap<>();
    private final Map<UUID, UUID> eyeContactTarget = new HashMap<>();
    /** Sneak-start timestamps, used to detect a sustained howl posture. */
    private final Map<UUID, Long> sneakStart = new HashMap<>();

    public RitualListener(MysticCraft plugin) {
        this.plugin = plugin;
    }

    // ==================================================================
    // WITCH: gesture spellcasting with the Grimoire Wand
    // ==================================================================
    @EventHandler(ignoreCancelled = true)
    public void onWandGesture(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!plugin.getMysticItems().hasTag(held, MysticItems.TAG_WITCH_WAND)) return;
        if (plugin.getRaceManager().getRace(player) != Race.WITCH) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                player.sendActionBar("§8The wand is inert in your hand. It isn't yours.");
                event.setCancelled(true);
            }
            return;
        }

        boolean rightClick = event.getAction() == Action.RIGHT_CLICK_AIR
                || event.getAction() == Action.RIGHT_CLICK_BLOCK;
        boolean leftClick = event.getAction() == Action.LEFT_CLICK_AIR
                || event.getAction() == Action.LEFT_CLICK_BLOCK;
        if (!rightClick && !leftClick) return;

        event.setCancelled(true);

        float pitch = player.getLocation().getPitch();
        boolean lookingUp = pitch < -50;      // at the sky
        boolean lookingDown = pitch > 55;     // at the ground
        boolean sneaking = player.isSneaking();
        LivingEntity target = rayTarget(player);

        // The gesture vocabulary. Each is a distinct physical motion.
        String spell;
        if (lookingDown && sneaking) {
            spell = "boundary";        // kneel and touch the earth
        } else if (lookingUp && !sneaking) {
            spell = "heal";            // raise the wand to the sky
        } else if (leftClick && target != null) {
            spell = "telekinesis";     // strike toward them
        } else if (sneaking && target != null) {
            spell = "pain";            // point low and close your stance
        } else if (lookingUp && sneaking) {
            spell = "forgering";       // both hands raised, kneeling - the long working
        } else if (target != null) {
            spell = "desiccate";       // point directly at them
        } else {
            player.sendActionBar("§8The gesture means nothing. Your hand knows it.");
            return;
        }

        Fx.castingBuildup(player);
        boolean cast = plugin.getSpellManager().cast(player, spell, target);
        if (cast) {
            Color color = switch (spell) {
                case "heal" -> Color.fromRGB(90, 220, 120);
                case "pain" -> Color.fromRGB(150, 40, 180);
                case "desiccate" -> Color.fromRGB(180, 140, 40);
                case "telekinesis" -> Color.fromRGB(120, 180, 220);
                case "forgering" -> Color.fromRGB(250, 220, 120);
                default -> Color.fromRGB(140, 60, 180);
            };
            Fx.spellRelease(player, color);
            if (target instanceof Player tp) Fx.spellBeam(player, tp, color);
            player.sendActionBar("§5" + spell);
        }
    }

    // ==================================================================
    // VAMPIRE: compulsion through sustained eye contact
    // ==================================================================
    /**
     * Called every tick-ish from the plugin's fast loop. A vampire who
     * sneaks, stands close, and holds someone's gaze builds toward
     * compulsion - and the target sees it coming, which gives them a chance
     * to look away or run.
     */
    public void tickEyeContact(Player vampire) {
        PlayerData data = plugin.getRaceManager().getData(vampire);
        if (data.getRace() != Race.VAMPIRE || !vampire.isSneaking()) {
            clearEyeContact(vampire);
            return;
        }

        double range = plugin.getConfig().getDouble("vampire.compulsion-range", 5.0);
        Player target = null;
        for (Player other : vampire.getWorld().getPlayers()) {
            if (other.equals(vampire)) continue;
            if (other.getLocation().distance(vampire.getLocation()) > range) continue;
            if (!isLookingAt(vampire, other)) continue;
            if (!isLookingAt(other, vampire)) continue; // mutual gaze required
            target = other;
            break;
        }

        if (target == null) {
            clearEyeContact(vampire);
            return;
        }

        long cooldownMillis = plugin.getConfig().getInt("vampire.compulsion-cooldown-seconds", 90) * 1000L;
        if (System.currentTimeMillis() - data.getLastCompulsionMillis() < cooldownMillis) {
            vampire.sendActionBar("§8Not yet. Whatever you spent hasn't come back.");
            return;
        }

        // Vervain breaks it immediately, and both parties feel the failure.
        if (carriesVervain(target)) {
            vampire.sendActionBar("§4Vervain. You can feel it push back.");
            target.sendActionBar("§dSomething tried to get in. It didn't.");
            clearEyeContact(vampire);
            return;
        }

        int held = eyeContactTicks.merge(vampire.getUniqueId(), 1, Integer::sum);
        eyeContactTarget.put(vampire.getUniqueId(), target.getUniqueId());

        int required = plugin.getConfig().getInt("vampire.compulsion-hold-ticks", 40);

        if (held < required) {
            // Both of them watch it building. The target can still break away.
            float progress = (float) held / required;
            vampire.sendActionBar("§4Holding their gaze... §8" + bar(progress));
            target.sendActionBar("§8You can't quite look away... §8" + bar(progress));
            target.getWorld().spawnParticle(org.bukkit.Particle.SMOKE,
                    target.getEyeLocation(), 3, 0.1, 0.1, 0.1, 0.01);
            return;
        }

        // Compulsion lands.
        data.setLastCompulsionMillis(System.currentTimeMillis());
        plugin.getDataStore().save(data);
        clearEyeContact(vampire);

        Fx.compulsion(vampire, target);
        target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.NAUSEA, 60, 0));
        target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.SLOWNESS, 60, 1));

        vampire.sendTitle("", "§4You have their attention. Tell them something.", 5, 40, 10);
        target.sendTitle("§4§o...", "§7You'll do what they say. You won't remember deciding to.", 10, 60, 20);

        // Bystanders see it happen - secrecy is a real problem.
        for (Player watcher : vampire.getWorld().getPlayers()) {
            if (watcher.equals(vampire) || watcher.equals(target)) continue;
            if (watcher.getLocation().distance(vampire.getLocation()) > 12) continue;
            watcher.sendActionBar("§8Something passed between them just then.");
            plugin.getCodexManager().discover(watcher, LoreFragment.THE_THIRST);
        }
    }

    private String bar(float progress) {
        int filled = (int) (progress * 10);
        StringBuilder sb = new StringBuilder("§4");
        for (int i = 0; i < 10; i++) {
            if (i == filled) sb.append("§8");
            sb.append("|");
        }
        return sb.toString();
    }

    private void clearEyeContact(Player vampire) {
        eyeContactTicks.remove(vampire.getUniqueId());
        eyeContactTarget.remove(vampire.getUniqueId());
    }

    private boolean isLookingAt(Player looker, Player target) {
        Vector toTarget = target.getEyeLocation().toVector()
                .subtract(looker.getEyeLocation().toVector()).normalize();
        return looker.getLocation().getDirection().normalize().dot(toTarget) > 0.93; // tight cone
    }

    private boolean carriesVervain(Player player) {
        for (ItemStack stack : player.getInventory().getContents()) {
            if (plugin.getMysticItems().hasTag(stack, MysticItems.TAG_VERVAIN)) return true;
        }
        return false;
    }

    // ==================================================================
    // WEREWOLF: howl at the sky to shift
    // ==================================================================
    @EventHandler
    public void onSneakToggle(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (event.isSneaking()) {
            sneakStart.put(player.getUniqueId(), System.currentTimeMillis());
        } else {
            sneakStart.remove(player.getUniqueId());
        }
    }

    /**
     * A werewolf who crouches, looks straight up at the night sky, and holds
     * it for a moment howls - and howling is what triggers a voluntary shift.
     * It is loud, it carries 120 blocks, and everyone hears it.
     */
    public void tickHowl(Player player) {
        PlayerData data = plugin.getRaceManager().getData(player);
        if (data.getRace() != Race.WEREWOLF || !data.hasTriggeredCurse()) return;
        if (!player.isSneaking()) return;
        if (player.getLocation().getPitch() > -60) return;   // must be looking up
        if (player.getWorld().isDayTime()) return;

        Long since = sneakStart.get(player.getUniqueId());
        if (since == null || System.currentTimeMillis() - since < 1500) return;

        sneakStart.remove(player.getUniqueId());

        boolean fullMoon = plugin.getMoonPhaseManager().isFullMoon(player.getWorld());
        if (fullMoon && !data.isShifted()) {
            player.sendActionBar("§6You don't have to call it tonight. It's already coming.");
            Fx.howl(player);
            return;
        }

        data.setShifted(!data.isShifted());
        plugin.getRaceManager().refreshAttributes(player);
        plugin.getDataStore().save(data);

        Fx.howl(player);
        Fx.transformation(player);

        if (data.isShifted()) {
            player.sendTitle("", "§6You give in to it.", 5, 30, 10);
        } else {
            player.sendTitle("", "§7You force yourself back.", 5, 30, 10);
        }

        // Anyone watching learns something they'd rather not have.
        for (Player watcher : player.getWorld().getPlayers()) {
            if (watcher.equals(player)) continue;
            if (watcher.getLocation().distance(player.getLocation()) > 24) continue;
            plugin.getCodexManager().discover(watcher, LoreFragment.THE_SLEEPING_CURSE);
        }
    }

    // ==================================================================
    // ANYONE: reading a lore book
    // ==================================================================
    @EventHandler(ignoreCancelled = true)
    public void onReadLoreBook(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack held = event.getItem();
        LoreFragment fragment = plugin.getLoreBooks().getFragment(held);
        if (fragment == null) return;

        Player player = event.getPlayer();
        boolean isNew = plugin.getCodexManager().discover(player, fragment);
        if (isNew) {
            Fx.discovery(player);
            player.sendMessage("§8You read it twice, then a third time. It stays with you now.");
            player.sendMessage("§8The book is just paper. §7You §8are the record.");
        }
        // The vanilla book UI opens on its own - we don't cancel it.
    }
}
