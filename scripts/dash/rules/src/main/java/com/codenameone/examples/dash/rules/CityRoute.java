/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.dash.rules;

import java.util.ArrayList;
import java.util.List;

/// A run down real streets: the legs of a route a map found between two
/// points, turned into a level. Each leg is a stretch of one street; a turn
/// into the next is a corner the runner has to take on the side it turns to,
/// with the street's name called out before it, and what lies between the
/// corners comes from the game's own chunks.
///
/// Nothing here knows about maps. The app reduces a route to this spec, and
/// the spec is all the server needs: [#level()] is deterministic, so the
/// server builds the very same level from the [#encode()]d spec a result
/// carries, and verifies the run on it as it would any other.
public final class CityRoute {
    /// What every city level's id starts with.
    public static final String ID_PREFIX = "city-";
    public static final int LEFT = -1;
    public static final int STRAIGHT = 0;
    public static final int RIGHT = 1;
    /// The shortest and longest run a route becomes. A longer route is run
    /// to scale, so a 6 km route is a 2.5 km level with its corners in the
    /// same proportion.
    public static final int MIN_METRES = 300;
    public static final int MAX_METRES = 2500;
    public static final int MAX_LEGS = 60;
    public static final int MAX_NAME = 32;
    private static final int VERSION = 1;
    private static final String[] THEMES = {"daybreak", "neon", "ember"};
    /// Clear track before the first obstacle, and around every corner.
    private static final int LEAD_IN = 40;
    private static final int CORNER = 24;

    /// One street of the route: how long it is, which way the runner turned
    /// into it, and its name (empty when the map has none).
    public static final class Leg {
        public final int metres;
        public final int turn;
        public final String street;

        Leg(int metres, int turn, String street) {
            this.metres = metres;
            this.turn = turn;
            this.street = street;
        }
    }

    public final String place;
    private final List<Leg> legs = new ArrayList<Leg>();

    /// A route through `place`, a name for the area shown on the level.
    public CityRoute(String place) {
        this.place = clean(place);
    }

    /// Adds the next street. `turn` is the turn into it -- [#LEFT],
    /// [#RIGHT] or [#STRAIGHT] -- and is ignored for the first.
    public CityRoute leg(int metres, int turn, String street) {
        if (legs.size() >= MAX_LEGS) {
            throw new IllegalArgumentException("a route has at most " + MAX_LEGS + " streets");
        }
        if (metres < 0 || metres > 100000) {
            throw new IllegalArgumentException("a street is 0 to 100 km long");
        }
        if (turn < LEFT || turn > RIGHT) {
            throw new IllegalArgumentException("a turn is left, right or straight on");
        }
        legs.add(new Leg(metres, legs.isEmpty() ? STRAIGHT : turn, clean(street)));
        return this;
    }

    public List<Leg> legs() {
        return legs;
    }

    /// The real length of the route, in metres.
    public int metres() {
        int m = 0;
        for (int i = 0; i < legs.size(); i++) {
            m += legs.get(i).metres;
        }
        return m;
    }

    /// The spec as one line: `1~place~metres,turn,street~...`. Names never
    /// hold a separator; [#clean] removed them.
    public String encode() {
        StringBuilder sb = new StringBuilder();
        sb.append(VERSION).append('~').append(place);
        for (int i = 0; i < legs.size(); i++) {
            Leg l = legs.get(i);
            sb.append('~').append(l.metres).append(',').append(l.turn).append(',').append(l.street);
        }
        return sb.toString();
    }

    /// Reads [#encode()]'s line back. Throws `IllegalArgumentException` for
    /// anything else, since a server decodes what a client sent.
    public static CityRoute decode(String s) {
        if (s == null || s.length() > 4096) {
            throw new IllegalArgumentException("not a city route");
        }
        List<String> parts = Text.split(s, '~');
        if (parts.size() < 3 || !String.valueOf(VERSION).equals(parts.get(0))) {
            throw new IllegalArgumentException("not a city route");
        }
        CityRoute r = new CityRoute(parts.get(1));
        for (int i = 2; i < parts.size(); i++) {
            List<String> f = Text.split(parts.get(i), ',');
            if (f.size() != 3) {
                throw new IllegalArgumentException("not a city route");
            }
            try {
                r.leg(Integer.parseInt(f.get(0)), Integer.parseInt(f.get(1)), f.get(2));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("not a city route");
            }
        }
        if (r.metres() < 1) {
            throw new IllegalArgumentException("an empty route");
        }
        return r;
    }

    /// The level's id: the same spec always names the same level, so a
    /// route has one board however many players find it.
    public String id() {
        String h = Long.toString(new Hash().add(encode()).value() >>> 1, 16);
        return ID_PREFIX + h;
    }

