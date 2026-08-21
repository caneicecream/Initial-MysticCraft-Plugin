package com.canopycreations.mysticcraft.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * The plan of Ashfall.
 *
 * Version one placed landmarks at offsets and drew roads to them, which
 * produced a starburst. Version two laid streets first, which fixed
 * orientation but was far too small and left forest standing between every
 * building - it read as a clearing with things in it.
 *
 * This version is built around how a small town is actually organised:
 *
 *   DOWNTOWN    a tight grid, buildings shoulder to shoulder with no yards,
 *               shopfronts hard against the pavement, land fully cleared.
 *   RESIDENTIAL streets running out from the core, houses set back with
 *               gardens between them, trees thinning but still present.
 *   OUTSKIRTS   estates, the church, the cemetery. Long lanes, deep lots,
 *               woodland left standing between properties.
 *
 * The footprint is roughly 900 x 900. That is deliberately not convenient
 * to cross. A town you can walk end to end in twenty seconds doesn't feel
 * like anywhere people live.
 */
public class TownPlan {

    public enum Facing {
        NORTH(0, -1), SOUTH(0, 1), EAST(1, 0), WEST(-1, 0);
        public final int dx, dz;
        Facing(int dx, int dz) { this.dx = dx; this.dz = dz; }
        public Facing opposite() {
            return switch (this) {
                case NORTH -> SOUTH; case SOUTH -> NORTH;
                case EAST -> WEST;   case WEST -> EAST;
            };
        }
    }

    /** What kind of place a plot sits in. Drives building type and spacing. */
    public enum District { DOWNTOWN, RESIDENTIAL, OUTSKIRTS }

    public record Street(int x1, int z1, int x2, int z2, int width,
                         boolean major, District district) {
        public boolean isVertical() { return x1 == x2; }
        public int length() { return Math.abs(x2 - x1) + Math.abs(z2 - z1); }
    }

    public record Plot(int x, int z, Facing facing, int width, int depth,
                       double distance, District district) {}

    private final List<Street> streets = new ArrayList<>();
    private final List<Plot> plots = new ArrayList<>();
    private final int cx, cz;
    private final Random rng;

    private static final int DOWNTOWN_R    = 75;
    private static final int RESIDENTIAL_R = 220;

    public TownPlan(int centerX, int centerZ, Random rng) {
        this.cx = centerX;
        this.cz = centerZ;
        this.rng = rng;
        layStreets();
        derivePlots();
    }

    private District districtAt(double distance) {
        if (distance <= DOWNTOWN_R) return District.DOWNTOWN;
        if (distance <= RESIDENTIAL_R) return District.RESIDENTIAL;
        return District.OUTSKIRTS;
    }

    private void layStreets() {
        // Main Street and the cross, running the full length of the town.
        streets.add(new Street(cx, cz - 300, cx, cz + 300, 4, true, District.DOWNTOWN));
        streets.add(new Street(cx - 300, cz, cx + 300, cz, 4, true, District.DOWNTOWN));

        // Downtown: a tight grid. Blocks ~32 across is what puts shopfronts
        // close enough together to read as a commercial street.
        for (int off = 32; off <= DOWNTOWN_R; off += 32) {
            streets.add(new Street(cx - off, cz - DOWNTOWN_R, cx - off, cz + DOWNTOWN_R, 3, false, District.DOWNTOWN));
            streets.add(new Street(cx + off, cz - DOWNTOWN_R, cx + off, cz + DOWNTOWN_R, 3, false, District.DOWNTOWN));
            streets.add(new Street(cx - DOWNTOWN_R, cz - off, cx + DOWNTOWN_R, cz - off, 3, false, District.DOWNTOWN));
            streets.add(new Street(cx - DOWNTOWN_R, cz + off, cx + DOWNTOWN_R, cz + off, 3, false, District.DOWNTOWN));
        }

        // Residential: wider spacing so houses get gardens, and the grid
        // shortens as it goes out rather than marching on forever.
        int[] resOffsets = {110, 165, 215};
        for (int off : resOffsets) {
            int span = RESIDENTIAL_R - (off - 110) / 2;
            streets.add(new Street(cx - off, cz - span, cx - off, cz + span, 3, false, District.RESIDENTIAL));
            streets.add(new Street(cx + off, cz - span, cx + off, cz + span, 3, false, District.RESIDENTIAL));
            streets.add(new Street(cx - span, cz - off, cx + span, cz - off, 3, false, District.RESIDENTIAL));
            streets.add(new Street(cx - span, cz + off, cx + span, cz + off, 3, false, District.RESIDENTIAL));
        }

        // Diagonals so the residential grid isn't purely square.
        streets.add(new Street(cx - 165, cz - 110, cx - 110, cz - 165, 2, false, District.RESIDENTIAL));
        streets.add(new Street(cx + 165, cz + 110, cx + 110, cz + 165, 2, false, District.RESIDENTIAL));
        streets.add(new Street(cx - 165, cz + 110, cx - 110, cz + 165, 2, false, District.RESIDENTIAL));

        // Country lanes leave from the ENDS of real streets rather than
        // radiating out of the square, which is what caused the spokes.
        streets.add(new Street(cx, cz - 300, cx - 120, cz - 400, 2, false, District.OUTSKIRTS));
        streets.add(new Street(cx, cz - 300, cx + 150, cz - 380, 2, false, District.OUTSKIRTS));
        streets.add(new Street(cx - 300, cz, cx - 430, cz + 90, 2, false, District.OUTSKIRTS));
        streets.add(new Street(cx + 300, cz, cx + 420, cz + 130, 2, false, District.OUTSKIRTS));
        streets.add(new Street(cx, cz + 300, cx + 90, cz + 420, 2, false, District.OUTSKIRTS));
    }

