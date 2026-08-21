package com.canopycreations.mysticcraft.items;

import com.canopycreations.mysticcraft.MysticCraft;
import com.canopycreations.mysticcraft.lore.LoreFragment;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Lore as a physical object.
 *
 * A fragment you unlock by typing a command is trivia. A fragment that
 * exists as a written book in your inventory is a THING: you can hide it in
 * a chest, carry it into enemy territory, hand it to an ally, lose it when
 * you die, or take it off someone's corpse. That's what turns "knowing
 * something" into a real position in the world's politics.
 *
 * Reading a lore book unlocks the fragment in your codex permanently - so
 * the book can then be given away, and the knowledge stays with you. Secrets
 * spread, and they spread through player choices.
 */
public class LoreBooks {

    private final MysticCraft plugin;
    private final NamespacedKey key;

    public LoreBooks(MysticCraft plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "lore_fragment");
    }

    public NamespacedKey getKey() {
        return key;
    }

    /** Builds the physical book for a fragment. */
    public ItemStack create(LoreFragment fragment) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();

        meta.setTitle("§f" + fragment.getTitle());
        meta.setAuthor(authorFor(fragment));
        meta.setPages(paginate(fragment.getText()));
        meta.setLore(List.of(
                "§8A record of something that happened.",
                "§8Right-click to read.",
                "§8It will stay with you afterward."
        ));
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, fragment.name());
        book.setItemMeta(meta);
        return book;
    }

    /** In-world authorship, so found books feel like they came from someone. */
    private String authorFor(LoreFragment fragment) {
        return switch (fragment) {
            case THE_BALANCE, THE_CURE -> "§7hand unknown";
            case SEVEN_KNIVES -> "§7scratched into stone";
            case THE_ASH_MOTHER -> "§7burned into bark";
            case THE_UNBURIED -> "§7torn from a longer book";
            case THE_SWITCH, THE_THIRST -> "§7a vampire, name removed";
            case THE_SLEEPING_CURSE, THE_VENOM -> "§7a survivor";
            case THE_PRICE, CHANNELING -> "§7a hedgewitch's notes";
            default -> "§7anonymous";
        };
    }

    /** Splits fragment text into readable book pages. */
    private List<String> paginate(String text) {
        List<String> pages = new ArrayList<>();
        String[] lines = text.strip().split("\n");
        StringBuilder page = new StringBuilder();
        int lineCount = 0;

        for (String line : lines) {
            if (lineCount >= 11) {
                pages.add(page.toString());
                page = new StringBuilder();
                lineCount = 0;
            }
            page.append(line.strip()).append("\n");
            lineCount++;
        }
        if (!page.isEmpty()) pages.add(page.toString());
        return pages;
    }

    /** Reads the fragment tag off a book, or null if it isn't one. */
    public LoreFragment getFragment(ItemStack item) {
        if (item == null || item.getType() != Material.WRITTEN_BOOK || !item.hasItemMeta()) return null;
        String stored = item.getItemMeta().getPersistentDataContainer()
                .get(key, PersistentDataType.STRING);
        return LoreFragment.fromString(stored);
    }

    public boolean isLoreBook(ItemStack item) {
        return getFragment(item) != null;
    }
}
