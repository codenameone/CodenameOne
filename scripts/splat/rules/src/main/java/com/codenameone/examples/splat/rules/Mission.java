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
package com.codenameone.examples.splat.rules;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// A field a player built in the mission editor: one half of it, the props,
/// spawns and flag base of team 0, mirrored through the centre for team 1 by
/// [Arena.Builder], so every mission is fair by construction.
///
/// It travels as JSON (`toJson`/`fromJson`), is checked by [#problem()] on
/// both the client and the server, and is played as the arena
/// `m-<code>` once [#register()] has put it where [Catalog#arena(String)]
/// looks.
public final class Mission {
    /// The prefix of a mission's arena id.
    public static final String PREFIX = "m-";
    public static final int WIDTH_M = 40;
    public static final int DEPTH_M = 56;
    public static final int MAX_PROPS = 24;
    public static final int MAX_SPAWNS = 6;
    public static final int MAX_NAME = 40;
    public static final int MAX_BRIEFING = 400;

    /// The props a player can place: kind, footprint and height in
    /// decimetres, sized after the models the app draws (see `Catalog`).
    public static final String[] KINDS = {"sandbags", "sandbags-small", "container", "barrier",
        "barrier-single", "car", "tank", "boxes", "tires", "sofa", "dumpster", "barrel", "crate", "snackbar"};
    private static final int[][] SIZES = {{34, 9, 13}, {25, 9, 10}, {44, 21, 21}, {38, 7, 32},
        {19, 7, 6}, {26, 55, 18}, {24, 25, 21}, {13, 12, 22}, {18, 11, 11}, {34, 15, 13},
        {24, 13, 20}, {8, 8, 10}, {8, 8, 8}, {22, 21, 21}};

    /// One placed prop, centre in decimetres; `turned` lays it across.
    public static final class Prop {
        public final String kind;
        public final int x;
        public final int z;
        public final boolean turned;

        public Prop(String kind, int x, int z, boolean turned) {
            this.kind = kind;
            this.x = x;
            this.z = z;
            this.turned = turned;
        }

        /// Width along x, in decimetres.
        public int width() {
            int[] s = SIZES[kindIndex(kind)];
            return turned ? s[1] : s[0];
        }

        /// Depth along z, in decimetres.
        public int depth() {
            int[] s = SIZES[kindIndex(kind)];
            return turned ? s[0] : s[1];
        }

        public int height() {
            return SIZES[kindIndex(kind)][2];
        }
    }

    private static final Hashtable<String, Arena> REGISTERED = new Hashtable<String, Arena>();

    public String code = "";
    public String name = "";
    public String briefing = "";
    public Mode mode = Mode.TEAM_PAINT;
    public final List<Prop> props = new ArrayList<Prop>();
    /// Team 0's spawns, `{x, z}` in decimetres.
    public final List<int[]> spawns = new ArrayList<int[]>();
    public int baseX = WIDTH_M * 5;
    public int baseZ = 60;

    /// A blank field with a sensible starting line, for the editor's "new".
    public static Mission blank() {
        Mission m = new Mission();
        m.name = "Operation Untitled";
        m.briefing = "Intelligence suggests the enemy has snacks. Take them.";
        m.spawns.add(new int[] {WIDTH_M * 5 - 60, 25});
        m.spawns.add(new int[] {WIDTH_M * 5, 20});
        m.spawns.add(new int[] {WIDTH_M * 5 + 60, 25});
        m.props.add(new Prop("sandbags", WIDTH_M * 5, 130, false));
        return m;
    }

    public static int kindIndex(String kind) {
        for (int i = 0; i < KINDS.length; i++) {
            if (KINDS[i].equals(kind)) {
                return i;
            }
        }
        return -1;
    }

    /// The arena id this mission plays as.
    public String arenaId() {
        return PREFIX + code;
    }

    /// What is wrong with the mission, in words for the player, or null when
    /// it can be played. The server asks the same before it stores one.
    public String problem() {
        if (name.trim().length() == 0) {
            return "Give the operation a name.";
        }
        if (name.length() > MAX_NAME) {
            return "The name is longer than " + MAX_NAME + " characters.";
        }
        if (briefing.length() > MAX_BRIEFING) {
            return "The briefing is longer than " + MAX_BRIEFING + " characters.";
        }
        if (spawns.isEmpty()) {
            return "Place at least one spawn point.";
        }
        if (spawns.size() > MAX_SPAWNS) {
            return "No more than " + MAX_SPAWNS + " spawn points.";
        }
        if (props.size() > MAX_PROPS) {
            return "No more than " + MAX_PROPS + " props on a side.";
        }
        int half = DEPTH_M * 5;
        for (int i = 0; i < props.size(); i++) {
            Prop p = props.get(i);
            if (kindIndex(p.kind) < 0) {
                return "Unknown prop: " + p.kind;
            }
            if (p.x - p.width() / 2 < 0 || p.x + p.width() / 2 > WIDTH_M * 10
                    || p.z - p.depth() / 2 < 0 || p.z + p.depth() / 2 > half) {
                return "Every prop has to be inside your half of the field.";
            }
        }
        for (int i = 0; i < spawns.size(); i++) {
            int[] s = spawns.get(i);
            if (s[0] < 5 || s[0] > WIDTH_M * 10 - 5 || s[1] < 5 || s[1] > half) {
                return "Every spawn has to be inside your half of the field.";
            }
            if (blocks(s[0], s[1], 6)) {
                return "A spawn point is inside a prop.";
            }
        }
        if (baseX < 5 || baseX > WIDTH_M * 10 - 5 || baseZ < 5 || baseZ > half) {
            return "The flag has to be inside your half of the field.";
        }
        if (blocks(baseX, baseZ, 4)) {
            return "The flag is inside a prop.";
        }
        return null;
    }