    private void derivePlots() {
        for (Street s : streets) {
            District d = districtAt(Math.hypot(
                    (s.x1() + s.x2()) / 2.0 - cx, (s.z1() + s.z2()) / 2.0 - cz));

            int spacing = switch (d) {
                case DOWNTOWN -> 11;
                case RESIDENTIAL -> 20;
                case OUTSKIRTS -> 55;
            };
            int setback = switch (d) {
                case DOWNTOWN -> 1;     // shopfronts hard against the pavement
                case RESIDENTIAL -> 6;
                case OUTSKIRTS -> 14;
            };

            // A plot's coordinate is the building's CENTRE. To keep the
            // building's front wall `setback` blocks clear of the road, the
            // centre has to sit that far out PLUS the building's own
            // half-depth. Getting this wrong put every plot on top of the
            // street and made every placement fail.
            int typicalDepth = switch (d) {
                case DOWNTOWN -> 7;
                case RESIDENTIAL -> 7;
                case OUTSKIRTS -> 10;
            };

            int steps = s.length() / spacing;
            for (int i = 1; i < steps; i++) {
                double t = (double) i / steps;
                int px = (int) Math.round(s.x1() + (s.x2() - s.x1()) * t);
                int pz = (int) Math.round(s.z1() + (s.z2() - s.z1()) * t);
                int offset = s.width() + setback + typicalDepth;

                if (s.isVertical()) {
                    addPlot(px + offset, pz, Facing.WEST);
                    addPlot(px - offset, pz, Facing.EAST);
                } else {
                    addPlot(px, pz + offset, Facing.NORTH);
                    addPlot(px, pz - offset, Facing.SOUTH);
                }
            }
        }
        plots.sort(Comparator.comparingDouble(Plot::distance));
    }

    private void addPlot(int x, int z, Facing facing) {
        double dist = Math.hypot(x - cx, z - cz);
        District d = districtAt(dist);

        int minGap = switch (d) {
            case DOWNTOWN -> 9;
            case RESIDENTIAL -> 17;
            case OUTSKIRTS -> 45;
        };
        for (Plot existing : plots) {
            if (Math.hypot(existing.x() - x, existing.z() - z) < minGap) return;
        }

        int w, dep;
        switch (d) {
            case DOWNTOWN -> { w = 4 + rng.nextInt(2); dep = 5 + rng.nextInt(3); }
            case RESIDENTIAL -> { w = 5 + rng.nextInt(3); dep = 5 + rng.nextInt(3); }
            default -> { w = 8 + rng.nextInt(4); dep = 7 + rng.nextInt(4); }
        }
        plots.add(new Plot(x, z, facing, w, dep, dist, d));
    }

    public List<Street> getStreets() { return streets; }
    public List<Plot> getPlots() { return plots; }
    public int getCenterX() { return cx; }
    public int getCenterZ() { return cz; }
    public static int downtownRadius() { return DOWNTOWN_R; }
    public static int residentialRadius() { return RESIDENTIAL_R; }

    /** Takes a plot in a given district, nearest the centre first. */
    public Plot takePlot(District district, int minWidth, int minDepth) {
        for (int i = 0; i < plots.size(); i++) {
            Plot p = plots.get(i);
            if (p.district() != district) continue;
            if (p.width() < minWidth || p.depth() < minDepth) continue;
            return plots.remove(i);
        }
        return null;
    }

    public Plot takeAnyPlot() {
        return plots.isEmpty() ? null : plots.remove(0);
    }

    /**
     * Downtown is essentially fully built. Residential thins with distance.
     * The outskirts are mostly empty land with the occasional property.
     */
    public boolean shouldBuild(Plot p, Random rng) {
        return switch (p.district()) {
            case DOWNTOWN -> rng.nextDouble() < 0.94;
            case RESIDENTIAL -> {
                double t = (p.distance() - DOWNTOWN_R) / (double) (RESIDENTIAL_R - DOWNTOWN_R);
                yield rng.nextDouble() < (0.85 - t * 0.45);
            }
            case OUTSKIRTS -> rng.nextDouble() < 0.35;
        };
    }
}
