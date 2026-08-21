package com.canopycreations.mysticcraft.world;

import com.canopycreations.mysticcraft.MysticCraft;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Builds the town of Ashfall.
 *
 * Generated in stages across ticks rather than all at once - a 250x250 build
 * done synchronously will hang the server for several seconds and may trip
 * watchdog. Each stage reports progress so you can watch it go up.
 *
 * Seeded: the same seed produces the same town, so you can re-roll until you
 * get a layout you like and then keep it. Landmark POSITIONS are fixed
 * (the mechanics depend on a coherent geography), but building footprints,
 * materials, weathering, filler houses and road routing all vary.
 */
public class TownGenerator {

    private final MysticCraft plugin;

    /**
     * Which landmarks sit on street frontage in the town proper, and which
     * belong on the outskirts. A cemetery and a quarry on the high street
     * would be strange; a tavern anywhere else would be.
     */
    /** The bar belongs downtown, on the main drag. */
    private static final Landmark[] DOWNTOWN_LANDMARKS = { Landmark.THE_KETTLE };

    /** The big houses sit out in the residential streets, on deep lots. */
    private static final Landmark[] RESIDENTIAL_LANDMARKS = {
            Landmark.THE_BOARDING_HOUSE,
            Landmark.LOCKRIDGE_MANOR,
            Landmark.THE_HEDGE_HOUSE
    };

    /** Fixed offsets for the things that need space and distance. */
    private static final Map<Landmark, int[]> OUTSKIRTS = new EnumMap<>(Landmark.class);
    static {
        // Real distances. The church is a walk. The quarry is a hike. That
        // is how these things sit relative to a town in reality.
        OUTSKIRTS.put(Landmark.THE_BURNED_CHURCH, new int[]{  -95, -305});
        OUTSKIRTS.put(Landmark.THE_TOMB,          new int[]{  -95, -305});
        OUTSKIRTS.put(Landmark.THE_OLD_CEMETERY,  new int[]{ -140, -355});
        OUTSKIRTS.put(Landmark.THE_WHITE_OAK,     new int[]{  160, -370});
        OUTSKIRTS.put(Landmark.THE_QUARRY,        new int[]{ -430,   85});
        OUTSKIRTS.put(Landmark.WICKER_BRIDGE,     new int[]{  400,  125});
    }

    public TownGenerator(MysticCraft plugin) {
        this.plugin = plugin;
    }

