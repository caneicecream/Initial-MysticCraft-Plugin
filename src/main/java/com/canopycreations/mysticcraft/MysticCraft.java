package com.canopycreations.mysticcraft;

import com.canopycreations.mysticcraft.clans.ClanManager;
import com.canopycreations.mysticcraft.commands.ClanCommand;
import com.canopycreations.mysticcraft.commands.LoreCommand;
import com.canopycreations.mysticcraft.commands.QuestCommand;
import com.canopycreations.mysticcraft.commands.MysticAdminCommand;
import com.canopycreations.mysticcraft.commands.RaceCommand;
import com.canopycreations.mysticcraft.commands.VampireCommand;
import com.canopycreations.mysticcraft.commands.WerewolfCommand;
import com.canopycreations.mysticcraft.commands.WitchCommand;
import com.canopycreations.mysticcraft.data.DataStore;
import com.canopycreations.mysticcraft.integrations.bluemap.BlueMapBridge;
import com.canopycreations.mysticcraft.integrations.papi.MysticPlaceholders;
import com.canopycreations.mysticcraft.items.LoreBooks;
import com.canopycreations.mysticcraft.items.MysticItems;
import com.canopycreations.mysticcraft.listeners.CurseTriggerListener;
import com.canopycreations.mysticcraft.listeners.JoinQuitListener;
import com.canopycreations.mysticcraft.listeners.DiscoveryListener;
import com.canopycreations.mysticcraft.listeners.LootListener;
import com.canopycreations.mysticcraft.listeners.RitualListener;
import com.canopycreations.mysticcraft.listeners.TotemListener;
import com.canopycreations.mysticcraft.listeners.WhiteOakListener;
import com.canopycreations.mysticcraft.world.LandmarkManager;
import com.canopycreations.mysticcraft.world.TownGenerator;
import com.canopycreations.mysticcraft.listeners.TerritoryListener;
import com.canopycreations.mysticcraft.listeners.TurningListener;
import com.canopycreations.mysticcraft.listeners.VampireListener;
import com.canopycreations.mysticcraft.listeners.WerewolfListener;
import com.canopycreations.mysticcraft.managers.BloodlineManager;
import com.canopycreations.mysticcraft.managers.BoundaryManager;
import com.canopycreations.mysticcraft.managers.HumanityManager;
import com.canopycreations.mysticcraft.managers.MoonPhaseManager;
import com.canopycreations.mysticcraft.managers.OriginalsManager;
import com.canopycreations.mysticcraft.managers.CodexManager;
import com.canopycreations.mysticcraft.managers.LorekeeperManager;
import com.canopycreations.mysticcraft.managers.ProgenitorManager;
import com.canopycreations.mysticcraft.managers.QuestManager;
import com.canopycreations.mysticcraft.managers.RaceManager;
import com.canopycreations.mysticcraft.managers.SpellManager;
import com.canopycreations.mysticcraft.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class MysticCraft extends JavaPlugin {

    private DataStore dataStore;
    private RaceManager raceManager;
    private HumanityManager humanityManager;
    private MoonPhaseManager moonPhaseManager;
    private SpellManager spellManager;
    private BoundaryManager boundaryManager;
    private MysticItems mysticItems;
    private OriginalsManager originalsManager;
    private BloodlineManager bloodlineManager;
    private TurningListener turningListener;

    private VampireListener vampireListener;
    private WerewolfListener werewolfListener;
    private ClanManager clanManager;
    private ProgenitorManager progenitorManager;
    private TerritoryListener territoryListener;
    private CodexManager codexManager;
    private QuestManager questManager;
    private LorekeeperManager lorekeeperManager;
    private DiscoveryListener discoveryListener;
    private RitualListener ritualListener;
    private TotemListener totemListener;
    private LoreBooks loreBooks;
    private BlueMapBridge blueMapBridge; // null unless BlueMap is installed
    private LandmarkManager landmarkManager;
    private TownGenerator townGenerator;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.dataStore = new DataStore(this);
        this.mysticItems = new MysticItems(this);
        this.loreBooks = new LoreBooks(this);
        this.raceManager = new RaceManager(this);
        this.humanityManager = new HumanityManager(this);
        this.moonPhaseManager = new MoonPhaseManager(this);
        this.spellManager = new SpellManager(this);
        this.boundaryManager = new BoundaryManager(this);
        this.originalsManager = new OriginalsManager(this);
        this.bloodlineManager = new BloodlineManager(this);
        this.clanManager = new ClanManager(this);
        this.progenitorManager = new ProgenitorManager(this);
        this.questManager = new QuestManager(this);
        this.codexManager = new CodexManager(this);
        this.lorekeeperManager = new LorekeeperManager(this);
        this.landmarkManager = new LandmarkManager(this);
        this.townGenerator = new TownGenerator(this);

        this.vampireListener = new VampireListener(this);
        this.werewolfListener = new WerewolfListener(this);
        this.turningListener = new TurningListener(this);
        this.territoryListener = new TerritoryListener(this);
        this.discoveryListener = new DiscoveryListener(this);
        this.ritualListener = new RitualListener(this);
        this.totemListener = new TotemListener(this);

        Bukkit.getPluginManager().registerEvents(vampireListener, this);
        Bukkit.getPluginManager().registerEvents(werewolfListener, this);
        Bukkit.getPluginManager().registerEvents(turningListener, this);
        Bukkit.getPluginManager().registerEvents(territoryListener, this);
        Bukkit.getPluginManager().registerEvents(discoveryListener, this);
        Bukkit.getPluginManager().registerEvents(ritualListener, this);
        Bukkit.getPluginManager().registerEvents(totemListener, this);
        Bukkit.getPluginManager().registerEvents(new LootListener(this), this);
        Bukkit.getPluginManager().registerEvents(new WhiteOakListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CurseTriggerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new JoinQuitListener(this), this);

        getCommand("race").setExecutor(new RaceCommand(this));
        getCommand("vampire").setExecutor(new VampireCommand(this));
        getCommand("werewolf").setExecutor(new WerewolfCommand(this));
        getCommand("witch").setExecutor(new WitchCommand(this));
        getCommand("mystic").setExecutor(new MysticAdminCommand(this));
        getCommand("clan").setExecutor(new ClanCommand(this));
        getCommand("lore").setExecutor(new LoreCommand(this));
        getCommand("quest").setExecutor(new QuestCommand(this));


        setupBlueMap();
        setupPlaceholders();

        startTasks();

        getLogger().info("MysticCraft has awoken. Vampires, werewolves, and witches now walk among your players.");
    }

    @Override
    public void onDisable() {
        if (dataStore != null) {
            dataStore.saveAll();
        }
        if (clanManager != null) {
            clanManager.save();
        }
        if (codexManager != null) {
            codexManager.save();
        }
        if (questManager != null) {
            questManager.save();
        }
        if (landmarkManager != null) {
            landmarkManager.save();
        }
        getLogger().info("MysticCraft has gone dormant.");
    }

    /**
     * BlueMap is optional. If it isn't installed this does nothing, and no
     * BlueMap class is ever loaded - so the plugin runs fine without it.
     */
    private void setupBlueMap() {
        if (Bukkit.getPluginManager().getPlugin("BlueMap") == null) {
            getLogger().info("BlueMap not detected - skipping map integration.");
            return;
        }
        try {
            this.blueMapBridge = new BlueMapBridge(this);
            blueMapBridge.register();
        } catch (Throwable t) {
            getLogger().log(java.util.logging.Level.WARNING,
                    "Found BlueMap but couldn't hook into it. Map integration disabled; "
                            + "everything else works normally.", t);
            this.blueMapBridge = null;
        }
    }

    /**
     * Registers the PlaceholderAPI expansion. This is what makes MysticCraft
     * data visible to HUDEngine, CommandPanels and EssentialsChat - one
     * integration instead of three.
     */
    private void setupPlaceholders() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().warning("PlaceholderAPI not found. HUDEngine and CommandPanels "
                    + "both need it to read MysticCraft data - install it to enable race HUDs and GUIs.");
            return;
        }
        try {
            new MysticPlaceholders(this).register();
            getLogger().info("PlaceholderAPI detected - %mysticcraft_...% placeholders registered.");
        } catch (Throwable t) {
            getLogger().log(java.util.logging.Level.WARNING,
                    "Found PlaceholderAPI but couldn't register the expansion.", t);
        }
    }

    /** Redraws map markers. Safe to call when claims change; no-op without BlueMap. */
    public void refreshMap() {
        if (blueMapBridge != null) blueMapBridge.redraw();
    }

    private void startTasks() {
        // Main per-second tick: sunlight, venom, ripper checks, ring detection, boundary enforcement.
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerData data = raceManager.getData(player);

                updateDaylightRingState(player, data);

                vampireListener.tickSunlight(player, data);
                humanityManager.tickRipperCheck(player);
                werewolfListener.tickToxin(player);
                boundaryManager.enforce(player);
                turningListener.tickTransitionDeadline(player, data);
            }
        }, 20L, 20L);

        // Moon phase check - forces shifts on full moons.
        int moonInterval = getConfig().getInt("werewolf.full-moon-check-interval-ticks", 1200);
        Bukkit.getScheduler().runTaskTimer(this, () -> moonPhaseManager.tick(), 100L, moonInterval);

        // Dawn check - reverts forced shifts. Runs every 10 seconds so it doesn't miss the window.
        Bukkit.getScheduler().runTaskTimer(this, () -> moonPhaseManager.revertAtDawn(), 100L, 200L);

        // Werewolf scent detection - runs every 5 seconds (BloodlineManager assumes this interval).
        Bukkit.getScheduler().runTaskTimer(this, () -> bloodlineManager.tickScent(), 200L, 100L);

        // Ritual detection: eye-contact compulsion and howling. Runs at 4 ticks
        // so holding a gaze feels responsive rather than laggy.
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                ritualListener.tickEyeContact(p);
                ritualListener.tickHowl(p);
            }
        }, 40L, 4L);

        // Territory totem glow, every 2 seconds.
        Bukkit.getScheduler().runTaskTimer(this, () -> totemListener.tickTotemAmbience(), 100L, 40L);

        // Race-flavored territory effects, every 5 seconds.
        Bukkit.getScheduler().runTaskTimer(this, () -> territoryListener.tickTerritoryEffects(), 200L, 100L);

        // Ambient lore discovery + passive quest objectives, every 10 seconds.
        if (getConfig().getBoolean("lore.discovery-enabled", true)) {
            int interval = getConfig().getInt("lore.ambient-check-interval-ticks", 200);
            Bukkit.getScheduler().runTaskTimer(this, () -> {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    discoveryListener.tickAmbientDiscovery(p);
                    landmarkManager.tick(p);
                }
            }, 300L, interval);
        }
    }

    private void updateDaylightRingState(Player player, PlayerData data) {
        boolean wearing = false;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (mysticItems.hasTag(stack, MysticItems.TAG_DAYLIGHT_RING)) {
                wearing = true;
                break;
            }
        }
        if (wearing != data.isDaylightRingEquipped()) {
            data.setDaylightRingEquipped(wearing);
        }
    }

    public void registerBoundary(Location center, double radius, long expiresAtMillis) {
        boundaryManager.register(center, radius, expiresAtMillis);
    }

    public DataStore getDataStore() {
        return dataStore;
    }

    public RaceManager getRaceManager() {
        return raceManager;
    }

    public HumanityManager getHumanityManager() {
        return humanityManager;
    }

    public MoonPhaseManager getMoonPhaseManager() {
        return moonPhaseManager;
    }

    public SpellManager getSpellManager() {
        return spellManager;
    }

    public MysticItems getMysticItems() {
        return mysticItems;
    }

    public WerewolfListener getWerewolfListener() {
        return werewolfListener;
    }

    public ClanManager getClanManager() {
        return clanManager;
    }

    public ProgenitorManager getProgenitorManager() {
        return progenitorManager;
    }

    public TerritoryListener getTerritoryListener() {
        return territoryListener;
    }

    public CodexManager getCodexManager() {
        return codexManager;
    }

    public QuestManager getQuestManager() {
        return questManager;
    }

    public LorekeeperManager getLorekeeperManager() {
        return lorekeeperManager;
    }

    public LoreBooks getLoreBooks() {
        return loreBooks;
    }

    public LandmarkManager getLandmarkManager() {
        return landmarkManager;
    }

    public TownGenerator getTownGenerator() {
        return townGenerator;
    }

    public RitualListener getRitualListener() {
        return ritualListener;
    }

    public TotemListener getTotemListener() {
        return totemListener;
    }

    public OriginalsManager getOriginalsManager() {
        return originalsManager;
    }

    public BloodlineManager getBloodlineManager() {
        return bloodlineManager;
    }
}
