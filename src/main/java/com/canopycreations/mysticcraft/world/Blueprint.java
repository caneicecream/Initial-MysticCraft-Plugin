package com.canopycreations.mysticcraft.world;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;

import java.util.Random;

/**
 * Building primitives.
 *
 * Procedural structures look generated when every building is a box. They
 * look BUILT when they share an architectural vocabulary - the same roof
 * pitch, the same window rhythm, foundations that meet the ground properly -
 * while varying in footprint and detail.
 *
 * These helpers are that vocabulary. Every structure in Ashfall is assembled
 * from them, which is what makes a hand-placed manor and a procedural filler
 * cottage read as belonging to the same town.
 */
public class Blueprint {

    private final World world;
    private final Random rng;

    public Blueprint(World world, Random rng) {
        this.world = world;
        this.rng = rng;
    }

    /**
     * The real ground height at a column.
     *
     * getHighestBlockYAt() returns the highest block of ANY kind, which in a
     * forest is a leaf twenty blocks up. Building on that is what put roads
     * in the sky. This scans down from the surface past vegetation, water and
     * snow until it hits something you could actually stand on.
     */
    public static int groundY(World world, int x, int z) {
        int y = world.getHighestBlockYAt(x, z);
        int floor = world.getMinHeight() + 1;

        while (y > floor) {
            Material m = world.getBlockAt(x, y, z).getType();
            if (isGround(m)) return y;
            y--;
        }
        return world.getSeaLevel();
    }

    /** Blocks that count as terrain rather than something growing on it. */
    private static boolean isGround(Material m) {
        return switch (m) {
            case GRASS_BLOCK, DIRT, COARSE_DIRT, PODZOL, ROOTED_DIRT, MUD,
                 STONE, DEEPSLATE, ANDESITE, DIORITE, GRANITE, TUFF, CALCITE,
                 SAND, RED_SAND, SANDSTONE, GRAVEL, CLAY,
                 MOSS_BLOCK, MYCELIUM, SNOW_BLOCK, PACKED_ICE, TERRACOTTA -> true;
            default -> false;
        };
    }

