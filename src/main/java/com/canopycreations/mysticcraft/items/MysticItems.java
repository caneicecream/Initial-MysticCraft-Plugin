package com.canopycreations.mysticcraft.items;

import com.canopycreations.mysticcraft.MysticCraft;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * Factory + identifier for all the lore items: Daylight Rings, Wolfsbane,
 * Vervain, Silver weapons, Stakes, Witch herbs and wands. Every item is
 * tagged with a PersistentDataContainer key so we never rely on display
 * name/lore matching (which breaks with resource packs / renames).
 */
public class MysticItems {

    public static final String TAG_DAYLIGHT_RING = "daylight_ring";
    public static final String TAG_WOLFSBANE = "wolfsbane";
    public static final String TAG_VERVAIN = "vervain";
    public static final String TAG_SILVER_WEAPON = "silver_weapon";
    public static final String TAG_STAKE = "stake";
    public static final String TAG_WITCH_HERB = "witch_herb";
    public static final String TAG_WITCH_WAND = "witch_wand";
    public static final String TAG_WHITE_OAK_STAKE = "white_oak_stake";

    private final MysticCraft plugin;
    private final NamespacedKey key;

    public MysticItems(MysticCraft plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "mystic_item");
    }

    private ItemStack build(Material material, String tag, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, tag);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack daylightRing() {
        Material mat = Material.matchMaterial(plugin.getConfig().getString("items.daylight-ring-material", "GOLD_NUGGET"));
        return build(mat != null ? mat : Material.GOLD_NUGGET, TAG_DAYLIGHT_RING,
                "§6" + plugin.getConfig().getString("items.daylight-ring-name", "Daylight Ring"),
                List.of("§7Enchanted by a witch.", "§7Wear in an accessory slot or keep on hotbar", "§7to walk in sunlight unharmed."));
    }

    public ItemStack wolfsbane() {
        Material mat = Material.matchMaterial(plugin.getConfig().getString("items.wolfsbane-material", "WITHER_ROSE"));
        return build(mat != null ? mat : Material.WITHER_ROSE, TAG_WOLFSBANE,
                "§2Wolfsbane",
                List.of("§7Deadly to werewolves.", "§7Thrown or brewed, it burns and weakens the wolf."));
    }

    public ItemStack vervain() {
        Material mat = Material.matchMaterial(plugin.getConfig().getString("items.vervain-material", "SPORE_BLOSSOM"));
        return build(mat != null ? mat : Material.SPORE_BLOSSOM, TAG_VERVAIN,
                "§dVervain",
                List.of("§7Burns vampires on contact.", "§7Worn as jewelry, it blocks compulsion."));
    }

    public ItemStack silverWeapon() {
        Material mat = Material.matchMaterial(plugin.getConfig().getString("items.silver-weapon-material", "IRON_SWORD"));
        return build(mat != null ? mat : Material.IRON_SWORD, TAG_SILVER_WEAPON,
                "§fSilver Blade",
                List.of("§7Deals extra damage to werewolves,", "§7shifted or not."));
    }

    public ItemStack stake() {
        Material mat = Material.matchMaterial(plugin.getConfig().getString("items.stake-material", "STICK"));
        return build(mat != null ? mat : Material.STICK, TAG_STAKE,
                "§8Wooden Stake",
                List.of("§7A stake to the heart is final", "§7for any vampire."));
    }

    public ItemStack witchHerb() {
        Material mat = Material.matchMaterial(plugin.getConfig().getString("items.witch-herb-material", "NETHER_WART"));
        return build(mat != null ? mat : Material.NETHER_WART, TAG_WITCH_HERB,
                "§aSpell Herb",
                List.of("§7Consumed when casting spells.", "§7No herb, no magic."));
    }

    public ItemStack witchWand() {
        Material mat = Material.matchMaterial(plugin.getConfig().getString("items.witch-wand-material", "BLAZE_ROD"));
        ItemStack item = build(mat != null ? mat : Material.BLAZE_ROD, TAG_WITCH_WAND,
                "§5Grimoire Wand",
                List.of("§7Channel your bloodline's power.", "§7Use /witch cast <spell> while holding this."));
        return item;
    }

    public ItemStack whiteOakStake() {
        return build(Material.PALE_OAK_PLANKS, TAG_WHITE_OAK_STAKE,
                "§f§lWhite Oak Stake",
                List.of("§7Cut from the tree the Ash-Mother drew on.",
                        "§4Kills any vampire permanently.",
                        "§4Even one that shouldn't be killable.",
                        "§8Consumed on use. There are only so many."));
    }

    public boolean isWhiteOakStake(ItemStack item) {
        return hasTag(item, TAG_WHITE_OAK_STAKE);
    }

    public boolean hasTag(ItemStack item, String tag) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        String stored = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        return tag.equals(stored);
    }

    public NamespacedKey getKey() {
        return key;
    }
}
