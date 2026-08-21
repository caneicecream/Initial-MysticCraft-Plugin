package com.canopycreations.mysticcraft.integrations.bluemap;

import com.canopycreations.mysticcraft.MysticCraft;
import com.canopycreations.mysticcraft.clans.Clan;
import com.canopycreations.mysticcraft.world.Landmark;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;

/**
 * Draws MysticCraft onto BlueMap.
 *
 * WHY THIS EXISTS: every claim addon in BlueMap's directory hooks a specific
 * third-party plugin - Towny, GriefPrevention, Lands, HuskTowns and so on.
 * MysticCraft's clans are native, so none of them can see our data. This is
 * the bridge that can.
 *
 * Two things get rendered:
 *   - Clan territory, as filled chunk shapes coloured by clan kind
 *   - Ashfall's landmarks, as points of interest
 *
 * SECRECY: a map that gives everything away undermines a server built on
 * hidden knowledge. So secret clans are never drawn, sealed landmarks stay
 * off the map, and the White Oak is only shown if you deliberately enable it
 * in config - its location being common knowledge changes the whole political
 * situation around it.
 */
public class BlueMapBridge {

    private static final String TERRITORY_SET = "mysticcraft-territory";
    private static final String LANDMARK_SET  = "mysticcraft-landmarks";

    private final MysticCraft plugin;

