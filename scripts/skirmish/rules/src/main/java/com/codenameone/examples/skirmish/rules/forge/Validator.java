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
import com.codenameone.examples.skirmish.rules.CardSet;
import com.codenameone.examples.skirmish.rules.Deck;
import com.codenameone.examples.skirmish.rules.Match;
import com.codenameone.examples.skirmish.rules.MatchRules;

import java.util.ArrayList;
import java.util.List;

/// Decides whether a pack can be played, and says what is wrong if not.
///
/// The editor runs it as the creator works, the server before it accepts a
/// publish, and the app before it loads a pack someone shared. Packs come from
/// strangers, so every number has a range: the limits keep a pack from stalling a
/// phone, hanging a server's match room or breaking the economy, not from being
/// strange -- a 1-elixir card with 9000 hit points is allowed.
public final class Validator {
    // tag::skirmish-forge-limits[]
    public static final int MAX_CARDS = 40;
    public static final int MAX_ARENAS = 10;
    public static final int MAX_MISSIONS = 60;
    public static final int MAX_CAMPAIGNS = 10;
    public static final int MAX_CAMPAIGN_MISSIONS = 30;
    public static final int MAX_ASSETS = 40;
    public static final int MAX_DIALOG = 60;
    public static final int MAX_TEXT = 280;
    public static final int MAX_LONG_TEXT = 1200;
    /// A model travels base64 encoded in one request, under the server's 8 MB body limit.
    public static final int MAX_MODEL_BYTES = 5 * 1024 * 1024;
    public static final int MAX_IMAGE_BYTES = 1024 * 1024;
    /// What any one mission may pay, on top of the server's caps for packs that
    /// are not official.
    public static final int MAX_REWARD_COINS = 500;
    public static final int MAX_REWARD_XP = 200;
    // end::skirmish-forge-limits[]

    private final List<String> problems = new ArrayList<String>();
    private final ContentPack pack;
    private CardSet cards;

    private Validator(ContentPack pack) {
        this.pack = pack;
    }

    /// Every problem with `pack`, as sentences naming where; empty when it can be played.
    public static List<String> check(ContentPack pack) {
        Validator v = new Validator(pack);
        v.run();
        return v.problems;
    }

    public static boolean isValid(ContentPack pack) {
        return check(pack).isEmpty();
    }

    private void fail(String where, String what) {
        problems.add(where + ": " + what);
    }

    private void range(String where, String field, int v, int min, int max) {
        if (v < min || v > max) {
            fail(where, field + " must be between " + min + " and " + max + " (is " + v + ")");
        }
    }

    private void text(String where, String field, String v, int min, int max) {
        int len = v == null ? 0 : v.length();
        if (len < min || len > max) {
            fail(where, min > 0 ? field + " needs 1 to " + max + " characters" : field + " is longer than " + max);
        }
    }

