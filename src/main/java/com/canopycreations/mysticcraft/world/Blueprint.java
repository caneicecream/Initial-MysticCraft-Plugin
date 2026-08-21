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
     * A road between two points, following terrain height. Roads are what
     * turn a scatter of buildings into a town.
     */
    public void road(int x1, int z1, int x2, int z2, int width, Material surface, Material edge) {
        double dist = Math.hypot(x2 - x1, z2 - z1);
        int steps = (int) Math.ceil(dist);
        if (steps == 0) return;

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            int x = (int) Math.round(x1 + (x2 - x1) * t);
            int z = (int) Math.round(z1 + (z2 - z1) * t);
            int y = world.getHighestBlockYAt(x, z);

            for (int ox = -width; ox <= width; ox++) {
                for (int oz = -width; oz <= width; oz++) {
                    if (ox * ox + oz * oz > width * width) continue;
                    Block b = world.getBlockAt(x + ox, y, z + oz);
                    boolean rim = ox * ox + oz * oz > (width - 1) * (width - 1);
                    b.setType(rim ? edge : surface);

                    // Clear headroom so roads cut through hills.
                    for (int cy = 1; cy <= 4; cy++) {
                        Block above = world.getBlockAt(x + ox, y + cy, z + oz);
                        if (above.getType() != Material.AIR) above.setType(Material.AIR);
                    }
                }
            }

            // Occasional lamp posts.
            if (i % 12 == 0) {
                world.getBlockAt(x + width + 1, y + 1, z).setType(Material.OAK_FENCE);
                world.getBlockAt(x + width + 1, y + 2, z).setType(Material.OAK_FENCE);
                world.getBlockAt(x + width + 1, y + 3, z).setType(Material.LANTERN);
            }
        }
    }
}