    /**
     * Kicks off a staged build. Streets go down first, then buildings are
     * placed onto plots derived from those streets - so everything fronts a
     * road and faces it.
     */
    public void generate(Location center, Player feedback, long seed) {
        World world = center.getWorld();
        Random rng = new Random(seed);
        Blueprint bp = new Blueprint(world, rng);
        TownPlan plan = new TownPlan(center.getBlockX(), center.getBlockZ(), rng);
        Occupancy occ = new Occupancy();

        List<Runnable> stages = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        // --- Stage 0: clear the town's footprint ---
        // Downtown is fully cleared, residential is thinned, and the edge
        // keeps enough trees that the town fades into woodland.
        stages.add(() -> bp.clearDistrict(center.getBlockX(), center.getBlockZ(),
                TownPlan.downtownRadius() + 15, 0.0, rng));
        labels.add("§7clearing the town centre");

        stages.add(() -> bp.clearDistrict(center.getBlockX(), center.getBlockZ(),
                TownPlan.residentialRadius(), 0.28, rng));
        labels.add("§7clearing the residential streets");

        // --- Stage 1: the street network. Everything else hangs off this. ---
        stages.add(() -> {
            for (TownPlan.Street st : plan.getStreets()) {
                occ.reserveStreet(st);
                bp.road(st.x1(), st.z1(), st.x2(), st.z2(),
                        st.width(),
                        st.major() ? Material.DIRT_PATH : Material.COARSE_DIRT,
                        Material.COBBLESTONE);
            }
        });
        labels.add("§7laying the streets");

        // --- Stage 2: the square, at the crossroads ---
        stages.add(() -> {
            Location spot = center.clone();
            spot.setY(Blueprint.groundY(world, spot.getBlockX(), spot.getBlockZ()));
            occ.reserve(spot.getBlockX(), spot.getBlockZ(), 22, 22, 0, "square");
            bp.clearVegetation(spot.getBlockX(), spot.getBlockZ(), 26);
            square(world, bp, rng, spot.getBlockX(), spot.getBlockY(), spot.getBlockZ());
            plugin.getLandmarkManager().setLocation(Landmark.TOWN_SQUARE, spot, 34);
        });
        labels.add(Landmark.TOWN_SQUARE.getFormattedName());

        // --- Stage 3a: downtown landmarks ---
        for (Landmark landmark : DOWNTOWN_LANDMARKS) {
            stages.add(() -> {
                TownPlan.Plot plot = takeBuildablePlot(plan, occ, world,
                        TownPlan.District.DOWNTOWN, 4, 5, landmark.name());
                if (plot == null) return;

                Location spot = new Location(world, plot.x(), 0, plot.z());
                spot.setY(Occupancy.averageGround(world, plot.x(), plot.z(),
                        plot.width(), plot.depth()));
                try {
                    bp.clearVegetation(plot.x(), plot.z(), clearRadiusFor(landmark));
                    buildOnPlot(world, bp, rng, spot, landmark, plot);
                    plugin.getLandmarkManager().setLocation(landmark, spot, radiusFor(landmark));
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed building " + landmark + ": " + e.getMessage());
                }
            });
            labels.add(landmark.getFormattedName());
        }

        // --- Stage 3b: the big residential houses ---
        for (Landmark landmark : RESIDENTIAL_LANDMARKS) {
            stages.add(() -> {
                TownPlan.Plot plot = takeBuildablePlot(plan, occ, world,
                        TownPlan.District.RESIDENTIAL, 5, 5, landmark.name());
                if (plot == null) return;

                Location spot = new Location(world, plot.x(), 0, plot.z());
                spot.setY(Occupancy.averageGround(world, plot.x(), plot.z(),
                        plot.width(), plot.depth()));
                try {
                    bp.clearVegetation(plot.x(), plot.z(), clearRadiusFor(landmark));
                    buildOnPlot(world, bp, rng, spot, landmark, plot);
                    plugin.getLandmarkManager().setLocation(landmark, spot, radiusFor(landmark));
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed building " + landmark + ": " + e.getMessage());
                }
            });
            labels.add(landmark.getFormattedName());
        }

        // --- Stage 4: outlying landmarks at fixed offsets ---
        for (Map.Entry<Landmark, int[]> entry : OUTSKIRTS.entrySet()) {
            Landmark landmark = entry.getKey();
            int[] off = entry.getValue();
            stages.add(() -> {
                Location spot = center.clone().add(off[0], 0, off[1]);
                spot.setY(Blueprint.groundY(world, spot.getBlockX(), spot.getBlockZ()));
                int r = clearRadiusFor(landmark);
                occ.reserve(spot.getBlockX(), spot.getBlockZ(), r, r, 0, landmark.name());
                try {
                    bp.clearVegetation(spot.getBlockX(), spot.getBlockZ(), r);
                    build(world, bp, rng, spot, landmark);
                    plugin.getLandmarkManager().setLocation(landmark, spot, radiusFor(landmark));
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed building " + landmark + ": " + e.getMessage());
                }
            });
            labels.add(landmark.getFormattedName());
        }

        // --- Stage 5: short spurs from the nearest country lane ---
        // These connect a landmark to the existing lane network rather than
        // running all the way back to the square, which is what produced the
        // spoke pattern in the first place.
        stages.add(() -> {
            for (int[] off : OUTSKIRTS.values()) {
                int tx = center.getBlockX() + off[0];
                int tz = center.getBlockZ() + off[1];

                // Find the closest point on any street and join to that.
                int bx = center.getBlockX(), bz = center.getBlockZ();
                double best = Double.MAX_VALUE;
                for (TownPlan.Street st : plan.getStreets()) {
                    for (double t = 0; t <= 1.0; t += 0.1) {
                        int sx = (int) Math.round(st.x1() + (st.x2() - st.x1()) * t);
                        int sz = (int) Math.round(st.z1() + (st.z2() - st.z1()) * t);
                        double d = Math.hypot(sx - tx, sz - tz);
                        if (d < best) { best = d; bx = sx; bz = sz; }
                    }
                }
                bp.road(bx, bz, tx, tz, 2, Material.COARSE_DIRT, Material.COBBLESTONE);
            }
        });
        labels.add("§7spurs to the outlying properties");

        // --- Stage 6: fill remaining street frontage with houses ---
        stages.add(() -> {
            for (TownPlan.Plot plot : new ArrayList<>(plan.getPlots())) {
                if (!plan.shouldBuild(plot, rng)) continue;

                int hw = plot.width() - 1, hl = plot.depth() - 1;

                // Uneven or waterlogged ground: leave a gap rather than
                // producing a half-buried, half-stilted building.
                int maxDrop = plot.district() == TownPlan.District.DOWNTOWN ? 3 : 5;
                if (!Occupancy.isBuildable(world, plot.x(), plot.z(), hw, hl, maxDrop)) continue;

                // Anything already standing here wins.
                int margin = plot.district() == TownPlan.District.DOWNTOWN ? 1 : 3;
                if (!occ.reserve(plot.x(), plot.z(), hw, hl, margin, "house")) continue;

                Location spot = new Location(world, plot.x(), 0, plot.z());
                spot.setY(Occupancy.averageGround(world, plot.x(), plot.z(), hw, hl));
                if (plugin.getLandmarkManager().landmarkAt(spot) != null) continue;

                try {
                    bp.clearVegetation(plot.x(), plot.z(), Math.max(plot.width(), plot.depth()) + 4);
                    if (plot.district() == TownPlan.District.DOWNTOWN) {
                        downtownShop(world, bp, rng, spot, plot);
                    } else {
                        fillerHouse(world, bp, rng, spot, plot);
                    }
                } catch (Exception ignored) {
                    // one failed cottage shouldn't abort the town
                }
            }
        });
        labels.add("§7houses along the streets");

        // Run the stages one per tick so the server keeps breathing.
        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                if (index >= stages.size()) {
                    if (feedback != null) {
                        feedback.sendMessage("");
                        feedback.sendMessage("§aAshfall stands. §8seed: §7" + seed);
                        feedback.sendMessage("§8Re-roll elsewhere with §7/mystic town confirm <seed>§8,");
                        feedback.sendMessage("§8or re-point landmarks onto your own builds with");
                        feedback.sendMessage("§8§7/mystic landmark set <name>§8.");
                    }
                    plugin.getLandmarkManager().save();
                    cancel();
                    return;
                }
                stages.get(index).run();
                if (feedback != null && index < labels.size()) {
                    feedback.sendActionBar("§8Building Ashfall... " + labels.get(index));
                }
                index++;
            }
        }.runTaskTimer(plugin, 5L, 2L);
    }

    /**
     * Pulls plots until one is found that is unoccupied AND on ground flat
     * enough to build on, reserving it. Returns null if the district is full.
     */
    private TownPlan.Plot takeBuildablePlot(TownPlan plan, Occupancy occ, World world,
                                            TownPlan.District district,
                                            int minW, int minD, String owner) {
        for (int attempt = 0; attempt < 40; attempt++) {
            TownPlan.Plot plot = plan.takePlot(district, minW, minD);
            if (plot == null) return null;

            int hw = plot.width() - 1, hl = plot.depth() - 1;
            if (!Occupancy.isBuildable(world, plot.x(), plot.z(), hw, hl, 5)) continue;
            // Landmarks get a wide berth so they read as important.
            if (!occ.reserve(plot.x(), plot.z(), hw + 3, hl + 3, 4, owner)) continue;
            return plot;
        }
        return null;
    }

    /** Routes an in-town landmark to its builder, oriented to its plot. */
    private void buildOnPlot(World world, Blueprint bp, Random rng,
                             Location at, Landmark landmark, TownPlan.Plot plot) {
        int x = at.getBlockX(), y = at.getBlockY(), z = at.getBlockZ();
        switch (landmark) {
            case THE_KETTLE -> tavern(world, bp, rng, x, y, z, plot.facing());
            case THE_BOARDING_HOUSE -> manor(world, bp, rng, x, y, z, true, plot.facing());
            case LOCKRIDGE_MANOR -> manor(world, bp, rng, x, y, z, false, plot.facing());
            case THE_HEDGE_HOUSE -> hedgeHouse(world, bp, rng, x, y, z, plot.facing());
            default -> build(world, bp, rng, at, landmark);
        }
    }

    private int clearRadiusFor(Landmark landmark) {
        return switch (landmark) {
            case TOWN_SQUARE -> 20;
            case THE_QUARRY -> 32;
            case THE_OLD_CEMETERY -> 26;
            case THE_WHITE_OAK -> 20;
            case THE_BOARDING_HOUSE, LOCKRIDGE_MANOR -> 22;
            case THE_BURNED_CHURCH -> 20;
            case WICKER_BRIDGE -> 30;
            case THE_TOMB -> 0;   // underground; nothing to clear
            default -> 14;
        };
    }

    private int radiusFor(Landmark landmark) {
        return switch (landmark) {
            case TOWN_SQUARE, THE_QUARRY, THE_OLD_CEMETERY -> 32;
            case THE_WHITE_OAK -> 18;
            case THE_TOMB -> 12;
            default -> 24;
        };
    }

    private void build(World world, Blueprint bp, Random rng, Location at, Landmark landmark) {
        int x = at.getBlockX(), y = at.getBlockY(), z = at.getBlockZ();

        switch (landmark) {
            case TOWN_SQUARE -> square(world, bp, rng, x, y, z);
            // These four normally arrive via buildOnPlot() with a real street
            // facing. This branch is the fallback for admin placement, where
            // there's no plot, so they default to facing south.
            case THE_KETTLE -> tavern(world, bp, rng, x, y, z, TownPlan.Facing.SOUTH);
            case THE_BOARDING_HOUSE -> manor(world, bp, rng, x, y, z, true, TownPlan.Facing.SOUTH);
            case LOCKRIDGE_MANOR -> manor(world, bp, rng, x, y, z, false, TownPlan.Facing.SOUTH);
            case THE_HEDGE_HOUSE -> hedgeHouse(world, bp, rng, x, y, z, TownPlan.Facing.SOUTH);
            case THE_BURNED_CHURCH -> ruinedChurch(world, bp, rng, x, y, z);
            case THE_TOMB -> tomb(world, x, y, z);
            case WICKER_BRIDGE -> bridge(world, x, y, z);
            case THE_WHITE_OAK -> whiteOak(world, rng, x, y, z);
            case THE_QUARRY -> quarry(world, rng, x, y, z);
            case THE_OLD_CEMETERY -> cemetery(world, bp, rng, x, y, z);
        }
    }

    // ------------------------------------------------------------------
    // Landmarks
    // ------------------------------------------------------------------

    private void square(World world, Blueprint bp, Random rng, int x, int y, int z) {
        // Circular plaza with a radial paving pattern
        for (int dx = -19; dx <= 19; dx++) {
            for (int dz = -19; dz <= 19; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > 19) continue;
                Block b = world.getBlockAt(x + dx, y - 1, z + dz);
                b.setType(d < 4 ? Material.POLISHED_ANDESITE
                        : ((dx + dz) % 4 == 0 ? Material.STONE_BRICKS : Material.SMOOTH_STONE));
                for (int cy = 0; cy < 8; cy++) {
                    Block above = world.getBlockAt(x + dx, y + cy, z + dz);
                    if (above.getType() != Material.AIR) above.setType(Material.AIR);
                }
            }
        }

        // Clock tower
        int h = 26;
        bp.prepareSite(x, y, z, 3, 3, Material.STONE_BRICKS);
        for (int cy = 0; cy < h; cy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    boolean edge = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                    if (!edge) continue;
                    Material m = (cy % 5 == 4) ? Material.POLISHED_ANDESITE : Material.STONE_BRICKS;
                    world.getBlockAt(x + dx, y + cy, z + dz).setType(m);
                }
            }
        }
        // Clock faces on all four sides, hands stopped
        for (int[] face : new int[][]{{0, -2}, {0, 2}, {-2, 0}, {2, 0}}) {
            world.getBlockAt(x + face[0] * 1, y + h - 5, z + face[1] * 1).setType(Material.WHITE_CONCRETE);
            world.getBlockAt(x + face[0], y + h - 4, z + face[1]).setType(Material.WHITE_CONCRETE);
            world.getBlockAt(x + face[0], y + h - 3, z + face[1]).setType(Material.BLACK_CONCRETE);
        }
        bp.gableRoof(x, y + h, z, 3, 3, Material.DEEPSLATE_TILE_STAIRS, Material.DEEPSLATE_TILES);
        bp.weather(x, y, z, 3, 3, h, 0.15);

        // Benches and lamps ringing the plaza
        for (int i = 0; i < 8; i++) {
            double a = (Math.PI * 2 / 8) * i;
            int bx = x + (int) (Math.cos(a) * 14);
            int bz = z + (int) (Math.sin(a) * 14);
            world.getBlockAt(bx, y, bz).setType(Material.OAK_STAIRS);
            world.getBlockAt(bx + 1, y, bz).setType(Material.OAK_STAIRS);
            if (i % 2 == 0) {
                world.getBlockAt(bx, y, bz + 2).setType(Material.OAK_FENCE);
                world.getBlockAt(bx, y + 1, bz + 2).setType(Material.OAK_FENCE);
                world.getBlockAt(bx, y + 2, bz + 2).setType(Material.LANTERN);
            }
        }
    }

    private void tavern(World world, Blueprint bp, Random rng, int x, int y, int z, TownPlan.Facing facing) {
        int hw = 7, hl = 6, h = 7;
        bp.prepareSite(x, y, z, hw, hl, Material.COBBLESTONE);
        bp.walls(x, y, z, hw, hl, h, Material.SPRUCE_PLANKS, Material.SPRUCE_LOG, Material.GLASS_PANE);
        bp.doorFacing(x, y, z, hw, hl, facing, Material.SPRUCE_DOOR);
        bp.porchFacing(x, y, z, hw, hl, facing, Material.SPRUCE_FENCE, Material.SPRUCE_SLAB);
        bp.gableRoof(x, y + h, z, hw, hl, Material.DARK_OAK_STAIRS, Material.DARK_OAK_PLANKS);
        bp.chimney(x + hw - 2, y + h, z + hl - 2, 5, Material.BRICKS);
        bp.foundationSkirt(x, y, z, hw, hl, Material.COBBLESTONE);

        // Bar interior
        for (int dx = -4; dx <= 4; dx++) {
            world.getBlockAt(x + dx, y, z + 3).setType(Material.SPRUCE_SLAB);
            world.getBlockAt(x + dx, y - 1, z + 3).setType(Material.SPRUCE_PLANKS);
        }
        world.getBlockAt(x - 2, y, z + 4).setType(Material.BARREL);
        world.getBlockAt(x + 2, y, z + 4).setType(Material.BREWING_STAND);
        for (int dx = -3; dx <= 3; dx += 3) {
            world.getBlockAt(x + dx, y, z - 2).setType(Material.OAK_STAIRS);
            world.getBlockAt(x + dx, y + 1, z - 2).setType(Material.AIR);
        }
        world.getBlockAt(x, y + 4, z).setType(Material.LANTERN);
        bp.weather(x, y, z, hw, hl, h, 0.08);
    }

    private void manor(World world, Blueprint bp, Random rng, int x, int y, int z, boolean dark, TownPlan.Facing facing) {
        Material wall = dark ? Material.DARK_OAK_PLANKS : Material.STRIPPED_OAK_WOOD;
        Material post = dark ? Material.DARK_OAK_LOG : Material.OAK_LOG;
        Material stair = dark ? Material.DARK_OAK_STAIRS : Material.OAK_STAIRS;
        Material roof = dark ? Material.DEEPSLATE_TILES : Material.TUFF_BRICKS;

        int hw = 10, hl = 8, h = 13;
        bp.prepareSite(x, y, z, hw, hl, Material.STONE_BRICKS);
        bp.walls(x, y, z, hw, hl, h, wall, post, Material.GLASS_PANE);
        bp.doorFacing(x, y, z, hw, hl, facing, dark ? Material.DARK_OAK_DOOR : Material.OAK_DOOR);
        bp.porchFacing(x, y, z, hw, hl, facing, post, dark ? Material.DARK_OAK_SLAB : Material.OAK_SLAB);
        bp.gableRoof(x, y + h, z, hw, hl, stair, roof);
        bp.chimney(x - hw + 2, y + h, z + hl - 3, 7, Material.DEEPSLATE_BRICKS);
        bp.chimney(x + hw - 3, y + h, z + hl - 3, 6, Material.DEEPSLATE_BRICKS);
        bp.foundationSkirt(x, y, z, hw, hl, Material.STONE_BRICKS);

        // Side wing
        int wx = x + hw + 5;
        bp.prepareSite(wx, y, z + 3, 5, 5, Material.STONE_BRICKS);
        bp.walls(wx, y, z + 3, 5, 5, 8, wall, post, Material.GLASS_PANE);
        bp.gableRoof(wx, y + 8, z + 3, 5, 5, stair, roof);

        // Interior floors
        for (int dx = -hw + 1; dx < hw; dx++) {
            for (int dz = -hl + 1; dz < hl; dz++) {
                world.getBlockAt(x + dx, y + 5, z + dz).setType(wall);
            }
        }
        bp.furnish(x, y, z, hw - 2, hl - 2, true);
        bp.furnish(x, y + 6, z, hw - 2, hl - 2, true);
        bp.weather(x, y, z, hw, hl, h, dark ? 0.2 : 0.1);
    }

    private void hedgeHouse(World world, Blueprint bp, Random rng, int x, int y, int z, TownPlan.Facing facing) {
        int hw = 5, hl = 5, h = 6;
        bp.prepareSite(x, y, z, hw, hl, Material.COBBLESTONE);
        bp.walls(x, y, z, hw, hl, h, Material.OAK_PLANKS, Material.OAK_LOG, Material.GLASS_PANE);
        bp.doorFacing(x, y, z, hw, hl, facing, Material.OAK_DOOR);
        bp.gableRoof(x, y + h, z, hw, hl, Material.OAK_STAIRS, Material.MOSSY_COBBLESTONE);
        bp.chimney(x + hw - 2, y + h, z + hl - 2, 4, Material.COBBLESTONE);
        bp.furnish(x, y, z, hw - 1, hl - 1, true);

        // Herb garden, behind the house rather than out front.
        TownPlan.Facing back = facing.opposite();
        for (int a = -7; a <= 7; a++) {
            for (int b = hl + 2; b <= hl + 7; b++) {
                int gx = x + (back.dx != 0 ? back.dx * b : a);
                int gz = z + (back.dz != 0 ? back.dz * b : a);
                Block soil = world.getBlockAt(gx, y - 1, gz);
                soil.setType(Material.FARMLAND);
                Block crop = soil.getRelative(BlockFace.UP);
                crop.setType(switch (rng.nextInt(4)) {
                    case 0 -> Material.NETHER_WART;
                    case 1 -> Material.SWEET_BERRY_BUSH;
                    case 2 -> Material.PUMPKIN_STEM;
                    default -> Material.WHEAT;
                });
            }
        }
        // Salt line across the threshold, on whichever side the door is.
        for (int a = -3; a <= 3; a++) {
            int sx = x + (facing.dx != 0 ? facing.dx * (hw + 1) : a);
            int sz = z + (facing.dz != 0 ? facing.dz * (hl + 1) : a);
            world.getBlockAt(sx, y - 1, sz).setType(Material.CALCITE);
        }
        // Drying racks
        for (int dx = -hw; dx <= hw; dx += 3) {
            world.getBlockAt(x + dx, y + 4, z - hl).setType(Material.OAK_FENCE);
        }
        bp.weather(x, y, z, hw, hl, h, 0.25);
        bp.overgrow(x, y, z, hw, hl, h, 0.06);
    }

    private void ruinedChurch(World world, Blueprint bp, Random rng, int x, int y, int z) {
        int hw = 9, hl = 14;
        bp.prepareSite(x, y, z, hw, hl, Material.CRACKED_STONE_BRICKS);

        // Broken walls with random surviving height
        for (int dx = -hw; dx <= hw; dx++) {
            for (int dz = -hl; dz <= hl; dz++) {
                boolean edge = Math.abs(dx) == hw || Math.abs(dz) == hl;
                world.getBlockAt(x + dx, y - 1, z + dz)
                        .setType(rng.nextInt(6) == 0 ? Material.BLACKSTONE : Material.CRACKED_STONE_BRICKS);
                if (!edge) continue;

                // Height falls off toward the middle of each wall - reads as collapse
                double along = Math.abs(dx) == hw ? (double) dz / hl : (double) dx / hw;
                int max = (int) (3 + Math.abs(along) * 8) + rng.nextInt(3);
                for (int cy = 0; cy < max; cy++) {
                    Material m = rng.nextInt(3) == 0 ? Material.MOSSY_STONE_BRICKS : Material.CRACKED_STONE_BRICKS;
                    world.getBlockAt(x + dx, y + cy, z + dz).setType(m);
                }
            }
        }

        // Surviving apse arch at the north end
        for (int dx = -3; dx <= 3; dx++) {
            for (int cy = 0; cy < 9; cy++) {
                if (Math.abs(dx) == 3 || cy == 8) {
                    world.getBlockAt(x + dx, y + cy, z - hl).setType(Material.MOSSY_STONE_BRICKS);
                }
            }
        }

        // The fallen bell, and the hole it went through
        world.getBlockAt(x + 3, y - 1, z - 6).setType(Material.BELL);
        for (int dx = 2; dx <= 4; dx++) {
            for (int dz = -7; dz <= -5; dz++) {
                world.getBlockAt(x + dx, y - 1, z + dz).setType(Material.AIR);
                world.getBlockAt(x + dx, y - 2, z + dz).setType(Material.DEEPSLATE_BRICKS);
            }
        }

        // Pews, mostly destroyed
        for (int dz = -8; dz <= 8; dz += 3) {
            if (rng.nextInt(3) == 0) continue;
            for (int dx = -5; dx <= 5; dx++) {
                if (rng.nextInt(4) == 0) continue;
                world.getBlockAt(x + dx, y, z + dz).setType(Material.OAK_STAIRS);
            }
        }
        bp.overgrow(x, y, z, hw, hl, 10, 0.1);
    }

    private void tomb(World world, int x, int y, int z) {
        int ty = Math.max(world.getMinHeight() + 14, y - 24);
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                for (int dy = 0; dy < 7; dy++) {
                    boolean shell = Math.abs(dx) == 8 || Math.abs(dz) == 8 || dy == 0 || dy == 6;
                    world.getBlockAt(x + dx, ty + dy, z + dz)
                            .setType(shell ? Material.DEEPSLATE_BRICKS : Material.AIR);
                }
            }
        }
        // Alcoves along the walls - something was kept in each
        for (int dz = -6; dz <= 6; dz += 3) {
            for (int side : new int[]{-7, 7}) {
                world.getBlockAt(x + side, ty + 1, z + dz).setType(Material.AIR);
                world.getBlockAt(x + side, ty + 2, z + dz).setType(Material.AIR);
                world.getBlockAt(x + side, ty + 1, z + dz).setType(Material.SOUL_LANTERN);
            }
        }
        // The seal
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 1; dy <= 3; dy++) {
                world.getBlockAt(x + dx, ty + dy, z + 8).setType(Material.REINFORCED_DEEPSLATE);
            }
        }
        // Shaft up to the church floor
        for (int dy = ty + 7; dy < y; dy++) {
            world.getBlockAt(x + 3, dy, z - 6).setType(Material.AIR);
            world.getBlockAt(x + 3, dy, z - 5).setType(Material.LADDER);
        }
    }

    private void bridge(World world, int x, int y, int z) {
        // Carve a river channel at ground level, then span it. The deck sits
        // just above the waterline rather than at whatever height the trees
        // happened to be.
        int waterY = y - 1;

        for (int dz = -26; dz <= 26; dz++) {
            for (int dx = -6; dx <= 6; dx++) {
                int wx = x + dx, wz = z + dz;
                double edge = Math.abs(dx) / 6.0;
                int depth = (int) (3 * (1 - edge)) + 1;

                // Clear anything standing in the channel.
                int top = world.getHighestBlockYAt(wx, wz);
                for (int cy = waterY - depth; cy <= top; cy++) {
                    world.getBlockAt(wx, cy, wz).setType(Material.AIR);
                }
                for (int dy = 0; dy < depth; dy++) {
                    world.getBlockAt(wx, waterY - dy, wz).setType(Material.WATER);
                }
                world.getBlockAt(wx, waterY - depth, wz).setType(Material.GRAVEL);
            }
        }

        // Deck, one above the water.
        int deckY = waterY + 2;
        for (int dz = -10; dz <= 10; dz++) {
            for (int dx = -3; dx <= 3; dx++) {
                world.getBlockAt(x + dx, deckY, z + dz).setType(Material.OAK_PLANKS);
            }
            world.getBlockAt(x - 4, deckY, z + dz).setType(Material.OAK_PLANKS);
            world.getBlockAt(x + 4, deckY, z + dz).setType(Material.OAK_PLANKS);
            world.getBlockAt(x - 4, deckY + 1, z + dz).setType(Material.OAK_FENCE);
            world.getBlockAt(x + 4, deckY + 1, z + dz).setType(Material.OAK_FENCE);

            if (dz % 5 == 0) {
                for (int dy = deckY - 1; dy >= waterY - 4; dy--) {
                    world.getBlockAt(x - 3, dy, z + dz).setType(Material.OAK_LOG);
                    world.getBlockAt(x + 3, dy, z + dz).setType(Material.OAK_LOG);
                }
                world.getBlockAt(x - 4, deckY + 2, z + dz).setType(Material.LANTERN);
            }
        }

        // Ramps up to the banks so the bridge connects to the ground.
        for (int side : new int[]{-1, 1}) {
            for (int step = 11; step <= 18; step++) {
                int bz = z + side * step;
                int bankY = Blueprint.groundY(world, x, bz);
                int rampY = deckY + (bankY - deckY) * (step - 10) / 8;
                for (int dx = -3; dx <= 3; dx++) {
                    world.getBlockAt(x + dx, rampY, z + side * step).setType(Material.DIRT_PATH);
                    for (int dy = 1; dy <= 4; dy++) {
                        Block above = world.getBlockAt(x + dx, rampY + dy, z + side * step);
                        if (above.getType() != Material.AIR) above.setType(Material.AIR);
                    }
                }
            }
        }
    }

    private void whiteOak(World world, Random rng, int x, int y, int z) {
        // Clear a proper glade. This tree should be visible from a distance
        // and impossible to mistake for ordinary forest.
        for (int dx = -20; dx <= 20; dx++) {
            for (int dz = -20; dz <= 20; dz++) {
                if (dx * dx + dz * dz > 400) continue;
                int wx = x + dx, wz = z + dz;
                int top = world.getHighestBlockYAt(wx, wz);
                int ground = Blueprint.groundY(world, wx, wz);
                for (int cy = ground + 1; cy <= top + 40; cy++) {
                    world.getBlockAt(wx, cy, wz).setType(Material.AIR);
                }
                if (dx * dx + dz * dz <= 196) {
                    world.getBlockAt(wx, ground, wz).setType(Material.PODZOL);
                }
            }
        }

        int height = 38;   // was 26 - it read as a shrub

        // Buttressed trunk: 5x5 at the base, tapering to 3x3, then 2x2.
        for (int dy = 0; dy < height; dy++) {
            int r = dy < 5 ? 2 : (dy < 16 ? 1 : 1);
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    // Round off the base corners so it's not a square column.
                    if (dy < 5 && Math.abs(dx) == 2 && Math.abs(dz) == 2) continue;
                    if (dy >= 16 && (Math.abs(dx) + Math.abs(dz)) > 1) continue;
                    world.getBlockAt(x + dx, y + dy, z + dz).setType(Material.PALE_OAK_LOG);
                }
            }
        }

        // Root flare at the base.
        for (int i = 0; i < 8; i++) {
            double a = (Math.PI * 2 / 8) * i;
            for (int step = 2; step <= 5; step++) {
                int rx = x + (int) Math.round(Math.cos(a) * step);
                int rz = z + (int) Math.round(Math.sin(a) * step);
                world.getBlockAt(rx, y, rz).setType(Material.PALE_OAK_WOOD);
            }
        }

        // Major boughs sweeping out and up.
        for (int i = 0; i < 7; i++) {
            double a = (Math.PI * 2 / 7) * i + rng.nextDouble() * 0.5;
            int by = y + height - 16 + rng.nextInt(9);
            for (int step = 1; step <= 9; step++) {
                int bx = x + (int) (Math.cos(a) * step);
                int bz = z + (int) (Math.sin(a) * step);
                world.getBlockAt(bx, by + step / 2, bz).setType(Material.PALE_OAK_LOG);
            }
        }

        // Broad canopy.
        int canopyBase = height - 16;
        for (int dy = canopyBase; dy < height + 8; dy++) {
            double t = (double) (dy - canopyBase) / (height + 8 - canopyBase);
            int r = (int) (16 * Math.sin(t * Math.PI) + 4);
            if (r <= 0) continue;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz > r * r) continue;
                    if (rng.nextInt(9) == 0) continue;   // ragged edges
                    Block b = world.getBlockAt(x + dx, y + dy, z + dz);
                    if (b.getType() == Material.AIR) b.setType(Material.PALE_OAK_LEAVES);
                }
            }
        }

        // The old stone ring. Somebody marked this a very long time ago.
        for (int i = 0; i < 48; i++) {
            double a = (Math.PI * 2 / 48) * i;
            int sx = x + (int) Math.round(Math.cos(a) * 11);
            int sz = z + (int) Math.round(Math.sin(a) * 11);
            int sy = Blueprint.groundY(world, sx, sz);
            world.getBlockAt(sx, sy, sz).setType(Material.MOSSY_COBBLESTONE);
            if (i % 6 == 0) {
                world.getBlockAt(sx, sy + 1, sz).setType(Material.MOSSY_COBBLESTONE_WALL);
                world.getBlockAt(sx, sy + 2, sz).setType(Material.MOSSY_COBBLESTONE_WALL);
                world.getBlockAt(sx, sy + 3, sz).setType(Material.MOSSY_COBBLESTONE_WALL);
            }
        }
    }

    private void quarry(World world, Random rng, int x, int y, int z) {
        int r = 26;
        // Water fills only the lower part of the pit. Everything above the
        // waterline is open air, so this reads as an excavation rather than
        // the floating column the old version produced.
        int waterTop = y - 10;

        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > r) continue;

                int wx = x + dx, wz = z + dz;
                int surface = Blueprint.groundY(world, wx, wz);

                // Terraced sides: deeper toward the middle, stepped in fours.
                int depth = (int) ((r - d) * 0.7);
                int terrace = (depth / 4) * 4;
                int floorY = y - terrace;

                // Clear everything from the pit floor up past the old surface.
                for (int cy = floorY; cy <= Math.max(surface, y) + 12; cy++) {
                    world.getBlockAt(wx, cy, wz).setType(Material.AIR);
                }

                // Fill the bottom with water, but never above the waterline.
                for (int cy = floorY; cy <= Math.min(waterTop, y - 1); cy++) {
                    world.getBlockAt(wx, cy, wz).setType(Material.WATER);
                }

                // Rock floor beneath.
                world.getBlockAt(wx, floorY - 1, wz)
                        .setType(rng.nextInt(8) == 0 ? Material.ANDESITE : Material.STONE);

                // Exposed stone on the terrace faces.
                if (terrace > 0) {
                    world.getBlockAt(wx, floorY, wz).setType(Material.STONE);
                }
            }
        }

        // Rusted fencing around the rim, on real ground, with gaps.
        for (int i = 0; i < 70; i++) {
            double a = (Math.PI * 2 / 70) * i;
            int fx = x + (int) (Math.cos(a) * (r + 2));
            int fz = z + (int) (Math.sin(a) * (r + 2));
            if (rng.nextInt(6) == 0) continue;
            int fy = Blueprint.groundY(world, fx, fz);
            world.getBlockAt(fx, fy + 1, fz).setType(Material.IRON_BARS);
            world.getBlockAt(fx, fy + 2, fz).setType(Material.IRON_BARS);
        }
    }

    private void cemetery(World world, Blueprint bp, Random rng, int x, int y, int z) {
        // Iron fence perimeter
        for (int dx = -20; dx <= 20; dx++) {
            for (int dz = -20; dz <= 20; dz++) {
                boolean edge = Math.abs(dx) == 20 || Math.abs(dz) == 20;
                if (!edge) continue;
                if (dx == 0 && dz == 20) continue; // gate
                int fy = Blueprint.groundY(world, x + dx, z + dz);
                world.getBlockAt(x + dx, fy, z + dz).setType(Material.IRON_BARS);
                world.getBlockAt(x + dx, fy + 1, z + dz).setType(Material.IRON_BARS);
            }
        }

        // Rows of headstones
        for (int dx = -16; dx <= 16; dx += 4) {
            for (int dz = -16; dz <= 14; dz += 5) {
                if (rng.nextInt(7) == 0) continue;
                int gy = Blueprint.groundY(world, x + dx, z + dz);
                Material stone = rng.nextInt(4) == 0 ? Material.MOSSY_COBBLESTONE_WALL : Material.STONE_BRICK_WALL;
                world.getBlockAt(x + dx, gy + 1, z + dz).setType(stone);
                if (rng.nextInt(3) == 0) {
                    world.getBlockAt(x + dx, gy + 2, z + dz).setType(Material.STONE_BRICK_SLAB);
                }
                // Disturbed earth on a few plots
                if (rng.nextInt(12) == 0) {
                    world.getBlockAt(x + dx, gy, z + dz + 1).setType(Material.COARSE_DIRT);
                }
            }
        }

        // Founders' mausoleum at the top of the hill
        int my = Blueprint.groundY(world, x, z - 18);
        bp.prepareSite(x, my, z - 18, 4, 4, Material.POLISHED_DEEPSLATE);
        bp.walls(x, my, z - 18, 4, 4, 6, Material.POLISHED_DEEPSLATE,
                Material.DEEPSLATE_BRICKS, null);
        bp.door(x, my, z - 18, 4, Material.IRON_DOOR);
        bp.gableRoof(x, my + 6, z - 18, 4, 4, Material.DEEPSLATE_TILE_STAIRS, Material.DEEPSLATE_TILES);
        bp.weather(x, my, z - 18, 4, 4, 6, 0.3);
    }

    // ------------------------------------------------------------------
    // Filler
    // ------------------------------------------------------------------

    /**
     * A downtown commercial building. Two or three storeys, flat roof, no
     * yard, sitting hard against the pavement and its neighbours. Brick and
     * painted timber rather than the cottage palette used further out.
     */
    private void downtownShop(World world, Blueprint bp, Random rng,
                              Location at, TownPlan.Plot plot) {
        int x = at.getBlockX(), y = at.getBlockY(), z = at.getBlockZ();

        Material[][] shopPalettes = {
                {Material.BRICKS,               Material.DEEPSLATE_BRICKS, Material.DEEPSLATE_TILES},
                {Material.STRIPPED_DARK_OAK_WOOD, Material.DARK_OAK_LOG,   Material.DEEPSLATE_TILES},
                {Material.SMOOTH_STONE,         Material.STONE_BRICKS,     Material.TUFF_BRICKS},
                {Material.TERRACOTTA,           Material.DARK_OAK_LOG,     Material.DEEPSLATE_TILES},
                {Material.WHITE_TERRACOTTA,     Material.STONE_BRICKS,     Material.DEEPSLATE_TILES}
        };
        Material[] pal = shopPalettes[rng.nextInt(shopPalettes.length)];

        // Taller nearer the square, so the skyline has a centre.
        // Vary storeys so a terrace has a real skyline instead of a flat top.
        // Taller nearer the square, but with enough spread that neighbours differ.
        int base = plot.distance() < 35 ? 3 : plot.distance() < 55 ? 2 : 1;
        int storeys = Math.max(1, base + rng.nextInt(2) - (rng.nextInt(4) == 0 ? 1 : 0));

        bp.shopfront(x, y, z, plot.width() - 1, plot.depth() - 1, storeys,
                plot.facing(), pal[0], pal[1], pal[2]);

        bp.furnish(x, y, z, plot.width() - 2, plot.depth() - 2, true);
        bp.weather(x, y, z, plot.width(), plot.depth(), 6, 0.06);
    }

    /**
     * Material palettes for residential houses. Varied enough that a street
     * isn't monotonous, narrow enough that they read as the same town.
     * Downtown uses its own separate set - see downtownShop().
     */
    private static final Material[][] PALETTES = {
            {Material.SPRUCE_PLANKS,          Material.SPRUCE_LOG, Material.SPRUCE_STAIRS,   Material.DEEPSLATE_TILES},
            {Material.OAK_PLANKS,             Material.OAK_LOG,    Material.OAK_STAIRS,      Material.DEEPSLATE_TILES},
            {Material.DARK_OAK_PLANKS,        Material.DARK_OAK_LOG, Material.DARK_OAK_STAIRS, Material.DEEPSLATE_BRICKS},
            {Material.STRIPPED_SPRUCE_WOOD,   Material.SPRUCE_LOG, Material.SPRUCE_STAIRS,   Material.TUFF_BRICKS},
            {Material.WHITE_TERRACOTTA,       Material.OAK_LOG,    Material.OAK_STAIRS,      Material.DEEPSLATE_TILES}
    };

    private void fillerHouse(World world, Blueprint bp, Random rng, Location at, TownPlan.Plot plot) {
        int x = at.getBlockX(), y = at.getBlockY(), z = at.getBlockZ();
        Material[] palette = PALETTES[rng.nextInt(PALETTES.length)];

        // Footprint comes from the plot, so buildings on the same street
        // sit at a consistent depth from the road.
        int hw = plot.width() - 1;
        int hl = plot.depth() - 1;
        int h = 5 + rng.nextInt(3);
        TownPlan.Facing facing = plot.facing();
        boolean abandoned = rng.nextInt(6) == 0;

        bp.prepareSite(x, y, z, hw, hl, Material.COBBLESTONE);
        bp.walls(x, y, z, hw, hl, h, palette[0], palette[1], abandoned ? null : Material.GLASS_PANE);
        bp.doorFacing(x, y, z, hw, hl, facing, Material.OAK_DOOR);
        bp.gableRoof(x, y + h, z, hw, hl, palette[2], palette[3]);
        bp.foundationSkirt(x, y, z, hw, hl, Material.COBBLESTONE);

        // A path from the door out to the street, stopping when it gets there.
        bp.frontPath(x, y, z, hw, hl, facing,
                plot.district() == TownPlan.District.DOWNTOWN ? 3 : 9);

        if (rng.nextBoolean()) {
            bp.chimney(x + hw - 2, y + h, z + hl - 2, 4, Material.BRICKS);
        }
        if (rng.nextInt(3) == 0) {
            bp.porchFacing(x, y, z, hw, hl, facing, palette[1], Material.OAK_SLAB);
        }

        bp.furnish(x, y, z, hw - 1, hl - 1, !abandoned);
        bp.weather(x, y, z, hw, hl, h, abandoned ? 0.4 : 0.1);
        if (abandoned) {
            bp.overgrow(x, y, z, hw, hl, h, 0.12);
            // Punch a hole in the roof
            for (int i = 0; i < 6; i++) {
                world.getBlockAt(x + rng.nextInt(5) - 2, y + h, z + rng.nextInt(5) - 2).setType(Material.AIR);
            }
        }

        // Fenced yard
        if (rng.nextBoolean()) {
            for (int dx = -hw - 3; dx <= hw + 3; dx++) {
                for (int dz = -hl - 3; dz <= hl + 3; dz++) {
                    boolean edge = Math.abs(dx) == hw + 3 || Math.abs(dz) == hl + 3;
                    if (!edge || rng.nextInt(8) == 0) continue;
                    int fy = Blueprint.groundY(world, x + dx, z + dz);
                    world.getBlockAt(x + dx, fy, z + dz).setType(Material.OAK_FENCE);
                }
            }
        }
    }
}