    /// The level this route runs as. Deterministic: the same spec builds the
    /// same placements, on the app and on the server.
    public LevelDef level() {
        int real = Math.max(1, metres());
        int total = Math.max(MIN_METRES, Math.min(MAX_METRES, real));
        LevelDef d = new LevelDef(id(), place.length() > 0 ? place : "City run");
        long h = new Hash().add(encode()).value();
        d.theme = THEMES[(int) ((h >>> 1) % THEMES.length)];
        d.length = total * Track.UNIT;
        d.stars = new int[] {total * 6 / 10, total * 9 / 10, total * 11 / 10};
        d.cue(0, ViewMode.CHASE_3D, 0);
        Rng rng = new Rng(h);
        List<Chunk> pool = Content.chunks();
        int start = 0;
        int lastCorner = -1000;
        for (int i = 0; i < legs.size(); i++) {
            Leg l = legs.get(i);
            // To scale, the last leg running to the finish exactly.
            int end = i == legs.size() - 1 ? total : (int) ((long) runUpTo(i + 1) * total / real);
            String street = l.street.length() > 0 ? l.street : "the next street";
            if (i == 0) {
                d.dialog.add(new DialogLine(5 * Track.UNIT, "Vee",
                        (l.street.length() > 0 ? l.street : "The street") + ". Go!", 0));
            } else if (l.turn != STRAIGHT) {
                // A street too short to run down between two turns is passed
                // rather than cornered twice in a few metres.
                // One right at the line would sit on the finish coins.
                if (start - lastCorner >= 2 * CORNER + 20 && start <= total - CORNER) {
                    corner(d, start, l.turn, street);
                    lastCorner = start;
                }
            } else if (start > LEAD_IN && l.street.length() > 0) {
                d.dialog.add(new DialogLine((start - 20) * Track.UNIT, "Vee", "Straight on into " + street + ".", 0));
            }
            fill(d, pool, rng, Math.max(LEAD_IN, start + CORNER), end - CORNER, total);
            start = end;
        }
        // A row of coins over the line.
        for (int m = total - 12; m < total - 2; m += 3) {
            d.place(m, Content.COIN, Placement.ALL_LANES);
        }
        return d;
    }

    private int runUpTo(int legCount) {
        int m = 0;
        for (int i = 0; i < legCount; i++) {
            m += legs.get(i).metres;
        }
        return m;
    }

    /// A turn at `at`: haulers block every lane but the one on the side the
    /// street turns to, with coins drawing the runner across, and the call
    /// comes in time to move.
    private static void corner(LevelDef d, int at, int turn, String street) {
        if (at < LEAD_IN) {
            return;
        }
        d.dialog.add(new DialogLine(Math.max(1, at - 45) * Track.UNIT, "Vee",
                (turn == LEFT ? "Left onto " : "Right onto ") + street + "!", turn == LEFT ? Input.LEFT : Input.RIGHT));
        int open = turn == LEFT ? 0 : d.lanes - 1;
        for (int lane = 0; lane < d.lanes; lane++) {
            if (lane != open) {
                d.place(at, Content.HAULER, lane);
            }
        }
        for (int m = at - 8; m <= at + 4; m += 3) {
            d.place(m, Content.COIN, open);
        }
    }

    /// Chunks from the game's pool between `from` and `to`, harder as the
    /// run goes on, with a breath between each.
    private static void fill(LevelDef d, List<Chunk> pool, Rng rng, int from, int to, int total) {
        int m = from;
        while (true) {
            int difficulty = m * 7 / Math.max(1, total);
            List<Chunk> fits = new ArrayList<Chunk>();
            for (int i = 0; i < pool.size(); i++) {
                Chunk c = pool.get(i);
                if (c.minDifficulty <= difficulty && m + c.length / Track.UNIT <= to) {
                    fits.add(c);
                }
            }
            if (fits.isEmpty()) {
                return;
            }
            Chunk c = fits.get(rng.nextInt(fits.size()));
            for (int j = 0; j < c.placements().size(); j++) {
                Placement p = c.placements().get(j);
                d.placements.add(new Placement(m * Track.UNIT + p.z, p.defId, p.lane));
            }
            m += c.length / Track.UNIT + 8;
        }
    }

    /// A name made safe for the spec and the screen: no separators or
    /// control characters, trimmed, at most [#MAX_NAME] characters.
    static String clean(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length() && sb.length() < MAX_NAME; i++) {
            char c = s.charAt(i);
            if (c >= ' ' && c != '~' && c != ',' && c != '|' && c != 0x7f) {
                sb.append(c);
            }
        }
        return sb.toString().trim();
    }
}
