package com.canopycreations.mysticcraft.world;

import org.bukkit.Material;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Nothing in the generator previously knew what had already been built.
 * Every structure placed blindly, which is why houses ended up inside each
 * other, paths ran through walls, and cottages landed on top of landmarks.
 *
 * This is the registry that fixes it. Every footprint - streets, landmarks,
 * houses, even front paths - gets reserved here before anything is placed,
 * and any later placement that would overlap is refused.
 */
public class Occupancy {

    /** An axis-aligned reserved rectangle, in world coordinates. */
    public record Rect(int minX, int minZ, int maxX, int maxZ, String owner) {
        public boolean overlaps(Rect other) {
            return minX <= other.maxX && maxX >= other.minX
                && minZ <= other.maxZ && maxZ >= other.minZ;
        }
        public boolean contains(int x, int z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }
    }

    private final List<Rect> reserved = new ArrayList<>();

    /**
     * Tries to reserve a footprint centred on (x,z). The margin is the gap
     * that must exist between this structure and anything already placed.
     *
     * @return true if reserved, false if it would overlap something.
     */
    public boolean reserve(int x, int z, int halfW, int halfL, int margin, String owner) {
        Rect candidate = new Rect(
                x - halfW - margin, z - halfL - margin,
                x + halfW + margin, z + halfL + margin,
                owner);

        for (Rect existing : reserved) {
            if (candidate.overlaps(existing)) return false;
        }
        // Store the actual footprint, not the margin-padded probe, so
        // structures can sit margin-apart rather than 2x margin apart.
        reserved.add(new Rect(x - halfW, z - halfL, x + halfW, z + halfL, owner));
        return true;
    }

    /** Reserves without checking - for streets, which are laid first and win. */
    public void reserveUnchecked(int minX, int minZ, int maxX, int maxZ, String owner) {
        reserved.add(new Rect(
                Math.min(minX, maxX), Math.min(minZ, maxZ),
                Math.max(minX, maxX), Math.max(minZ, maxZ), owner));
    }

    /** Reserves the full run of a street, including its verges. */
    public void reserveStreet(TownPlan.Street s) {
        int pad = s.width() + 1;
        reserveUnchecked(
                Math.min(s.x1(), s.x2()) - pad, Math.min(s.z1(), s.z2()) - pad,
                Math.max(s.x1(), s.x2()) + pad, Math.max(s.z1(), s.z2()) + pad,
                "street");
    }

    /** True if anything is reserved at this point. */
    public boolean isOccupied(int x, int z) {
        for (Rect r : reserved) {
            if (r.contains(x, z)) return true;
        }
        return false;
    }

    /** True if anything owned by the given name is reserved here. */
    public boolean isOccupiedBy(int x, int z, String owner) {
        for (Rect r : reserved) {
            if (r.contains(x, z) && r.owner().equals(owner)) return true;
        }
        return false;
    }

    public int count() {
        return reserved.size();
    }

    // ------------------------------------------------------------------
    // Terrain suitability
    // ------------------------------------------------------------------

    /**
     * Rejects plots on ground too uneven to build on.
     *
     * This is the other half of the "mismatched heightmap" problem: a
     * building placed across a 12-block drop ends up with half its walls
     * buried and half floating on a foundation pillar. Better not to build
     * there at all and let the town have a gap.
     *
     * @param maxDrop the biggest height difference tolerated across the footprint
     */
    public static boolean isBuildable(World world, int x, int z,
                                      int halfW, int halfL, int maxDrop) {
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;

        // Sample the corners and the middle rather than every block.
        int[][] samples = {
                {0, 0},
                {-halfW, -halfL}, {halfW, -halfL}, {-halfW, halfL}, {halfW, halfL},
                {-halfW, 0}, {halfW, 0}, {0, -halfL}, {0, halfL}
        };

        for (int[] s : samples) {
            int wx = x + s[0], wz = z + s[1];
            int y = Blueprint.groundY(world, wx, wz);

            // Refuse to build in or over water.
            Material at = world.getBlockAt(wx, y, wz).getType();
            if (at == Material.WATER || at == Material.ICE) return false;
            if (y < world.getSeaLevel()) return false;

            min = Math.min(min, y);
            max = Math.max(max, y);
        }
        return (max - min) <= maxDrop;
    }

    /**
     * The average ground height across a footprint. Building at this level
     * rather than at the centre point's height means a structure sits fairly
     * on uneven ground instead of being pinned to whatever the middle block
     * happened to be.
     */
    public static int averageGround(World world, int x, int z, int halfW, int halfL) {
        int total = 0, n = 0;
        for (int dx = -halfW; dx <= halfW; dx += Math.max(1, halfW)) {
            for (int dz = -halfL; dz <= halfL; dz += Math.max(1, halfL)) {
                total += Blueprint.groundY(world, x + dx, z + dz);
                n++;
            }
        }
        return n == 0 ? Blueprint.groundY(world, x, z) : Math.round((float) total / n);
    }
}