    /**
     * Strips trees, grass, flowers and snow layers from a radius so a
     * building doesn't generate half-buried in a spruce forest.
     */
    public void clearVegetation(int cx, int cz, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > radius * radius) continue;
                int wx = cx + x, wz = cz + z;
                int top = world.getHighestBlockYAt(wx, wz);
                int ground = groundY(world, wx, wz);
                for (int y = ground + 1; y <= top; y++) {
                    Block b = world.getBlockAt(wx, y, wz);
                    if (b.getType() != Material.AIR) b.setType(Material.AIR);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Terrain preparation
    // ------------------------------------------------------------------

    /**
     * Levels a footprint to a target height and carries a foundation down to
     * meet the real ground. This is the single most important thing for
     * making generated buildings not look like they're floating or buried.
     */
    public void prepareSite(int cx, int cy, int cz, int halfW, int halfL, Material foundation) {
        for (int x = -halfW - 1; x <= halfW + 1; x++) {
            for (int z = -halfL - 1; z <= halfL + 1; z++) {
                int wx = cx + x, wz = cz + z;

                // Clear everything above the pad.
                for (int y = cy; y < cy + 24; y++) {
                    Block b = world.getBlockAt(wx, y, wz);
                    if (b.getType() != Material.AIR) b.setType(Material.AIR);
                }

                // Fill down until we hit solid ground, so the building sits on something.
                for (int y = cy - 1; y > cy - 30; y--) {
                    Block b = world.getBlockAt(wx, y, wz);
                    if (b.getType().isSolid() && y < cy - 1) break;
                    b.setType(foundation);
                }
            }
        }
    }

    /** A slightly wider stone skirt so the pad blends into the terrain. */
    public void foundationSkirt(int cx, int cy, int cz, int halfW, int halfL, Material material) {
        for (int x = -halfW - 2; x <= halfW + 2; x++) {
            for (int z = -halfL - 2; z <= halfL + 2; z++) {
                if (Math.abs(x) <= halfW && Math.abs(z) <= halfL) continue;
                Block b = world.getBlockAt(cx + x, cy - 1, cz + z);
                if (rng.nextInt(5) == 0) continue; // ragged edge reads as natural
                b.setType(material);
            }
        }
    }

    // ------------------------------------------------------------------
    // Walls
    // ------------------------------------------------------------------

    /**
     * Walls with a proper window rhythm and corner posts. Windows are placed
     * on a regular beat rather than randomly, which is what makes a facade
     * look designed.
     */
    public void walls(int cx, int cy, int cz, int halfW, int halfL, int height,
                      Material wall, Material post, Material window) {
        for (int x = -halfW; x <= halfW; x++) {
            for (int z = -halfL; z <= halfL; z++) {
                boolean edge = Math.abs(x) == halfW || Math.abs(z) == halfL;
                if (!edge) continue;
                boolean corner = Math.abs(x) == halfW && Math.abs(z) == halfL;

                for (int y = 0; y < height; y++) {
                    Material m = corner ? post : wall;

                    // Window band: every third block, one storey up, two tall.
                    if (!corner && window != null) {
                        int along = (Math.abs(x) == halfW) ? z : x;
                        boolean onBeat = Math.floorMod(along, 3) == 0;
                        boolean firstFloor = y == 2 || y == 3;
                        boolean secondFloor = height > 8 && (y == 7 || y == 8);
                        if (onBeat && (firstFloor || secondFloor)) m = window;
                    }
                    world.getBlockAt(cx + x, cy + y, cz + z).setType(m);
                }
            }
        }
        // Floor
        for (int x = -halfW + 1; x < halfW; x++) {
            for (int z = -halfL + 1; z < halfL; z++) {
                world.getBlockAt(cx + x, cy - 1, cz + z).setType(post);
            }
        }
    }

    /**
     * A door on whichever side faces the street, with a step and a lantern.
     * This is what makes a row of houses read as a row rather than a
     * collection - they all open the same way, onto the same road.
     */
    public void doorFacing(int cx, int cy, int cz, int halfW, int halfL,
                           TownPlan.Facing facing, Material doorMaterial) {
        int dx = cx + facing.dx * halfW;
        int dz = cz + facing.dz * halfL;

        world.getBlockAt(dx, cy, dz).setType(Material.AIR);
        world.getBlockAt(dx, cy + 1, dz).setType(Material.AIR);
        try {
            world.getBlockAt(dx, cy, dz).setType(doorMaterial);
        } catch (Exception ignored) {
            // odd geometry; the opening still works
        }

        // Step outside the threshold.
        world.getBlockAt(dx + facing.dx, cy - 1, dz + facing.dz).setType(Material.STONE_BRICK_SLAB);

        // Lantern beside the door, offset perpendicular to the facing.
        int lx = dx + (facing.dx != 0 ? 0 : 1);
        int lz = dz + (facing.dz != 0 ? 0 : 1);
        world.getBlockAt(lx, cy + 2, lz).setType(Material.LANTERN);
    }

    /** A covered porch on the street side. */
    public void porchFacing(int cx, int cy, int cz, int halfW, int halfL,
                            TownPlan.Facing facing, Material post, Material roof) {
        int depth = 3;

        for (int a = -halfW; a <= halfW; a++) {
            for (int b = 1; b <= depth; b++) {
                int px = cx + (facing.dx != 0 ? facing.dx * (halfW + b) : a);
                int pz = cz + (facing.dz != 0 ? facing.dz * (halfL + b) : a);
                world.getBlockAt(px, cy + 4, pz).setType(roof);
            }
        }

        // Corner posts at the porch edge.
        for (int side : new int[]{-halfW, halfW}) {
            int px = cx + (facing.dx != 0 ? facing.dx * (halfW + depth) : side);
            int pz = cz + (facing.dz != 0 ? facing.dz * (halfL + depth) : side);
            for (int y = 0; y <= 3; y++) {
                world.getBlockAt(px, cy + y, pz).setType(post);
            }
        }
    }

    /**
     * A short path from the door out to the street. Small detail, but it's
     * what visually ties a building to the road it belongs to.
     */
    public void frontPath(int cx, int cy, int cz, int halfW, int halfL, TownPlan.Facing facing) {
        for (int b = 1; b <= 6; b++) {
            int px = cx + facing.dx * (halfW + b);
            int pz = cz + facing.dz * (halfL + b);
            int py = groundY(world, px, pz);
            world.getBlockAt(px, py, pz).setType(Material.GRAVEL);

            // A second tile wide, so it reads as a path not a line.
            int sx = px + (facing.dx != 0 ? 0 : 1);
            int sz = pz + (facing.dz != 0 ? 0 : 1);
            world.getBlockAt(sx, groundY(world, sx, sz), sz).setType(Material.GRAVEL);
        }
    }

    /**
     * Clears a whole district's footprint of trees. Real towns don't have
     * forest standing between the buildings - the land was cleared before
     * anyone built on it, and that absence of trees is a big part of what
     * makes somewhere read as settled rather than as a camp in the woods.
     */
    public void clearDistrict(int cx, int cz, int radius, double keepTreeChance, Random rng) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double d = Math.hypot(x, z);
                if (d > radius) continue;

                // Near the edge, leave more standing so the town fades into
                // the woods instead of ending at a hard circle.
                double edgeT = d / radius;
                double keep = keepTreeChance + edgeT * edgeT * 0.55;
                if (rng.nextDouble() < keep) continue;

                int wx = cx + x, wz = cz + z;
                int top = world.getHighestBlockYAt(wx, wz);
                int ground = groundY(world, wx, wz);
                for (int y = ground + 1; y <= top; y++) {
                    Block b = world.getBlockAt(wx, y, wz);
                    Material m = b.getType();
                    if (m == Material.AIR) continue;
                    // Leave anything already built.
                    if (m.name().contains("PLANK") || m.name().contains("BRICK")
                            || m.name().contains("STAIRS") || m.name().contains("LANTERN")) continue;
                    b.setType(Material.AIR);
                }
            }
        }
    }

    /**
     * A downtown shopfront: taller, flat-roofed, no yard, built hard against
     * the pavement and against its neighbours. This is the building type
     * that makes a commercial street feel like one.
     */
    public void shopfront(int cx, int cy, int cz, int halfW, int halfL, int storeys,
                          TownPlan.Facing facing, Material wall, Material trim, Material roof) {
        int h = 4 + storeys * 4;
        prepareSite(cx, cy, cz, halfW, halfL, Material.STONE_BRICKS);

        for (int x = -halfW; x <= halfW; x++) {
            for (int z = -halfL; z <= halfL; z++) {
                boolean edge = Math.abs(x) == halfW || Math.abs(z) == halfL;
                if (!edge) continue;
                boolean corner = Math.abs(x) == halfW && Math.abs(z) == halfL;

                for (int y = 0; y < h; y++) {
                    Material m = corner ? trim : wall;

                    // Big display windows on the street side at ground level.
                    boolean streetSide =
                            (facing.dx != 0 && x == facing.dx * halfW) ||
                            (facing.dz != 0 && z == facing.dz * halfL);
                    if (streetSide && !corner && y >= 1 && y <= 3) {
                        m = Material.GLASS_PANE;
                    }
                    // Upper storey windows on a regular beat, all sides.
                    else if (!corner && y > 4 && (y % 4 == 2 || y % 4 == 3)) {
                        int along = (Math.abs(x) == halfW) ? z : x;
                        if (Math.floorMod(along, 3) == 0) m = Material.GLASS_PANE;
                    }
                    world.getBlockAt(cx + x, cy + y, cz + z).setType(m);
                }
            }
        }

        // Floors between storeys.
        for (int s = 1; s <= storeys; s++) {
            for (int x = -halfW + 1; x < halfW; x++) {
                for (int z = -halfL + 1; z < halfL; z++) {
                    world.getBlockAt(cx + x, cy + s * 4, cz + z).setType(trim);
                }
            }
        }

        doorFacing(cx, cy, cz, halfW, halfL, facing, Material.OAK_DOOR);

        // Awning over the shopfront.
        for (int a = -halfW; a <= halfW; a++) {
            int ax = cx + (facing.dx != 0 ? facing.dx * (halfW + 1) : a);
            int az = cz + (facing.dz != 0 ? facing.dz * (halfL + 1) : a);
            world.getBlockAt(ax, cy + 4, az).setType(roof);
        }

        // Flat roof with a parapet, the way a real high street looks.
        flatRoof(cx, cy + h, cz, halfW, halfL, roof, trim);
    }

    /** A doorway punched through a wall, with a step and a lantern. */
    public void door(int cx, int cy, int cz, int halfL, Material doorMaterial) {
        int dz = cz - halfL;
        world.getBlockAt(cx, cy, dz).setType(Material.AIR);
        world.getBlockAt(cx, cy + 1, dz).setType(Material.AIR);
        try {
            world.getBlockAt(cx, cy, dz).setType(doorMaterial);
        } catch (Exception ignored) {
            // door placement can fail on odd geometry; the opening still works
        }
        world.getBlockAt(cx, cy - 1, dz - 1).setType(Material.STONE_BRICK_SLAB);
        world.getBlockAt(cx - 1, cy + 2, dz).setType(Material.LANTERN);
    }

    // ------------------------------------------------------------------
    // Roofs
    // ------------------------------------------------------------------

    /**
     * A gabled roof built from stairs. This is the detail that most separates
     * "generated box" from "building" at a glance.
     */
    public void gableRoof(int cx, int cy, int cz, int halfW, int halfL, Material stairs, Material fill) {
        int peak = Math.min(halfW, 6);
        for (int layer = 0; layer <= peak; layer++) {
            int inset = layer;
            int y = cy + layer;

            for (int z = -halfL - 1 + inset; z <= halfL + 1 - inset; z++) {
                // Sloped sides
                placeStair(cx - halfW - 1 + inset, y, cz + z, stairs, BlockFace.EAST);
                placeStair(cx + halfW + 1 - inset, y, cz + z, stairs, BlockFace.WEST);
            }
            // Cap the ridge
            if (layer == peak) {
                for (int z = -halfL - 1 + inset; z <= halfL + 1 - inset; z++) {
                    for (int x = -halfW - 1 + inset; x <= halfW + 1 - inset; x++) {
                        world.getBlockAt(cx + x, y, cz + z).setType(fill);
                    }
                }
            } else {
                // Close the gable ends
                for (int x = -halfW - 1 + inset; x <= halfW + 1 - inset; x++) {
                    world.getBlockAt(cx + x, y, cz - halfL - 1 + inset).setType(fill);
                    world.getBlockAt(cx + x, y, cz + halfL + 1 - inset).setType(fill);
                }
            }
        }
    }

    private void placeStair(int x, int y, int z, Material stairs, BlockFace facing) {
        Block b = world.getBlockAt(x, y, z);
        b.setType(stairs);
        BlockData data = b.getBlockData();
        if (data instanceof Stairs s) {
            s.setFacing(facing);
            b.setBlockData(s);
        }
    }

    /** A flat roof with a lip - for civic and commercial buildings. */
    public void flatRoof(int cx, int cy, int cz, int halfW, int halfL, Material deck, Material lip) {
        for (int x = -halfW - 1; x <= halfW + 1; x++) {
            for (int z = -halfL - 1; z <= halfL + 1; z++) {
                world.getBlockAt(cx + x, cy, cz + z).setType(deck);
                boolean edge = Math.abs(x) == halfW + 1 || Math.abs(z) == halfL + 1;
                if (edge) world.getBlockAt(cx + x, cy + 1, cz + z).setType(lip);
            }
        }
    }

    // ------------------------------------------------------------------
    // Details - the things that sell it
    // ------------------------------------------------------------------

    public void chimney(int cx, int cy, int cz, int height, Material material) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x <= 1; x++) {
                for (int z = 0; z <= 1; z++) {
                    world.getBlockAt(cx + x, cy + y, cz + z).setType(material);
                }
            }
        }
        world.getBlockAt(cx, cy + height, cz).setType(Material.CAMPFIRE);
    }

    /** A covered porch - reads immediately as "front of house". */
    public void porch(int cx, int cy, int cz, int halfW, Material post, Material roof) {
        for (int x = -halfW; x <= halfW; x++) {
            for (int z = 1; z <= 3; z++) {
                world.getBlockAt(cx + x, cy + 4, cz - z).setType(roof);
            }
        }
        world.getBlockAt(cx - halfW, cy, cz - 3).setType(post);
        world.getBlockAt(cx - halfW, cy + 1, cz - 3).setType(post);
        world.getBlockAt(cx - halfW, cy + 2, cz - 3).setType(post);
        world.getBlockAt(cx - halfW, cy + 3, cz - 3).setType(post);
        world.getBlockAt(cx + halfW, cy, cz - 3).setType(post);
        world.getBlockAt(cx + halfW, cy + 1, cz - 3).setType(post);
        world.getBlockAt(cx + halfW, cy + 2, cz - 3).setType(post);
        world.getBlockAt(cx + halfW, cy + 3, cz - 3).setType(post);
    }

    /** Sparse, believable interior clutter. */
    public void furnish(int cx, int cy, int cz, int halfW, int halfL, boolean occupied) {
        if (!occupied) return;

        Material[] furniture = {
                Material.CRAFTING_TABLE, Material.BARREL, Material.BOOKSHELF,
                Material.CHEST, Material.FURNACE, Material.CAULDRON,
                Material.LOOM, Material.COMPOSTER
        };

        int placements = 3 + rng.nextInt(4);
        for (int i = 0; i < placements; i++) {
            int x = cx - halfW + 1 + rng.nextInt(Math.max(1, halfW * 2 - 1));
            int z = cz - halfL + 1 + rng.nextInt(Math.max(1, halfL * 2 - 1));
            Block b = world.getBlockAt(x, cy, z);
            if (b.getType() != Material.AIR) continue;
            b.setType(furniture[rng.nextInt(furniture.length)]);
        }

        // A bed and a light source make it feel lived in.
        world.getBlockAt(cx - halfW + 1, cy, cz - halfL + 1).setType(Material.RED_BED);
        world.getBlockAt(cx, cy + 3, cz).setType(Material.LANTERN);
    }

    /** Age and weather a structure so not everything looks newly built. */
    public void weather(int cx, int cy, int cz, int halfW, int halfL, int height, double intensity) {
        for (int x = -halfW; x <= halfW; x++) {
            for (int z = -halfL; z <= halfL; z++) {
                for (int y = 0; y < height; y++) {
                    if (rng.nextDouble() > intensity) continue;
                    Block b = world.getBlockAt(cx + x, cy + y, cz + z);
                    Material t = b.getType();
                    if (t == Material.STONE_BRICKS) {
                        b.setType(rng.nextBoolean() ? Material.CRACKED_STONE_BRICKS : Material.MOSSY_STONE_BRICKS);
                    } else if (t == Material.COBBLESTONE) {
                        b.setType(Material.MOSSY_COBBLESTONE);
                    }
                }
            }
        }
    }

    /** Vines and overgrowth on a ruin. */
    public void overgrow(int cx, int cy, int cz, int halfW, int halfL, int height, double chance) {
        for (int x = -halfW; x <= halfW; x++) {
            for (int z = -halfL; z <= halfL; z++) {
                for (int y = 0; y < height; y++) {
                    if (rng.nextDouble() > chance) continue;
                    Block b = world.getBlockAt(cx + x, cy + y, cz + z);
                    if (b.getType() != Material.AIR) continue;
                    Block below = b.getRelative(BlockFace.DOWN);
                    if (below.getType().isSolid()) continue;
                    b.setType(Material.VINE);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Roads
    // ------------------------------------------------------------------

    /**
     * A road between two points, laid on real ground.
     *
     * Two fixes over the naive version: it uses groundY() so it can't ride
     * along treetops, and it smooths its own gradient so a road crossing
     * rolling terrain doesn't turn into a staircase. Vegetation is cleared
     * above the surface, and the road is cut INTO hills rather than draped
     * over whatever was there.
     */
    public void road(int x1, int z1, int x2, int z2, int width, Material surface, Material edge) {
        double dist = Math.hypot(x2 - x1, z2 - z1);
        int steps = (int) Math.ceil(dist);
        if (steps == 0) return;

        // First pass: sample real ground height along the route.
        int[] heights = new int[steps + 1];
        int[] xs = new int[steps + 1];
        int[] zs = new int[steps + 1];
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            xs[i] = (int) Math.round(x1 + (x2 - x1) * t);
            zs[i] = (int) Math.round(z1 + (z2 - z1) * t);
            heights[i] = groundY(world, xs[i], zs[i]);
        }

        // Second pass: smooth so the road grades instead of stepping.
        int[] smooth = heights.clone();
        for (int pass = 0; pass < 3; pass++) {
            for (int i = 1; i < steps; i++) {
                smooth[i] = Math.round((heights[i - 1] + heights[i] * 2 + heights[i + 1]) / 4f);
            }
            heights = smooth.clone();
        }

        // Third pass: actually lay it.
        for (int i = 0; i <= steps; i++) {
            int x = xs[i], z = zs[i], y = heights[i];

            for (int ox = -width; ox <= width; ox++) {
                for (int oz = -width; oz <= width; oz++) {
                    if (ox * ox + oz * oz > width * width) continue;
                    int wx = x + ox, wz = z + oz;

                    boolean rim = ox * ox + oz * oz > (width - 1) * (width - 1);
                    world.getBlockAt(wx, y, wz).setType(rim ? edge : surface);

                    // Support underneath, so the road never floats over a dip.
                    for (int dy = 1; dy <= 4; dy++) {
                        Block below = world.getBlockAt(wx, y - dy, wz);
                        if (below.getType() == Material.AIR || below.getType() == Material.WATER) {
                            below.setType(Material.DIRT);
                        } else break;
                    }

                    // Cut headroom, so the road passes through hills and trees.
                    for (int dy = 1; dy <= 5; dy++) {
                        Block above = world.getBlockAt(wx, y + dy, wz);
                        if (above.getType() != Material.AIR) above.setType(Material.AIR);
                    }
                }
            }

            // Lamp posts, planted on the verge at road height.
            if (i % 14 == 0 && i > 0 && i < steps) {
                int lx = x + width + 1, lz = z;
                int ly = heights[i];
                world.getBlockAt(lx, ly, lz).setType(Material.COBBLESTONE);
                world.getBlockAt(lx, ly + 1, lz).setType(Material.OAK_FENCE);
                world.getBlockAt(lx, ly + 2, lz).setType(Material.OAK_FENCE);
                world.getBlockAt(lx, ly + 3, lz).setType(Material.LANTERN);
            }
        }
    }
}