    /// Lower-case letters, digits and dashes.
    public static boolean isSlug(String s, int min, int max) {
        if (s == null || s.length() < min || s.length() > max) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-')) {
                return false;
            }
        }
        return true;
    }

    private static boolean isHex64(String s) {
        if (s == null || s.length() != 64) {
            return false;
        }
        for (int i = 0; i < 64; i++) {
            char c = s.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) {
                return false;
            }
        }
        return true;
    }

    private void run() {
        if (!isSlug(pack.id, 3, 40)) {
            fail("pack", "id must be 3 to 40 lower-case letters, digits or dashes");
        }
        text("pack", "name", pack.name, 1, 60);
        text("pack", "author", pack.author, 0, 60);
        text("pack", "description", pack.description, 0, MAX_LONG_TEXT);
        if (pack.cards.size() > MAX_CARDS || pack.arenas.size() > MAX_ARENAS || pack.missions.size() > MAX_MISSIONS
                || pack.campaigns.size() > MAX_CAMPAIGNS || pack.assets.size() > MAX_ASSETS) {
            fail("pack", "at most " + MAX_CARDS + " cards, " + MAX_ARENAS + " arenas, " + MAX_MISSIONS
                    + " missions, " + MAX_CAMPAIGNS + " campaigns and " + MAX_ASSETS + " files");
            return;
        }
        if (pack.missions.isEmpty() && pack.cards.isEmpty() && pack.arenas.isEmpty()) {
            fail("pack", "is empty");
        }
        checkAssets();
        try {
            cards = pack.cardSet();
        } catch (IllegalArgumentException e) {
            fail("cards", e.getMessage());
            return;
        }
        for (int i = 0; i < pack.cards.size(); i++) {
            checkCard(pack.cards.get(i), i);
        }
        for (int i = 0; i < pack.arenas.size(); i++) {
            checkArena(pack.arenas.get(i), i);
        }
        for (int i = 0; i < pack.missions.size(); i++) {
            checkMission(pack.missions.get(i), i);
        }
        for (int i = 0; i < pack.campaigns.size(); i++) {
            checkCampaign(pack.campaigns.get(i), i);
        }
        duplicates();
    }

    private void duplicates() {
        List<String> seen = new ArrayList<String>();
        for (int i = 0; i < pack.cards.size(); i++) {
            unique(seen, "card", pack.cards.get(i).id);
        }
        seen.clear();
        seen.add("classic");
        for (int i = 0; i < pack.arenas.size(); i++) {
            unique(seen, "arena", pack.arenas.get(i).id());
        }
        seen.clear();
        for (int i = 0; i < pack.missions.size(); i++) {
            unique(seen, "mission", pack.missions.get(i).id);
        }
        seen.clear();
        for (int i = 0; i < pack.campaigns.size(); i++) {
            unique(seen, "campaign", pack.campaigns.get(i).id);
        }
    }

    private void unique(List<String> seen, String what, String id) {
        if (seen.contains(id)) {
            fail(what + " " + id, "id is used twice");
        }
        seen.add(id);
    }

    private void checkAssets() {
        for (int i = 0; i < pack.assets.size(); i++) {
            AssetRef a = pack.assets.get(i);
            String where = "file " + (a.name.length() > 0 ? a.name : String.valueOf(i + 1));
            if (!isHex64(a.sha256)) {
                fail(where, "has no valid checksum");
            }
            text(where, "name", a.name, 1, 80);
            if (AssetRef.MODEL.equals(a.kind)) {
                range(where, "size", a.bytes, 1, MAX_MODEL_BYTES);
            } else if (AssetRef.IMAGE.equals(a.kind)) {
                range(where, "size", a.bytes, 1, MAX_IMAGE_BYTES);
            } else {
                fail(where, "is neither a model nor an image");
            }
        }
    }

    private void checkCard(CardDef c, int index) {
        String where = "card " + (c.id.length() > 0 ? c.id : String.valueOf(index + 1));
        if (!isSlug(c.id, 2, 32)) {
            fail(where, "id must be 2 to 32 lower-case letters, digits or dashes");
        }
        text(where, "name", c.name, 1, 32);
        text(where, "description", c.description(), 0, MAX_TEXT);
        if (c.kind < CardDef.TROOP || c.kind > CardDef.TOWER) {
            fail(where, "has no kind");
            return;
        }
        range(where, "cost", c.cost, 0, 10);
        if (c.kind != CardDef.SPELL) {
            range(where, "hit points", c.hp(), 1, 20000);
            range(where, "size", c.radius(), 200, 3000);
        }
        range(where, "damage", c.damage(), 0, 5000);
        range(where, "attack time", c.hitTicks(), 5, 200);
        range(where, "range", c.range(), 0, 12000);
        range(where, "sight", c.sight(), 0, 12000);
        range(where, "speed", c.speed(), 0, 150);
        range(where, "count", c.count(), 1, 12);
        range(where, "spread", c.spread(), 0, 2000);
        range(where, "splash", c.splash(), 0, 6000);
        range(where, "lifetime", c.lifetimeTicks(), 0, 6000);
        range(where, "tower damage", c.towerPercent(), 0, 100);
        range(where, "deploy time", c.deployTicks(), 0, 100);
        range(where, "scale", c.scalePercent(), 25, 400);
        if (c.tint() < -1 || c.tint() > 0xffffff) {
            fail(where, "tint is not a colour");
        }
        if (c.kind == CardDef.TROOP && c.speed() <= 0) {
            fail(where, "a troop needs a speed above 0");
        }
        if (c.kind == CardDef.SPELL && c.splash() <= 0) {
            fail(where, "a spell needs a splash radius");
        }
        if (c.spawns() != null) {
            CardDef s = cards.get(c.spawns());
            if (c.kind != CardDef.BUILDING) {
                fail(where, "only a building can spawn");
            } else if (s == null || s.kind != CardDef.TROOP) {
                // A spawned building could spawn buildings in turn; troops only.
                fail(where, "can only spawn a troop");
            }
            range(where, "spawn time", c.spawnTicks(), 20, 2000);
        }
        String model = c.model();
        if (model != null) {
            if (model.startsWith("asset:")) {
                AssetRef a = pack.asset(model.substring(6));
                if (a == null || !AssetRef.MODEL.equals(a.kind)) {
                    fail(where, "uses a model that is not in the pack");
                }
                if (c.clips() == null || c.clips().length() > 200) {
                    fail(where, "an uploaded model needs its animations chosen");
                }
            } else if (!isModelName(model)) {
                fail(where, "model name is not valid");
            }
        }
    }

    private static boolean isModelName(String s) {
        if (s.length() == 0 || s.length() > 40) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_'
                    || c == '-')) {
                return false;
            }
        }
        return true;
    }

    private void checkArena(Arena a, int index) {
        String where = "arena " + (a.id().length() > 0 ? a.id() : String.valueOf(index + 1));
        if (!isSlug(a.id(), 2, 32)) {
            fail(where, "id must be 2 to 32 lower-case letters, digits or dashes");
        }
        text(where, "name", a.name(), 1, 40);
        if (!isSlug(a.theme(), 1, 20)) {
            fail(where, "theme is not valid");
        }
        int t = Arena.TILE;
        range(where, "width", a.width(), Arena.MIN_TILES_WIDE * t, Arena.MAX_TILES_WIDE * t);
        range(where, "height", a.height(), Arena.MIN_TILES_TALL * t, Arena.MAX_TILES_TALL * t);
        if (a.riverTop() + a.riverBottom() != a.height() || a.riverBottom() - a.riverTop() < t
                || a.riverBottom() - a.riverTop() > 6 * t) {
            fail(where, "the river must cross the middle, 1 to 6 tiles wide");
        }
        range(where, "lanes", a.laneCount(), 1, Arena.MAX_LANES);
        for (int i = 0; i < a.laneCount(); i++) {
            range(where, "lane " + (i + 1), a.laneX(i), t, a.width() - t);
            if (i > 0 && a.laneX(i) - a.laneX(i - 1) < 3 * t) {
                fail(where, "lanes must be listed left to right, 3 tiles apart");
            }
        }
        range(where, "bridge width", a.bridgeHalfWidth(), 300, 3000);
        range(where, "towers", a.towers().size(), 2, Arena.MAX_TOWERS);
        range(where, "obstacles", a.obstacles().size(), 0, Arena.MAX_OBSTACLES);
        int[] kings = new int[2];
        int lastOwner = 0;
        for (int i = 0; i < a.towers().size(); i++) {
            Arena.Tower tw = a.towers().get(i);
            String tw0 = where + " tower " + (i + 1);
            if (tw.owner != 0 && tw.owner != 1) {
                fail(tw0, "belongs to nobody");
                continue;
            }
            if (tw.owner < lastOwner) {
                fail(where, "towers must list player 0's before player 1's");
            }
            lastOwner = tw.owner;
            CardDef c = cards.get(tw.card);
            if (c == null || c.kind != CardDef.TOWER) {
                fail(tw0, "is not a tower card");
            }
            if (tw.x < 0 || tw.x > a.width() || !a.ownHalf(tw.owner, tw.y)) {
                fail(tw0, "must stand in its owner's half");
            }
            if (tw.king) {
                kings[tw.owner]++;
            }
        }
        if (kings[0] != 1 || kings[1] != 1) {
            fail(where, "each side needs exactly one king tower");
        }
        for (int i = 0; i < a.obstacles().size(); i++) {
            Arena.Obstacle o = a.obstacles().get(i);
            String o0 = where + " obstacle " + (i + 1);
            if (!isSlug(o.kind, 1, 20)) {
                fail(o0, "kind is not valid");
            }
            range(o0, "size", o.radius, 200, 3000);
            if (o.x < 0 || o.x > a.width() || o.y < 0 || o.y > a.height()) {
                fail(o0, "is off the field");
            }
        }
    }

    private boolean deckOk(String[] deck) {
        return deck == null || cards.isValidDeck(deck);
    }

    private void checkMission(MissionDef m, int index) {
        String where = "mission " + (m.name.length() > 0 ? m.name : String.valueOf(index + 1));
        if (!isSlug(m.id, 2, 32)) {
            fail(where, "id must be 2 to 32 lower-case letters, digits or dashes");
        }
        text(where, "name", m.name, 1, 40);
        text(where, "briefing", m.briefing, 0, MAX_LONG_TEXT);
        Arena a = pack.arena(m.arena);
        if (a == null) {
            fail(where, "uses an arena that is not in the pack");
            return;
        }
        if (!deckOk(m.playerDeck)) {
            fail(where, "the player's deck must be 8 different playable cards");
        }
        if (!deckOk(m.enemyDeck)) {
            fail(where, "the enemy's deck must be 8 different playable cards");
        }
        if (m.enemyDeck == null && m.waves.isEmpty()) {
            fail(where, "the enemy needs a deck or at least one wave");
        }
        range(where, "bot speed", m.botThinkTicks, 2, 200);
        range(where, "length", m.seconds, MatchRules.MIN_TICKS / Match.TICKS_PER_SECOND,
                MatchRules.MAX_TICKS / Match.TICKS_PER_SECOND);
        range(where, "overtime", m.overtimeSeconds, 0, 180);
        range(where, "double elixir", m.doubleElixirFromSecond, 0, 600);
        range(where, "player's starting elixir", m.playerStartElixir, 0, 10);
        range(where, "enemy's starting elixir", m.enemyStartElixir, 0, 10);
        range(where, "player's elixir rate", m.playerElixirPercent, 25, 400);
        range(where, "enemy's elixir rate", m.enemyElixirPercent, 25, 400);
        range(where, "objective", m.objective, MatchRules.CROWNS, MatchRules.RAID);
        range(where, "crowns to win", m.objectiveCount, 1, 3);
        range(where, "waves", m.waves.size(), 0, MatchRules.MAX_WAVES);
        range(where, "dialog", m.dialog.size(), 0, MAX_DIALOG);
        int maxTick = (m.seconds + (m.objective == MatchRules.CROWNS ? m.overtimeSeconds : 0))
                * Match.TICKS_PER_SECOND;
        for (int i = 0; i < m.waves.size(); i++) {
            MatchRules.Wave w = m.waves.get(i);
            String w0 = where + " wave " + (i + 1);
            CardDef c = cards.get(w.card);
            if (c == null || !c.isPlayable()) {
                fail(w0, "plays a card that does not exist");
            }
            if (w.player != 0 && w.player != 1) {
                fail(w0, "belongs to nobody");
            }
            range(w0, "time", w.tick, 0, maxTick);
            if (w.x < 0 || w.x > a.width() || w.y < 0 || w.y > a.height()) {
                fail(w0, "is off the field");
            }
        }
        int last = -1;
        for (int i = 0; i < m.dialog.size(); i++) {
            DialogStep s = m.dialog.get(i);
            String s0 = where + " line " + (i + 1);
            text(s0, "text", s.text, 1, MAX_TEXT);
            text(s0, "speaker", s.speaker, 0, 32);
            range(s0, "time", s.tick, 0, maxTick);
            if (s.tick < last) {
                fail(s0, "comes before the line above it");
            }
            last = s.tick;
            if (s.playCard != null) {
                if (m.playerDeck == null) {
                    fail(s0, "can only ask for a card when the mission sets the player's deck");
                } else if (!contains(m.playerDeck, s.playCard)) {
                    fail(s0, "asks for a card that is not in the player's deck");
                }
                if (!s.freeze) {
                    fail(s0, "must stop the clock to ask for a card");
                }
            }
            if ((s.targetX >= 0) != (s.targetY >= 0) || s.targetX > a.width() || s.targetY > a.height()) {
                fail(s0, "points off the field");
            }
        }
        int ownTowers = 0;
        for (int i = 0; i < a.towers().size(); i++) {
            if (a.towers().get(i).owner == 0) {
                ownTowers++;
            }
        }
        range(where, "towers for two stars", m.twoStarTowers, 0, ownTowers);
        range(where, "seconds for three stars", m.threeStarSeconds, 0, m.seconds);
        range(where, "coins", m.rewardCoins, 0, MAX_REWARD_COINS);
        range(where, "experience", m.rewardXp, 0, MAX_REWARD_XP);
    }

    private static boolean contains(String[] deck, String id) {
        for (int i = 0; i < deck.length; i++) {
            if (deck[i].equals(id)) {
                return true;
            }
        }
        return false;
    }

    private void checkCampaign(CampaignDef c, int index) {
        String where = "campaign " + (c.name.length() > 0 ? c.name : String.valueOf(index + 1));
        if (!isSlug(c.id, 2, 32)) {
            fail(where, "id must be 2 to 32 lower-case letters, digits or dashes");
        }
        text(where, "name", c.name, 1, 40);
        text(where, "intro", c.intro, 0, MAX_LONG_TEXT);
        text(where, "ending", c.outro, 0, MAX_LONG_TEXT);
        range(where, "missions", c.missions.size(), 1, MAX_CAMPAIGN_MISSIONS);
        List<String> seen = new ArrayList<String>();
        for (int i = 0; i < c.missions.size(); i++) {
            String id = c.missions.get(i);
            if (pack.mission(id) == null) {
                fail(where, "lists a mission that is not in the pack: " + id);
            }
            if (seen.contains(id)) {
                fail(where, "lists " + id + " twice");
            }
            seen.add(id);
        }
        range(where, "stars for the finale", c.finaleStars, 0, 3 * MAX_CAMPAIGN_MISSIONS);
    }

    /// The deck size a mission's fixed decks must have; here so the editor can say it.
    public static int deckSize() {
        return Deck.SIZE;
    }
}
