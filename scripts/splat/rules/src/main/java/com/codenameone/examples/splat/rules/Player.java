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

/// The state of one player. Only [Match] writes it; everything else reads.
public final class Player {
    public final int index;
    public final Slot slot;
    public final MarkerDef marker;

    int x;
    int y;
    int z;
    int vy;
    int yaw;
    int pitch;
    boolean grounded;
    boolean crouched;
    boolean alive;
    int respawnAt;
    int wipeUntil;
    boolean wiped;
    int paintedBy = -1;
    int protectedUntil;
    int cooldownUntil;
    int ammo;
    int reloadUntil;
    boolean grenade;
    /// Team whose flag this player carries, or -1.
    int carrying = -1;
    int paints;
    int outs;
    int captures;
    int lastButtons;
    Input input = Input.IDLE;

    Player(int index, Slot slot) {
        this.index = index;
        this.slot = slot;
        this.marker = Catalog.marker(slot.marker);
    }

    public int team() {
        return slot.team;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    public int yaw() {
        return yaw;
    }

    public int pitch() {
        return pitch;
    }

    public boolean alive() {
        return alive;
    }

    public boolean crouched() {
        return crouched;
    }

    public boolean grounded() {
        return grounded;
    }

    public int height() {
        return crouched ? Match.CROUCH_HEIGHT : Match.HEIGHT;
    }

    public int eyeY() {
        return y + height() - 150;
    }

    public int ammo() {
        return ammo;
    }

    public boolean reloading(int tick) {
        return tick < reloadUntil;
    }

    public boolean hasGrenade() {
        return grenade;
    }

    public int carrying() {
        return carrying;
    }

    public int respawnAt() {
        return respawnAt;
    }

    /// Who painted this player last, or -1.
    public int paintedBy() {
        return paintedBy;
    }

    public boolean canWipe(int tick) {
        return !alive && !wiped && tick <= wipeUntil;
    }

    public boolean shielded(int tick) {
        return tick < protectedUntil;
    }

    public int paints() {
        return paints;
    }

    public int outs() {
        return outs;
    }

    public int captures() {
        return captures;
    }

    public Input input() {
        return input;
    }

    void write(IntBuf b) {
        b.add(x).add(y).add(z).add(vy).add(yaw).add(pitch).add(grounded).add(crouched).add(alive)
                .add(respawnAt).add(wipeUntil).add(wiped).add(paintedBy).add(protectedUntil)
                .add(cooldownUntil).add(ammo).add(reloadUntil).add(grenade).add(carrying).add(paints)
                .add(outs).add(captures).add(lastButtons);
        b.add(input.strafe).add(input.forward).add(input.yaw).add(input.pitch).add(input.buttons)
                .add(input.viewTick);
    }

    void read(IntBuf.Reader r) {
        x = r.next();
        y = r.next();
        z = r.next();
        vy = r.next();
        yaw = r.next();
        pitch = r.next();
        grounded = r.bool();
        crouched = r.bool();
        alive = r.bool();
        respawnAt = r.next();
        wipeUntil = r.next();
        wiped = r.bool();
        paintedBy = r.next();
        protectedUntil = r.next();
        cooldownUntil = r.next();
        ammo = r.next();
        reloadUntil = r.next();
        grenade = r.bool();
        carrying = r.next();
        paints = r.next();
        outs = r.next();
        captures = r.next();
        lastButtons = r.next();
        input = new Input(r.next(), r.next(), r.next(), r.next(), r.next(), r.next());
    }
}
