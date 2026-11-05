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

/// What one player asks for during one tick. Immutable; the server keeps the last
/// one it received and repeats it when a frame arrives late, which is also what the
/// client predicts with.
public final class Input {
    public static final int FIRE = 1;
    public static final int JUMP = 2;
    public static final int CROUCH = 4;
    public static final int SPRINT = 8;
    public static final int RELOAD = 16;
    /// Throw the paint grenade (one per life).
    public static final int THROW = 32;
    /// Wipe the paint off and pretend nothing happened. See [Match#WIPE_WINDOW].
    public static final int WIPE = 64;

    public static final Input IDLE = new Input(0, 0, 0, 0, 0, -1);

    /// Strafe, -127 (left) to 127 (right).
    public final int strafe;
    /// Forward, -127 (back) to 127.
    public final int forward;
    /// Absolute look direction; binary angle, [Trig#FULL] per turn.
    public final int yaw;
    /// Absolute look pitch, clamped to [Match#PITCH_LIMIT].
    public final int pitch;
    public final int buttons;
    /// The tick the player was looking at when this was sent, or -1. The server
    /// moves a new shot forward by the difference (capped at [Match#MAX_LAG_TICKS])
    /// so a ball leaves where the shooter saw the world, not 100 ms later.
    public final int viewTick;

    public Input(int strafe, int forward, int yaw, int pitch, int buttons, int viewTick) {
        this.strafe = FixedMath.clamp(strafe, -127, 127);
        this.forward = FixedMath.clamp(forward, -127, 127);
        this.yaw = Trig.wrap(yaw);
        this.pitch = FixedMath.clamp(pitch, -Match.PITCH_LIMIT, Match.PITCH_LIMIT);
        this.buttons = buttons;
        this.viewTick = viewTick;
    }

    public boolean has(int button) {
        return (buttons & button) != 0;
    }

    /// One line, the form replays and the wire share: `strafe forward yaw pitch buttons viewTick`.
    public String encode() {
        return strafe + " " + forward + " " + yaw + " " + pitch + " " + buttons + " " + viewTick;
    }

    public static Input decode(String s) {
        int[] v = Text.ints(s, 6);
        return new Input(v[0], v[1], v[2], v[3], v[4], v[5]);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Input)) {
            return false;
        }
        Input i = (Input) o;
        return i.strafe == strafe && i.forward == forward && i.yaw == yaw && i.pitch == pitch
                && i.buttons == buttons && i.viewTick == viewTick;
    }

    @Override
    public int hashCode() {
        return ((strafe * 31 + forward) * 31 + yaw) * 31 + pitch * 7 + buttons;
    }
}
