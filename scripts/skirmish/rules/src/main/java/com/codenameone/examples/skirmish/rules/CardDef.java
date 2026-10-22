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
package com.codenameone.examples.skirmish.rules;

/// The numbers behind one card: everything the simulation knows about it.
///
/// A card is data. Behaviour comes from its [#kind] and these values, which is why
/// rebalancing a card, or adding one, is an edit to [Cards] and nothing else: the
/// server, the app, offline practice and every replay pick it up from there.
///
/// Lengths are milli-tiles, durations are ticks ([Match#TICKS_PER_SECOND] a second).
public final class CardDef {
    public static final int TROOP = 0;
    public static final int BUILDING = 1;
    public static final int SPELL = 2;
    public static final int TOWER = 3;

    public final String id;
    public final String name;
    public final int kind;
    public final int cost;

    int hp;
    int damage;
    int hitTicks = 20;
    int range = 600;
    int sight = 5500;
    int speed;
    int targets = TargetKind.GROUND;
    boolean flying;
    boolean buildingsOnly;
    int splash;
    int count = 1;
    int spread = 400;
    int radius = 400;
    String spawns;
    int spawnTicks;
    int lifetimeTicks;
    int towerPercent = 100;
    int deployTicks = 20;

    // How the card looks. None of these reach the simulation or its hash.
    String model;
    int tint = -1;
    int scalePercent = 100;
    String description = "";
    String clips;

    public CardDef(String id, String name, int kind, int cost) {
        this.id = id;
        this.name = name;
        this.kind = kind;
        this.cost = cost;
    }

    public CardDef hp(int v) { hp = v; return this; }
    public CardDef damage(int v) { damage = v; return this; }
    public CardDef hitTicks(int v) { hitTicks = v; return this; }
    public CardDef range(int v) { range = v; return this; }
    public CardDef sight(int v) { sight = v; return this; }
    public CardDef speed(int v) { speed = v; return this; }
    public CardDef targets(int v) { targets = v; return this; }
    public CardDef flying() { flying = true; return this; }
    public CardDef buildingsOnly() { buildingsOnly = true; targets = TargetKind.BUILDINGS; return this; }
    public CardDef splash(int v) { splash = v; return this; }
    public CardDef count(int v) { count = v; return this; }
    public CardDef radius(int v) { radius = v; return this; }
    public CardDef spawns(String card, int every) { spawns = card; spawnTicks = every; return this; }
    public CardDef lifetime(int v) { lifetimeTicks = v; return this; }
    public CardDef towerPercent(int v) { towerPercent = v; return this; }
    public CardDef deploy(int v) { deployTicks = v; return this; }
    public CardDef spread(int v) { spread = v; return this; }

    /// The glTF character drawn for the card: a bundled model's name, or
    /// `asset:<sha256>` for one a creator uploaded. Null means the client's default.
    public CardDef model(String v) { model = v; return this; }
    /// An RGB tint multiplied over the model, or -1 for none.
    public CardDef tint(int v) { tint = v; return this; }
    public CardDef scalePercent(int v) { scalePercent = v; return this; }
    public CardDef description(String v) { description = v == null ? "" : v; return this; }
    /// Which of an uploaded model's animations plays which part, as
    /// `idle=Name;run=Name;attack=Name;death=Name`. Null for the bundled models,
    /// which use the KayKit names.
    public CardDef clips(String v) { clips = v; return this; }
    public String clips() { return clips; }

    public String model() { return model; }
    public int tint() { return tint; }
    public int scalePercent() { return scalePercent; }
    public String description() { return description; }

    public int hp() { return hp; }
    public int damage() { return damage; }
    public int hitTicks() { return hitTicks; }
    public int range() { return range; }
    public int sight() { return sight; }
    /// Milli-tiles per tick.
    public int speed() { return speed; }
    public int targets() { return targets; }
    public boolean isFlying() { return flying; }
    public boolean isBuildingsOnly() { return buildingsOnly; }
    public int splash() { return splash; }
    public int count() { return count; }
    public int radius() { return radius; }
    public String spawns() { return spawns; }
    public int spawnTicks() { return spawnTicks; }
    public int lifetimeTicks() { return lifetimeTicks; }
    /// The share of a spell's damage a tower takes, in percent.
    public int towerPercent() { return towerPercent; }
    public int deployTicks() { return deployTicks; }
    public int spread() { return spread; }

    public boolean isPlayable() {
        return kind != TOWER && cost > 0;
    }

    /// True when a body of this kind is a building (towers included), which
    /// buildings-only attackers go for.
    public boolean isStructure() {
        return kind == BUILDING || kind == TOWER;
    }
}
