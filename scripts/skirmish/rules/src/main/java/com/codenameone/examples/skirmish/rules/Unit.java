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

/// One body on the field: a troop, a building, a tower or a spell in flight.
///
/// The renderer reads these through the getters; only [Match] changes them.
public final class Unit {
    final int id;
    final CardDef card;
    final int owner;
    int x;
    int y;
    int hp;
    final int maxHp;
    int cooldown;
    int targetId = -1;
    int deployTimer;
    int age;
    int lane;
    boolean active = true;

    Unit(int id, CardDef card, int owner, int x, int y) {
        this.id = id;
        this.card = card;
        this.owner = owner;
        this.x = x;
        this.y = y;
        // A spell is a body only while it flies; it needs a pulse to be updated.
        this.hp = card.kind == CardDef.SPELL ? 1 : card.hp;
        this.maxHp = this.hp;
        this.deployTimer = card.kind == CardDef.TOWER ? 0 : card.deployTicks;
        this.lane = Arena.laneOf(x);
    }

    public int id() { return id; }
    public CardDef card() { return card; }
    public int owner() { return owner; }
    public int x() { return x; }
    public int y() { return y; }
    public int hp() { return hp; }
    public int maxHp() { return maxHp; }
    /// The id of the unit this one is attacking or walking to, or -1.
    public int targetId() { return targetId; }
    /// Ticks until this unit takes the field (for a spell: until it lands).
    public int deployTimer() { return deployTimer; }
    public boolean isAlive() { return hp > 0; }
    public boolean isDeploying() { return deployTimer > 0; }
    /// A king tower stays asleep until it is hit or loses a princess tower.
    public boolean isActive() { return active; }

    boolean isFlying() {
        return card.flying;
    }

    boolean isStructure() {
        return card.isStructure();
    }

    /// True when `attacker`'s card may hit this unit at all.
    boolean hittableBy(CardDef attacker) {
        if (card.kind == CardDef.SPELL || hp <= 0) {
            return false;
        }
        if (attacker.buildingsOnly) {
            return isStructure();
        }
        if (isFlying()) {
            return (attacker.targets & TargetKind.AIR) != 0;
        }
        return (attacker.targets & TargetKind.GROUND) != 0 || (attacker.targets & TargetKind.BUILDINGS) != 0;
    }
}
