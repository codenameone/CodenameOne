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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// A level as JSON: what the editor saves, the server stores and a share link
/// carries. A level may bring its own obstacles and pickups; their ids must be
/// namespaced `u:<anything>:<id>` so a community level can never redefine a
/// built-in one. Each may name `art`, an asset id the client resolves to a sprite
/// or a glTF model.
///
/// Distances in the document are millimetres, exactly as the rules hold them.
public final class LevelCodec {
    /// A decoded level, its own catalog entries and the art they name.
    public static final class Decoded {
        public final LevelDef level;
        public final Catalog custom;
        public final Map<String, String> art;
        /// The runner model the level asks for, or null for the player's own.
        public final String runnerArt;

        Decoded(LevelDef level, Catalog custom, Map<String, String> art, String runnerArt) {
            this.level = level;
            this.custom = custom;
            this.art = art;
            this.runnerArt = runnerArt;
        }

        /// The built-in catalog plus this level's own entries.
        public Catalog catalog() {
            return Content.catalog().with(custom);
        }
    }

    public static final String CUSTOM_PREFIX = "u:";

    private LevelCodec() {
    }

    public static String encode(LevelDef d) {
        return encode(d, null, null, null);
    }

    /// `customObstacles`/`customPickups` may be null; `art` maps a definition id
    /// to its asset id.
    public static String encode(LevelDef d, List<Object> customDefs, Map<String, String> art, String runnerArt) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("format", Long.valueOf(1));
        m.put("id", d.id);
        m.put("name", d.name);
        m.put("theme", d.theme);
        m.put("lanes", num(d.lanes));
        m.put("startSpeed", num(d.startSpeed));
        m.put("maxSpeed", num(d.maxSpeed));
        m.put("accel", num(d.accel));
        m.put("length", num(d.length));
        m.put("goal", d.goal.name());
        m.put("goalValue", num(d.goalValue));
        m.put("proceduralFrom", num(d.proceduralFrom));
        m.put("difficultyStep", num(d.difficultyStep));
        List<Object> stars = new ArrayList<Object>();
        for (int i = 0; i < d.stars.length; i++) {
            stars.add(num(d.stars[i]));
        }
        m.put("stars", stars);
        m.put("placements", placements(d.placements));
        List<Object> chunks = new ArrayList<Object>();
        for (int i = 0; i < d.chunks.size(); i++) {
            Chunk c = d.chunks.get(i);
            Map<String, Object> cm = new LinkedHashMap<String, Object>();
            cm.put("id", c.id);
            cm.put("length", num(c.length));
            cm.put("minDifficulty", num(c.minDifficulty));
            cm.put("placements", placements(c.placements()));
            chunks.add(cm);
        }
        m.put("chunks", chunks);
        List<Object> cues = new ArrayList<Object>();
        for (int i = 0; i < d.cues.size(); i++) {
            ViewCue c = d.cues.get(i);
            Map<String, Object> cm = new LinkedHashMap<String, Object>();
            cm.put("at", num(c.at));
            cm.put("mode", c.mode.name());
            cm.put("blend", num(c.blendMillis));
            cues.add(cm);
        }
        m.put("cues", cues);
        List<Object> dialog = new ArrayList<Object>();
        for (int i = 0; i < d.dialog.size(); i++) {
            DialogLine l = d.dialog.get(i);
            Map<String, Object> lm = new LinkedHashMap<String, Object>();
            lm.put("at", num(l.at));
            lm.put("speaker", l.speaker);
            lm.put("text", l.text);
            lm.put("hint", num(l.hint));
            dialog.add(lm);
        }
        m.put("dialog", dialog);
        List<Object> defs = new ArrayList<Object>();
        if (customDefs != null) {
            for (int i = 0; i < customDefs.size(); i++) {
                Object o = customDefs.get(i);
                Map<String, Object> dm = new LinkedHashMap<String, Object>();
                if (o instanceof ObstacleDef) {
                    ObstacleDef od = (ObstacleDef) o;
                    dm.put("kind", "obstacle");
                    dm.put("id", od.id);
                    dm.put("clearance", od.clearance.name());
                    dm.put("depth", num(od.depth));
                    dm.put("height", num(od.height));
                    dm.put("lethal", Boolean.valueOf(od.lethal));
                } else if (o instanceof PickupDef) {
                    PickupDef pd = (PickupDef) o;
                    dm.put("kind", "pickup");
                    dm.put("id", pd.id);
                    dm.put("pickup", pd.kind.name());
                    dm.put("points", num(pd.points));
                    dm.put("ticks", num(pd.ticks));
                } else {
                    continue;
                }
                String a = art == null ? null : art.get(dm.get("id"));
                if (a != null) {
                    dm.put("art", a);
                }
                defs.add(dm);
            }
        }
        m.put("defs", defs);
        if (runnerArt != null) {
            m.put("runnerArt", runnerArt);
        }
        return Json.write(m);
    }

    private static List<Object> placements(List<Placement> ps) {
        List<Object> out = new ArrayList<Object>();
        for (int i = 0; i < ps.size(); i++) {
            Placement p = ps.get(i);
            List<Object> row = new ArrayList<Object>();
            row.add(num(p.z));
            row.add(p.defId);
            row.add(num(p.lane));
            out.add(row);
        }
        return out;
    }

    private static Long num(int v) {
        return Long.valueOf(v);
    }

    /// Parses a level. Throws `IllegalArgumentException` on anything malformed;
    /// it does not judge whether the level is any good, which is [Validator]'s job.
    public static Decoded decode(String json) {
        Object root = Json.parse(json);
        if (!(root instanceof Map)) {
            throw new IllegalArgumentException("a level is an object");
        }
        Map<?, ?> m = (Map<?, ?>) root;
        LevelDef d = new LevelDef(str(m, "id", null), str(m, "name", "Untitled"));
        d.theme = str(m, "theme", d.theme);
        d.lanes = integer(m, "lanes", d.lanes);
        d.startSpeed = integer(m, "startSpeed", d.startSpeed);
        d.maxSpeed = integer(m, "maxSpeed", d.maxSpeed);
        d.accel = integer(m, "accel", d.accel);
        d.length = integer(m, "length", d.length);
        d.goal = goal(str(m, "goal", d.goal.name()));
        d.goalValue = integer(m, "goalValue", 0);
        d.proceduralFrom = integer(m, "proceduralFrom", 0);
        d.difficultyStep = integer(m, "difficultyStep", d.difficultyStep);
        List<?> stars = list(m, "stars");
        if (stars.size() == 3) {
            d.stars = new int[] {asInt(stars.get(0)), asInt(stars.get(1)), asInt(stars.get(2))};
        }
        readPlacements(list(m, "placements"), d.placements);
        List<?> chunks = list(m, "chunks");
        for (int i = 0; i < chunks.size(); i++) {
            Map<?, ?> cm = map(chunks.get(i));
            Chunk c = new Chunk(str(cm, "id", "chunk" + i), integer(cm, "length", 20 * Track.UNIT),
                    integer(cm, "minDifficulty", 0));
            List<Placement> ps = new ArrayList<Placement>();
            readPlacements(list(cm, "placements"), ps);
            for (int j = 0; j < ps.size(); j++) {
                c.add(ps.get(j).z, ps.get(j).defId, ps.get(j).lane);
            }
            d.chunks.add(c);
        }
        List<?> cues = list(m, "cues");
        for (int i = 0; i < cues.size(); i++) {
            Map<?, ?> cm = map(cues.get(i));
            d.cues.add(new ViewCue(integer(cm, "at", 0), view(str(cm, "mode", "CHASE_3D")), integer(cm, "blend", 0)));
        }
        List<?> dialog = list(m, "dialog");
        for (int i = 0; i < dialog.size(); i++) {
            Map<?, ?> lm = map(dialog.get(i));
            d.dialog.add(new DialogLine(integer(lm, "at", 0), str(lm, "speaker", ""), str(lm, "text", ""),
                    integer(lm, "hint", 0) & Input.ALL));
        }
        Catalog custom = new Catalog();
        Map<String, String> art = new LinkedHashMap<String, String>();
        List<?> defs = list(m, "defs");
        for (int i = 0; i < defs.size(); i++) {
            Map<?, ?> dm = map(defs.get(i));
            String id = str(dm, "id", "");
            if (!id.startsWith(CUSTOM_PREFIX) || Content.catalog().has(id)) {
                throw new IllegalArgumentException("custom ids start with u: (" + id + ")");
            }
            if ("obstacle".equals(str(dm, "kind", ""))) {
                custom.add(new ObstacleDef(id, clearance(str(dm, "clearance", "JUMP")), integer(dm, "depth", 300),
                        integer(dm, "height", 600), Boolean.TRUE.equals(dm.get("lethal"))));
            } else if ("pickup".equals(str(dm, "kind", ""))) {
                custom.add(new PickupDef(id, pickupKind(str(dm, "pickup", "COIN")), integer(dm, "points", 10),
                        integer(dm, "ticks", 0)));
            } else {
                throw new IllegalArgumentException("unknown def kind for " + id);
            }
            Object a = dm.get("art");
            if (a instanceof String) {
                art.put(id, (String) a);
            }
        }
        Object runner = m.get("runnerArt");
        return new Decoded(d, custom, art, runner instanceof String ? (String) runner : null);
    }

    private static void readPlacements(List<?> rows, List<Placement> into) {
        for (int i = 0; i < rows.size(); i++) {
            Object r = rows.get(i);
            if (!(r instanceof List) || ((List<?>) r).size() != 3) {
                throw new IllegalArgumentException("a placement is [z, id, lane]");
            }
            List<?> row = (List<?>) r;
            Object id = row.get(1);
            if (!(id instanceof String)) {
                throw new IllegalArgumentException("a placement names a definition");
            }
            into.add(new Placement(asInt(row.get(0)), (String) id, asInt(row.get(2))));
        }
    }

    private static Goal goal(String s) {
        Goal[] all = Goal.values();
        for (int i = 0; i < all.length; i++) {
            if (all[i].name().equals(s)) {
                return all[i];
            }
        }
        throw new IllegalArgumentException("goal " + s);
    }

    private static ViewMode view(String s) {
        ViewMode[] all = ViewMode.values();
        for (int i = 0; i < all.length; i++) {
            if (all[i].name().equals(s)) {
                return all[i];
            }
        }
        throw new IllegalArgumentException("view " + s);
    }

    private static Clearance clearance(String s) {
        Clearance[] all = Clearance.values();
        for (int i = 0; i < all.length; i++) {
            if (all[i].name().equals(s)) {
                return all[i];
            }
        }
        throw new IllegalArgumentException("clearance " + s);
    }

    private static PickupKind pickupKind(String s) {
        PickupKind[] all = PickupKind.values();
        for (int i = 0; i < all.length; i++) {
            if (all[i].name().equals(s)) {
                return all[i];
            }
        }
        throw new IllegalArgumentException("pickup " + s);
    }

    private static Map<?, ?> map(Object o) {
        if (!(o instanceof Map)) {
            throw new IllegalArgumentException("expected an object");
        }
        return (Map<?, ?>) o;
    }

    private static List<?> list(Map<?, ?> m, String key) {
        Object v = m.get(key);
        if (v == null) {
            return new ArrayList<Object>();
        }
        if (!(v instanceof List)) {
            throw new IllegalArgumentException(key + " is a list");
        }
        return (List<?>) v;
    }

    private static String str(Map<?, ?> m, String key, String def) {
        Object v = m.get(key);
        return v instanceof String ? (String) v : def;
    }

    private static int integer(Map<?, ?> m, String key, int def) {
        Object v = m.get(key);
        return v == null ? def : asInt(v);
    }

    private static int asInt(Object v) {
        if (!(v instanceof Long)) {
            throw new IllegalArgumentException("expected a whole number");
        }
        long l = ((Long) v).longValue();
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("number out of range");
        }
        return (int) l;
    }
}
