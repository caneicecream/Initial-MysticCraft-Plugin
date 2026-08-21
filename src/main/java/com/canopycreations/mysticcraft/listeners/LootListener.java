package com.canopycreations.mysticcraft.listeners;

import com.canopycreations.mysticcraft.MysticCraft;
import com.canopycreations.mysticcraft.lore.LoreFragment;
import org.bukkit.Material;
import org.bukkit.block.Container;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.LootGenerateEvent;

import java.util.List;
import java.util.Random;

/**
 * Lore in the ground.
 *
 * Fragments are seeded into naturally generated loot so that exploring
 * actually turns up the world's history - a journal in a village chest, a
 * torn page in a stronghold library, something somebody buried in a desert
 * temple and never came back for.
 *
 * Which fragment appears is weighted by where you found it, so the placement
 * reads as deliberate rather than random: wolf-shrine lore turns up in
 * jungle temples and mineshafts, witch material in strongholds, vampire
 * material in mansions and villages.
 */
public class LootListener implements Listener {

    private final MysticCraft plugin;
    private final Random random = new Random();

    public LootListener(MysticCraft plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onLootGenerate(LootGenerateEvent event) {
        if (!plugin.getConfig().getBoolean("lore.seed-books-in-loot", true)) return;
        if (!(event.getInventoryHolder() instanceof Container)) return;

        double chance = plugin.getConfig().getDouble("lore.book-loot-chance-percent", 12.0);
        if (random.nextDouble() * 100.0 > chance) return;

        String table = event.getLootTable().getKey().getKey().toLowerCase();
        LoreFragment fragment = pickForLocation(table);
        if (fragment == null) return;

        event.getLoot().add(plugin.getLoreBooks().create(fragment));
    }

    /** Weighted by structure so found lore feels placed rather than sprinkled. */
    private LoreFragment pickForLocation(String table) {
        List<LoreFragment> pool;

        if (table.contains("stronghold") || table.contains("library")) {
            pool = List.of(LoreFragment.THE_PRICE, LoreFragment.CHANNELING,
                    LoreFragment.THE_ASH_MOTHER, LoreFragment.THE_CURE);
        } else if (table.contains("mansion") || table.contains("village")) {
            pool = List.of(LoreFragment.THE_THIRST, LoreFragment.THE_SUN_AND_THE_RING,
                    LoreFragment.THE_SWITCH, LoreFragment.SOMETHING_IN_THE_DARK);
        } else if (table.contains("jungle") || table.contains("mineshaft") || table.contains("temple")) {
            pool = List.of(LoreFragment.SEVEN_KNIVES, LoreFragment.THE_SLEEPING_CURSE,
                    LoreFragment.THE_VENOM);
        } else if (table.contains("end") || table.contains("bastion") || table.contains("fortress")) {
            pool = List.of(LoreFragment.THE_UNBURIED, LoreFragment.THE_CURE);
        } else {
            pool = List.of(LoreFragment.THE_BALANCE, LoreFragment.SOMETHING_IN_THE_DARK);
        }

        return pool.get(random.nextInt(pool.size()));
    }
}