    /// Whether a circle of `r` decimetres at `(x, z)` touches a prop.
    public boolean blocks(int x, int z, int r) {
        return propAt(x, z, r) >= 0;
    }

    /// The index of the prop under a circle at `(x, z)`, or -1.
    public int propAt(int x, int z, int r) {
        for (int i = props.size() - 1; i >= 0; i--) {
            Prop p = props.get(i);
            if (Math.abs(x - p.x) < p.width() / 2 + r && Math.abs(z - p.z) < p.depth() / 2 + r) {
                return i;
            }
        }
        return -1;
    }

    /// The whole field, both halves.
    public Arena arena() {
        Arena.Builder b = new Arena.Builder(arenaId(), name, WIDTH_M, DEPTH_M).base(baseX, baseZ).hill(40);
        for (int i = 0; i < spawns.size(); i++) {
            b.spawn(spawns.get(i)[0], spawns.get(i)[1]);
        }
        for (int i = 0; i < props.size(); i++) {
            Prop p = props.get(i);
            b.mirror(p.kind, p.x, p.z, p.width(), p.depth(), p.height());
        }
        return b.build();
    }

    /// Makes this mission's arena one [Catalog#arena(String)] finds, so a
    /// match, a replay or a lobby can name it like a built-in one.
    public Arena register() {
        Arena a = arena();
        REGISTERED.put(a.id, a);
        return a;
    }

    static Arena registered(String id) {
        return REGISTERED.get(id);
    }

    public String toJson() {
        Map<String, Object> o = new LinkedHashMap<String, Object>();
        o.put("code", code);
        o.put("name", name);
        o.put("briefing", briefing);
        o.put("mode", mode.name());
        o.put("base", ints(baseX, baseZ));
        List<Object> sp = new ArrayList<Object>();
        for (int i = 0; i < spawns.size(); i++) {
            sp.add(ints(spawns.get(i)[0], spawns.get(i)[1]));
        }
        o.put("spawns", sp);
        List<Object> ps = new ArrayList<Object>();
        for (int i = 0; i < props.size(); i++) {
            Prop p = props.get(i);
            Map<String, Object> q = new LinkedHashMap<String, Object>();
            q.put("kind", p.kind);
            q.put("x", Long.valueOf(p.x));
            q.put("z", Long.valueOf(p.z));
            q.put("turned", Boolean.valueOf(p.turned));
            ps.add(q);
        }
        o.put("props", ps);
        return Json.write(o);
    }

    private static List<Object> ints(int a, int b) {
        List<Object> l = new ArrayList<Object>();
        l.add(Long.valueOf(a));
        l.add(Long.valueOf(b));
        return l;
    }

    /// Reads a mission, or throws `IllegalArgumentException` for a document
    /// that is not one. A document that is one but cannot be played still
    /// reads: [#problem()] says why.
    @SuppressWarnings("unchecked")
    public static Mission fromJson(String text) {
        Object root = Json.parse(text);
        if (!(root instanceof Map)) {
            throw new IllegalArgumentException("not a mission");
        }
        Map<String, Object> o = (Map<String, Object>) root;
        Mission m = new Mission();
        m.code = str(o.get("code"));
        m.name = str(o.get("name"));
        m.briefing = str(o.get("briefing"));
        m.mode = Mode.byName(str(o.get("mode")));
        int[] base = pair(o.get("base"));
        m.baseX = base[0];
        m.baseZ = base[1];
        for (Object s : list(o.get("spawns"))) {
            if (m.spawns.size() > MAX_SPAWNS) {
                break;
            }
            m.spawns.add(pair(s));
        }
        for (Object p : list(o.get("props"))) {
            if (m.props.size() > MAX_PROPS) {
                break;
            }
            if (!(p instanceof Map)) {
                throw new IllegalArgumentException("not a prop");
            }
            Map<String, Object> q = (Map<String, Object>) p;
            m.props.add(new Prop(str(q.get("kind")), num(q.get("x")), num(q.get("z")),
                    Boolean.TRUE.equals(q.get("turned"))));
        }
        return m;
    }

    private static String str(Object v) {
        return v instanceof String ? (String) v : "";
    }

    private static int num(Object v) {
        if (!(v instanceof Long)) {
            throw new IllegalArgumentException("not a number");
        }
        long n = ((Long) v).longValue();
        return (int) Math.max(-100000, Math.min(100000, n));
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object v) {
        return v instanceof List ? (List<Object>) v : new ArrayList<Object>();
    }

    private static int[] pair(Object v) {
        List<Object> l = list(v);
        if (l.size() != 2) {
            throw new IllegalArgumentException("not a point");
        }
        return new int[] {num(l.get(0)), num(l.get(1))};
    }
}
