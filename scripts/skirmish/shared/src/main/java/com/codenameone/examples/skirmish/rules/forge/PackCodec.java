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
package com.codenameone.examples.skirmish.rules.forge;

import com.codenameone.examples.skirmish.rules.Arena;
import com.codenameone.examples.skirmish.rules.CardDef;
import com.codenameone.examples.skirmish.rules.MatchRules;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Writes a [ContentPack] as JSON and reads one back.
///
/// Writing is canonical: every field, in a fixed order, so the same pack is always
/// the same text and hashes the same everywhere. Reading is defensive: a missing
/// field takes its default, a field of the wrong type or a value outside what the
/// reader can hold fails with the field's path. Whether the values make a playable
/// pack is [Validator]'s question, not this class's.
public final class PackCodec {
    private PackCodec() {
    }

    // ------------------------------------------------------------------ writing

    public static String write(ContentPack p) {
        return Json.write(toMap(p));
    }

    public static Map<String, Object> toMap(ContentPack p) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("format", Long.valueOf(ContentPack.FORMAT));
        m.put("id", p.id);
        m.put("name", p.name);
        m.put("author", p.author);
        m.put("description", p.description);
        m.put("version", Long.valueOf(p.version));
        List<Object> cards = new ArrayList<Object>();
        for (int i = 0; i < p.cards.size(); i++) {
            cards.add(card(p.cards.get(i)));
        }
        m.put("cards", cards);
        List<Object> arenas = new ArrayList<Object>();
        for (int i = 0; i < p.arenas.size(); i++) {
            arenas.add(arena(p.arenas.get(i)));
        }
        m.put("arenas", arenas);
        List<Object> missions = new ArrayList<Object>();
        for (int i = 0; i < p.missions.size(); i++) {
            missions.add(mission(p.missions.get(i)));
        }
        m.put("missions", missions);
        List<Object> campaigns = new ArrayList<Object>();
        for (int i = 0; i < p.campaigns.size(); i++) {
            CampaignDef c = p.campaigns.get(i);
            Map<String, Object> cm = new LinkedHashMap<String, Object>();
            cm.put("id", c.id);
            cm.put("name", c.name);
            cm.put("intro", c.intro);
            cm.put("outro", c.outro);
            cm.put("missions", new ArrayList<Object>(c.missions));
            cm.put("finaleStars", Long.valueOf(c.finaleStars));
            campaigns.add(cm);
        }
        m.put("campaigns", campaigns);
        List<Object> assets = new ArrayList<Object>();
        for (int i = 0; i < p.assets.size(); i++) {
            AssetRef a = p.assets.get(i);
            Map<String, Object> am = new LinkedHashMap<String, Object>();
            am.put("sha256", a.sha256);
            am.put("kind", a.kind);
            am.put("name", a.name);
            am.put("bytes", Long.valueOf(a.bytes));
            assets.add(am);
        }
        m.put("assets", assets);
        return m;
    }

    private static Long n(int v) {
        return Long.valueOf(v);
    }

    public static Map<String, Object> card(CardDef c) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", c.id);
        m.put("name", c.name);
        m.put("kind", n(c.kind));
        m.put("cost", n(c.cost));
        m.put("hp", n(c.hp()));
        m.put("damage", n(c.damage()));
        m.put("hitTicks", n(c.hitTicks()));
        m.put("range", n(c.range()));
        m.put("sight", n(c.sight()));
        m.put("speed", n(c.speed()));
        m.put("targets", n(c.targets()));
        m.put("flying", Boolean.valueOf(c.isFlying()));
        m.put("buildingsOnly", Boolean.valueOf(c.isBuildingsOnly()));
        m.put("splash", n(c.splash()));
        m.put("count", n(c.count()));
        m.put("spread", n(c.spread()));
        m.put("radius", n(c.radius()));
        m.put("spawns", c.spawns());
        m.put("spawnTicks", n(c.spawnTicks()));
        m.put("lifetimeTicks", n(c.lifetimeTicks()));
        m.put("towerPercent", n(c.towerPercent()));
        m.put("deployTicks", n(c.deployTicks()));
        m.put("model", c.model());
        m.put("tint", n(c.tint()));
        m.put("scalePercent", n(c.scalePercent()));
        m.put("description", c.description());
        m.put("clips", c.clips());
        return m;
    }

    private static Map<String, Object> arena(Arena a) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", a.id());
        m.put("name", a.name());
        m.put("theme", a.theme());
        m.put("width", n(a.width()));
        m.put("height", n(a.height()));
        m.put("riverTop", n(a.riverTop()));
        m.put("riverBottom", n(a.riverBottom()));
        List<Object> lanes = new ArrayList<Object>();
        for (int i = 0; i < a.laneCount(); i++) {
            lanes.add(n(a.laneX(i)));
        }
        m.put("lanes", lanes);
        m.put("bridgeHalfWidth", n(a.bridgeHalfWidth()));
        List<Object> towers = new ArrayList<Object>();
        for (int i = 0; i < a.towers().size(); i++) {
            Arena.Tower t = a.towers().get(i);
            Map<String, Object> tm = new LinkedHashMap<String, Object>();
            tm.put("owner", n(t.owner));
            tm.put("card", t.card);
            tm.put("x", n(t.x));
            tm.put("y", n(t.y));
            tm.put("king", Boolean.valueOf(t.king));
            towers.add(tm);
        }
        m.put("towers", towers);
        List<Object> rocks = new ArrayList<Object>();
        for (int i = 0; i < a.obstacles().size(); i++) {
            Arena.Obstacle o = a.obstacles().get(i);
            Map<String, Object> om = new LinkedHashMap<String, Object>();
            om.put("kind", o.kind);
            om.put("x", n(o.x));
            om.put("y", n(o.y));
            om.put("radius", n(o.radius));
            rocks.add(om);
        }
        m.put("obstacles", rocks);
        return m;
    }

    private static Object deck(String[] d) {
        if (d == null) {
            return null;
        }
        List<Object> out = new ArrayList<Object>();
        for (int i = 0; i < d.length; i++) {
            out.add(d[i]);
        }
        return out;
    }

    private static Map<String, Object> mission(MissionDef d) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", d.id);
        m.put("name", d.name);
        m.put("briefing", d.briefing);
        m.put("arena", d.arena);
        m.put("playerDeck", deck(d.playerDeck));
        m.put("enemyDeck", deck(d.enemyDeck));
        m.put("botThinkTicks", n(d.botThinkTicks));
        m.put("seconds", n(d.seconds));
        m.put("overtimeSeconds", n(d.overtimeSeconds));
        m.put("doubleElixirFromSecond", n(d.doubleElixirFromSecond));
        m.put("playerStartElixir", n(d.playerStartElixir));
        m.put("enemyStartElixir", n(d.enemyStartElixir));
        m.put("playerElixirPercent", n(d.playerElixirPercent));
        m.put("enemyElixirPercent", n(d.enemyElixirPercent));
        m.put("objective", n(d.objective));
        m.put("objectiveCount", n(d.objectiveCount));
        m.put("kingsAsleep", Boolean.valueOf(d.kingsAsleep));
        List<Object> waves = new ArrayList<Object>();
        for (int i = 0; i < d.waves.size(); i++) {
            MatchRules.Wave w = d.waves.get(i);
            Map<String, Object> wm = new LinkedHashMap<String, Object>();
            wm.put("tick", n(w.tick));
            wm.put("player", n(w.player));
            wm.put("card", w.card);
            wm.put("x", n(w.x));
            wm.put("y", n(w.y));
            waves.add(wm);
        }
        m.put("waves", waves);
        List<Object> dialog = new ArrayList<Object>();
        for (int i = 0; i < d.dialog.size(); i++) {
            DialogStep s = d.dialog.get(i);
            Map<String, Object> sm = new LinkedHashMap<String, Object>();
            sm.put("tick", n(s.tick));
            sm.put("speaker", s.speaker);
            sm.put("text", s.text);
            sm.put("freeze", Boolean.valueOf(s.freeze));
            sm.put("playCard", s.playCard);
            sm.put("targetX", n(s.targetX));
            sm.put("targetY", n(s.targetY));
            dialog.add(sm);
        }
        m.put("dialog", dialog);
        m.put("twoStarTowers", n(d.twoStarTowers));
        m.put("threeStarSeconds", n(d.threeStarSeconds));
        m.put("rewardCoins", n(d.rewardCoins));
        m.put("rewardXp", n(d.rewardXp));
        return m;
    }

    // ------------------------------------------------------------------ reading

    public static ContentPack read(String json) {
        return fromMap(new Reader(Json.parse(json), "pack"));
    }

    private static ContentPack fromMap(Reader r) {
        int format = r.integer("format", ContentPack.FORMAT);
        if (format > ContentPack.FORMAT) {
            throw new IllegalArgumentException("pack format " + format + " is newer than this app");
        }
        ContentPack p = new ContentPack();
        p.id = r.string("id", "");
        p.name = r.string("name", "");
        p.author = r.string("author", "");
        p.description = r.string("description", "");
        p.version = r.integer("version", 0);
        List<Reader> cards = r.list("cards");
        for (int i = 0; i < cards.size(); i++) {
            p.cards.add(readCard(cards.get(i)));
        }
        List<Reader> arenas = r.list("arenas");
        for (int i = 0; i < arenas.size(); i++) {
            p.arenas.add(readArena(arenas.get(i)));
        }
        List<Reader> missions = r.list("missions");
        for (int i = 0; i < missions.size(); i++) {
            p.missions.add(readMission(missions.get(i)));
        }
        List<Reader> campaigns = r.list("campaigns");
        for (int i = 0; i < campaigns.size(); i++) {
            Reader c = campaigns.get(i);
            CampaignDef d = new CampaignDef();
            d.id = c.string("id", "");
            d.name = c.string("name", "");
            d.intro = c.string("intro", "");
            d.outro = c.string("outro", "");
            String[] ids = c.strings("missions");
            for (int k = 0; ids != null && k < ids.length; k++) {
                d.missions.add(ids[k]);
            }
            d.finaleStars = c.integer("finaleStars", 0);
            p.campaigns.add(d);
        }
        List<Reader> assets = r.list("assets");
        for (int i = 0; i < assets.size(); i++) {
            Reader a = assets.get(i);
            AssetRef ref = new AssetRef();
            ref.sha256 = a.string("sha256", "");
            ref.kind = a.string("kind", AssetRef.MODEL);
            ref.name = a.string("name", "");
            ref.bytes = a.integer("bytes", 0);
            p.assets.add(ref);
        }
        return p;
    }

    public static CardDef readCard(Reader r) {
        CardDef c = new CardDef(r.string("id", ""), r.string("name", ""), r.integer("kind", CardDef.TROOP),
                r.integer("cost", 3));
        c.hp(r.integer("hp", 100)).damage(r.integer("damage", 0)).hitTicks(r.integer("hitTicks", 20))
                .range(r.integer("range", 600)).sight(r.integer("sight", 5500)).speed(r.integer("speed", 0))
                .targets(r.integer("targets", 1)).splash(r.integer("splash", 0)).count(r.integer("count", 1))
                .spread(r.integer("spread", 400)).radius(r.integer("radius", 400))
                .lifetime(r.integer("lifetimeTicks", 0)).towerPercent(r.integer("towerPercent", 100))
                .deploy(r.integer("deployTicks", 20)).model(r.string("model", null))
                .tint(r.integer("tint", -1)).scalePercent(r.integer("scalePercent", 100))
                .description(r.string("description", "")).clips(r.string("clips", null));
        if (r.bool("flying", false)) {
            c.flying();
        }
        if (r.bool("buildingsOnly", false)) {
            c.buildingsOnly();
        }
        String spawns = r.string("spawns", null);
        if (spawns != null) {
            c.spawns(spawns, r.integer("spawnTicks", 0));
        }
        return c;
    }

    private static Arena readArena(Reader r) {
        int[] lanes = r.ints("lanes");
        List<Arena.Tower> towers = new ArrayList<Arena.Tower>();
        List<Reader> tl = r.list("towers");
        for (int i = 0; i < tl.size(); i++) {
            Reader t = tl.get(i);
            towers.add(new Arena.Tower(t.integer("owner", 0), t.string("card", ""), t.integer("x", 0),
                    t.integer("y", 0), t.bool("king", false)));
        }
        List<Arena.Obstacle> rocks = new ArrayList<Arena.Obstacle>();
        List<Reader> ol = r.list("obstacles");
        for (int i = 0; i < ol.size(); i++) {
            Reader o = ol.get(i);
            rocks.add(new Arena.Obstacle(o.string("kind", "rock"), o.integer("x", 0), o.integer("y", 0),
                    o.integer("radius", 500)));
        }
        return new Arena(r.string("id", ""), r.string("name", ""), r.string("theme", "meadow"),
                r.integer("width", 18 * Arena.TILE), r.integer("height", 32 * Arena.TILE),
                r.integer("riverTop", 15 * Arena.TILE), r.integer("riverBottom", 17 * Arena.TILE),
                lanes == null ? new int[0] : lanes, r.integer("bridgeHalfWidth", Arena.TILE), towers, rocks);
    }

    private static MissionDef readMission(Reader r) {
        MissionDef d = new MissionDef();
        d.id = r.string("id", "");
        d.name = r.string("name", "");
        d.briefing = r.string("briefing", "");
        d.arena = r.string("arena", "classic");
        d.playerDeck = r.strings("playerDeck");
        d.enemyDeck = r.strings("enemyDeck");
        d.botThinkTicks = r.integer("botThinkTicks", 12);
        d.seconds = r.integer("seconds", 180);
        d.overtimeSeconds = r.integer("overtimeSeconds", 60);
        d.doubleElixirFromSecond = r.integer("doubleElixirFromSecond", 120);
        d.playerStartElixir = r.integer("playerStartElixir", 5);
        d.enemyStartElixir = r.integer("enemyStartElixir", 5);
        d.playerElixirPercent = r.integer("playerElixirPercent", 100);
        d.enemyElixirPercent = r.integer("enemyElixirPercent", 100);
        d.objective = r.integer("objective", MatchRules.CROWNS);
        d.objectiveCount = r.integer("objectiveCount", 3);
        d.kingsAsleep = r.bool("kingsAsleep", true);
        List<Reader> waves = r.list("waves");
        for (int i = 0; i < waves.size(); i++) {
            Reader w = waves.get(i);
            d.waves.add(new MatchRules.Wave(w.integer("tick", 0), w.integer("player", 1), w.string("card", ""),
                    w.integer("x", 0), w.integer("y", 0)));
        }
        List<Reader> dialog = r.list("dialog");
        for (int i = 0; i < dialog.size(); i++) {
            Reader s = dialog.get(i);
            DialogStep step = new DialogStep();
            step.tick = s.integer("tick", 0);
            step.speaker = s.string("speaker", "elda");
            step.text = s.string("text", "");
            step.freeze = s.bool("freeze", true);
            step.playCard = s.string("playCard", null);
            step.targetX = s.integer("targetX", -1);
            step.targetY = s.integer("targetY", -1);
            d.dialog.add(step);
        }
        d.twoStarTowers = r.integer("twoStarTowers", 2);
        d.threeStarSeconds = r.integer("threeStarSeconds", 0);
        d.rewardCoins = r.integer("rewardCoins", 50);
        d.rewardXp = r.integer("rewardXp", 20);
        return d;
    }

    /// Typed access to a parsed object, naming the path of anything wrong.
    public static final class Reader {
        private final Map<String, Object> map;
        private final String path;

        @SuppressWarnings("unchecked")
        public Reader(Object value, String path) {
            if (!(value instanceof Map)) {
                throw new IllegalArgumentException(path + ": expected an object");
            }
            this.map = (Map<String, Object>) value;
            this.path = path;
        }

        public String string(String key, String def) {
            Object v = map.get(key);
            if (v == null) {
                return def;
            }
            if (!(v instanceof String)) {
                throw new IllegalArgumentException(path + "." + key + ": expected text");
            }
            return (String) v;
        }

        public int integer(String key, int def) {
            Object v = map.get(key);
            if (v == null) {
                return def;
            }
            if (!(v instanceof Long)) {
                throw new IllegalArgumentException(path + "." + key + ": expected a whole number");
            }
            long l = ((Long) v).longValue();
            if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
                throw new IllegalArgumentException(path + "." + key + ": out of range");
            }
            return (int) l;
        }

        public boolean bool(String key, boolean def) {
            Object v = map.get(key);
            if (v == null) {
                return def;
            }
            if (!(v instanceof Boolean)) {
                throw new IllegalArgumentException(path + "." + key + ": expected true or false");
            }
            return ((Boolean) v).booleanValue();
        }

        private List<?> raw(String key) {
            Object v = map.get(key);
            if (v == null) {
                return null;
            }
            if (!(v instanceof List)) {
                throw new IllegalArgumentException(path + "." + key + ": expected a list");
            }
            return (List<?>) v;
        }

        public List<Reader> list(String key) {
            List<?> l = raw(key);
            List<Reader> out = new ArrayList<Reader>();
            for (int i = 0; l != null && i < l.size(); i++) {
                out.add(new Reader(l.get(i), path + "." + key + "[" + i + "]"));
            }
            return out;
        }

        public String[] strings(String key) {
            List<?> l = raw(key);
            if (l == null) {
                return null;
            }
            String[] out = new String[l.size()];
            for (int i = 0; i < out.length; i++) {
                Object v = l.get(i);
                if (!(v instanceof String)) {
                    throw new IllegalArgumentException(path + "." + key + "[" + i + "]: expected text");
                }
                out[i] = (String) v;
            }
            return out;
        }

        public int[] ints(String key) {
            List<?> l = raw(key);
            if (l == null) {
                return null;
            }
            int[] out = new int[l.size()];
            for (int i = 0; i < out.length; i++) {
                Object v = l.get(i);
                if (!(v instanceof Long)) {
                    throw new IllegalArgumentException(path + "." + key + "[" + i + "]: expected a whole number");
                }
                long x = ((Long) v).longValue();
                if (x < Integer.MIN_VALUE || x > Integer.MAX_VALUE) {
                    throw new IllegalArgumentException(path + "." + key + "[" + i + "]: out of range");
                }
                out[i] = (int) x;
            }
            return out;
        }
    }
}
