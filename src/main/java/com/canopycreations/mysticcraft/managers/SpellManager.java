package com.canopycreations.mysticcraft.managers;

import com.canopycreations.mysticcraft.MysticCraft;
import com.canopycreations.mysticcraft.data.PlayerData;
import com.canopycreations.mysticcraft.items.MysticItems;
import com.canopycreations.mysticcraft.races.Race;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Witch spellcasting. Every spell consumes one Spell Herb (configurable),
 * respects a cooldown, and gets stronger the more nearby witches are
 * "channeling" with the caster (TVD's channeling mechanic).
 */
public class SpellManager {

    public static final List<String> SPELLS = List.of("heal", "telekinesis", "pain", "boundary", "desiccate", "forgering");

    private final MysticCraft plugin;
    private final Random random = new Random();

    public SpellManager(MysticCraft plugin) {
        this.plugin = plugin;
    }

    public boolean canCast(Player witch, PlayerData data) {
        if (data.getRace() != Race.WITCH) return false;
        long cooldownMillis = plugin.getConfig().getInt("witch.spell-cooldown-seconds", 20) * 1000L;
        return System.currentTimeMillis() - data.getLastSpellMillis() >= cooldownMillis;
    }

    public long cooldownRemainingSeconds(PlayerData data) {
        long cooldownMillis = plugin.getConfig().getInt("witch.spell-cooldown-seconds", 20) * 1000L;
        long remaining = cooldownMillis - (System.currentTimeMillis() - data.getLastSpellMillis());
        return Math.max(0, remaining / 1000L);
    }

    /**
     * Attempts to cast a spell. Returns true if the spell was cast (and
     * herb/cooldown consumed), false if it failed silently validated checks
     * (caller is responsible for messaging on false).
     */
    public boolean cast(Player witch, String spellName, LivingEntity target) {
        PlayerData data = plugin.getRaceManager().getData(witch);
        if (!canCast(witch, data)) {
            witch.sendMessage("§5Your power hasn't replenished yet. (" + cooldownRemainingSeconds(data) + "s left)");
            return false;
        }

        String spell = spellName.toLowerCase(Locale.ROOT);
        if (!SPELLS.contains(spell)) {
            witch.sendMessage("§5Unknown spell. Known spells: " + String.join(", ", SPELLS));
            return false;
        }

        int herbCost = spell.equals("forgering")
                ? plugin.getConfig().getInt("witch.ring-forge-herb-cost", 3)
                : 1;
        if (plugin.getConfig().getBoolean("witch.spellcasting-requires-herb", true) && !consumeHerbs(witch, herbCost)) {
            witch.sendMessage("§5You need " + herbCost + " Spell Herb" + (herbCost == 1 ? "" : "s") + " to channel that.");
            return false;
        }

        int channelers = countNearbyWitches(witch);
        double power = 1.0 + channelers * (plugin.getConfig().getDouble("witch.channeling-nearby-witch-bonus-percent", 25) / 100.0);

        boolean overchannel = channelers >= 2;
        if (overchannel && random.nextInt(100) < plugin.getConfig().getInt("witch.overchannel-backlash-chance-percent", 20)) {
            double backlash = plugin.getConfig().getDouble("witch.overchannel-backlash-damage", 6.0);
            witch.damage(backlash);
            com.canopycreations.mysticcraft.util.Fx.backlash(witch);
            witch.sendTitle("", "§4The magic pushes back.", 5, 40, 10);
            plugin.getQuestManager().progress(witch, com.canopycreations.mysticcraft.quests.Questline.Objective.SURVIVE_BACKLASH);
        }

        switch (spell) {
            case "heal" -> castHeal(witch, power);
            case "telekinesis" -> castTelekinesis(witch, target, power);
            case "pain" -> castPain(witch, target, power);
            case "boundary" -> castBoundary(witch, power);
            case "desiccate" -> castDesiccate(witch, target, power);
            case "forgering" -> castForgeRing(witch, target);
        }

        plugin.getQuestManager().progress(witch, com.canopycreations.mysticcraft.quests.Questline.Objective.CAST_SPELLS);
        plugin.getCodexManager().discover(witch, com.canopycreations.mysticcraft.lore.LoreFragment.THE_PRICE);
        if (channelers > 0) {
            plugin.getQuestManager().progress(witch, com.canopycreations.mysticcraft.quests.Questline.Objective.CHANNEL_WITH_WITCH);
            plugin.getCodexManager().discover(witch, com.canopycreations.mysticcraft.lore.LoreFragment.CHANNELING);
        }

        data.setLastSpellMillis(System.currentTimeMillis());
        data.setSpellsCastToday(data.getSpellsCastToday() + 1);
        plugin.getDataStore().save(data);

        // Mastery of the craft is what makes The Original Witch.
        if (plugin.getConfig().getBoolean("progenitors.enabled", true)) {
            int required = plugin.getConfig().getInt("progenitors.original-witch-requires-spells", 50);
            if (data.getSpellsCastToday() >= required) {
                plugin.getProgenitorManager().claim(witch,
                        com.canopycreations.mysticcraft.lore.Progenitor.THE_ORIGINAL_WITCH);
            }
        }
        return true;
    }

    private boolean consumeHerbs(Player witch, int amount) {
        MysticItems items = plugin.getMysticItems();
        int available = 0;
        for (ItemStack stack : witch.getInventory().getContents()) {
            if (items.hasTag(stack, MysticItems.TAG_WITCH_HERB)) {
                available += stack.getAmount();
            }
        }
        if (available < amount) return false;

        int remaining = amount;
        for (ItemStack stack : witch.getInventory().getContents()) {
            if (remaining <= 0) break;
            if (!items.hasTag(stack, MysticItems.TAG_WITCH_HERB)) continue;
            int take = Math.min(remaining, stack.getAmount());
            stack.setAmount(stack.getAmount() - take);
            remaining -= take;
        }
        return true;
    }

    private int countNearbyWitches(Player witch) {
        double radius = plugin.getConfig().getDouble("witch.channeling-radius", 15);
        int count = 0;
        for (org.bukkit.entity.Entity nearby : witch.getNearbyEntities(radius, radius, radius)) {
            if (nearby instanceof Player p && plugin.getRaceManager().getRace(p) == Race.WITCH) {
                count++;
            }
        }
        return count;
    }

    private void castHeal(Player witch, double power) {
        double maxHealth = witch.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        witch.setHealth(Math.min(maxHealth, witch.getHealth() + 6.0 * power));
        witch.sendMessage("§aYou channel restorative magic through yourself.");
    }

    private void castTelekinesis(Player witch, LivingEntity target, double power) {
        if (target == null) {
            witch.sendMessage("§5Telekinesis needs a target - look at an entity when casting.");
            return;
        }
        Vector push = target.getLocation().toVector().subtract(witch.getLocation().toVector()).normalize().multiply(1.5 * power);
        push.setY(0.5 * power);
        target.setVelocity(push);
        witch.sendMessage("§5You fling them back with your mind.");
    }

    private void castPain(Player witch, LivingEntity target, double power) {
        if (target == null) {
            witch.sendMessage("§5Pain requires a target - look at an entity when casting.");
            return;
        }
        int durationTicks = (int) (60 * power);
        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, durationTicks, 0));
        target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, durationTicks, 1));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, 1));
        if (target instanceof Player targetPlayer) {
            targetPlayer.sendMessage("§5§lYour skull is splitting - an unbearable, invisible pain.");
        }
        witch.sendMessage("§5You focus, and they crumple in agony.");
    }

    private void castBoundary(Player witch, double power) {
        double radius = 10 * power;
        int durationTicks = 20 * 30; // 30 seconds
        witch.sendMessage("§5You draw a boundary spell - vampires cannot cross it for 30 seconds.");
        plugin.registerBoundary(witch.getLocation(), radius, System.currentTimeMillis() + durationTicks * 50L);
        com.canopycreations.mysticcraft.util.Fx.boundaryWall(witch.getLocation(), radius);
    }

    private void castDesiccate(Player witch, LivingEntity target, double power) {
        if (target == null || !(target instanceof Player targetPlayer)) {
            witch.sendMessage("§5Desiccate only works on a vampire target - look at them when casting.");
            return;
        }
        if (plugin.getRaceManager().getRace(targetPlayer) != Race.VAMPIRE) {
            witch.sendMessage("§5Desiccate only works on vampires.");
            return;
        }
        targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, (int) (100 * power), 3));
        targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int) (100 * power), 2));
        targetPlayer.damage(2.0 * power);
        targetPlayer.sendMessage("§5Your veins dry up - a witch is desiccating you!");
        witch.sendMessage("§5You draw the moisture from their very veins.");
    }

    private void castForgeRing(Player witch, LivingEntity target) {
        if (!(target instanceof Player targetPlayer) || plugin.getRaceManager().getRace(targetPlayer) != Race.VAMPIRE) {
            witch.sendMessage("§5Forging a Daylight Ring requires a vampire target - look at them when casting.");
            return;
        }
        ItemStack ring = plugin.getMysticItems().daylightRing();
        targetPlayer.getInventory().addItem(ring);
        com.canopycreations.mysticcraft.util.Fx.ringForged(witch, targetPlayer);
        targetPlayer.sendTitle("", "§6A Daylight Ring, forged for you.", 10, 50, 15);
        witch.sendMessage("§5You channel old magic, binding sun-ward protection into a ring for " + targetPlayer.getName() + ".");
        plugin.getQuestManager().progress(witch, com.canopycreations.mysticcraft.quests.Questline.Objective.FORGE_RING);
    }
}