    public BlueMapBridge(MysticCraft plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers with BlueMap. Markers aren't persistent across BlueMap
     * reloads, so this re-draws everything each time the API comes up.
     */
    public void register() {
        BlueMapAPI.onEnable(api -> {
            try {
                redraw(api);
                plugin.getLogger().info("BlueMap detected - territory and landmarks drawn.");
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed drawing MysticCraft markers on BlueMap.", e);
            }
        });
    }

    /** Redraws everything. Safe to call whenever claims change. */
    public void redraw() {
        BlueMapAPI.getInstance().ifPresent(api -> {
            try {
                redraw(api);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed refreshing BlueMap markers.", e);
            }
        });
    }

    private void redraw(BlueMapAPI api) {
        MarkerSet territory = MarkerSet.builder()
                .label("Territory")
                .toggleable(true)
                .defaultHidden(false)
                .build();

        MarkerSet landmarks = MarkerSet.builder()
                .label("Ashfall")
                .toggleable(true)
                .defaultHidden(false)
                .build();

        drawTerritory(territory);
        drawLandmarks(landmarks);

        // Attach to every map of every world.
        for (var world : api.getWorlds()) {
            for (BlueMapMap map : world.getMaps()) {
                map.getMarkerSets().put(TERRITORY_SET, territory);
                map.getMarkerSets().put(LANDMARK_SET, landmarks);
            }
        }
    }

    // ------------------------------------------------------------------
    // Territory
    // ------------------------------------------------------------------
    private void drawTerritory(MarkerSet set) {
        if (!plugin.getConfig().getBoolean("bluemap.show-territory", true)) return;

        for (Clan clan : plugin.getClanManager().getAllClans()) {
            // A secret society that shows up on a public map isn't secret.
            if (clan.isSecret()) continue;
            if (clan.getClaimedChunks().isEmpty()) continue;

            Color fill = fillFor(clan);
            Color line = lineFor(clan);
            int index = 0;

            for (String chunkKey : clan.getClaimedChunks()) {
                String[] parts = chunkKey.split(":");
                if (parts.length != 3) continue;

                World world = Bukkit.getWorld(parts[0]);
                if (world == null) continue;

                try {
                    int cx = Integer.parseInt(parts[1]);
                    int cz = Integer.parseInt(parts[2]);

                    // One square per claimed chunk. BlueMap merges them
                    // visually when they're adjacent.
                    Shape shape = Shape.createRect(
                            cx * 16, cz * 16,
                            cx * 16 + 16, cz * 16 + 16);

                    ShapeMarker marker = ShapeMarker.builder()
                            .label(clan.getName())
                            .shape(shape, world.getSeaLevel() + 12)
                            .lineColor(line)
                            .fillColor(fill)
                            .lineWidth(1)
                            .depthTestEnabled(false)
                            .detail(territoryDetail(clan))
                            .build();

                    set.getMarkers().put(
                            "clan-" + clan.getName().toLowerCase(Locale.ROOT) + "-" + (index++),
                            marker);
                } catch (NumberFormatException ignored) {
                    // malformed chunk key; skip it
                }
            }
        }
    }

    private String territoryDetail(Clan clan) {
        String leader = Bukkit.getOfflinePlayer(clan.getLeader()).getName();
        return "<div style=\"font-family:serif\">"
                + "<b style=\"font-size:1.1em\">" + escape(clan.getName()) + "</b><br>"
                + "<i>" + clan.getKind().getLabel() + "</i><br><br>"
                + (clan.getDescription().isBlank() ? "" : escape(clan.getDescription()) + "<br><br>")
                + "Led by " + escape(leader == null ? "someone" : leader) + "<br>"
                + clan.getMemberCount() + " member" + (clan.getMemberCount() == 1 ? "" : "s") + "<br>"
                + clan.getClaimCount() + " chunks held"
                + "</div>";
    }

    /** Territory colour follows the race that holds it. */
    private Color fillFor(Clan clan) {
        return switch (clan.getKind()) {
            case COURT  -> new Color(150,  20,  20, 0.35f);
            case PACK   -> new Color(200, 140,  40, 0.35f);
            case COVEN  -> new Color(140,  60, 190, 0.35f);
            case ORDER  -> new Color(225, 225, 225, 0.30f);
            default     -> new Color(120, 120, 120, 0.28f);
        };
    }

    private Color lineFor(Clan clan) {
        return switch (clan.getKind()) {
            case COURT  -> new Color(190,  30,  40, 0.9f);
            case PACK   -> new Color(230, 170,  60, 0.9f);
            case COVEN  -> new Color(175,  95, 220, 0.9f);
            case ORDER  -> new Color(255, 255, 255, 0.85f);
            default     -> new Color(160, 160, 160, 0.8f);
        };
    }

    // ------------------------------------------------------------------
    // Landmarks
    // ------------------------------------------------------------------
    private void drawLandmarks(MarkerSet set) {
        if (!plugin.getConfig().getBoolean("bluemap.show-landmarks", true)) return;

        Set<Landmark> hidden = hiddenLandmarks();

        for (Landmark landmark : Landmark.values()) {
            if (hidden.contains(landmark)) continue;

            Location loc = plugin.getLandmarkManager().getLocation(landmark);
            if (loc == null) continue;

            POIMarker marker = POIMarker.builder()
                    .label(landmark.getDisplayName())
                    .position(loc.getX(), loc.getY() + 2, loc.getZ())
                    .maxDistance(3000)
                    .detail(landmarkDetail(landmark))
                    .build();

            set.getMarkers().put("landmark-" + landmark.name().toLowerCase(Locale.ROOT), marker);
        }
    }

    /**
     * Some places shouldn't be handed to people on a map.
     *
     * The Tomb is sealed content. The White Oak is the single most
     * consequential object in the world - if everyone knows exactly where it
     * is from day one, the politics around guarding it never develop. Both
     * can be switched on in config if you'd rather they were public.
     */
    private Set<Landmark> hiddenLandmarks() {
        Set<Landmark> hidden = new HashSet<>();
        if (!plugin.getConfig().getBoolean("bluemap.reveal-the-tomb", false)) {
            hidden.add(Landmark.THE_TOMB);
        }
        if (!plugin.getConfig().getBoolean("bluemap.reveal-the-white-oak", false)) {
            hidden.add(Landmark.THE_WHITE_OAK);
        }
        return hidden;
    }

    private String landmarkDetail(Landmark landmark) {
        return "<div style=\"font-family:serif;max-width:280px\">"
                + "<b style=\"font-size:1.1em\">" + escape(landmark.getDisplayName()) + "</b><br><br>"
                + "<i>" + escape(landmark.getShortDescription()) + "</i>"
                + "</div>";
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
