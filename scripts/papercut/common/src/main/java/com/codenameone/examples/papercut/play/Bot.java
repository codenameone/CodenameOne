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
package com.codenameone.examples.papercut.play;

/// A player that is neither good nor hopeless: it flees what is close, aims at
/// what is nearest and drifts toward ink drops when nothing is in its way.
///
/// It flies the menu's background run and the tests. `skill`, from 0 to 1,
/// is how far ahead it looks and how sharply it reacts.
public final class Bot {
    private final float skill;
    private float wanderX = 1;
    private float wanderY;
    private float wanderT;
    private float react;

    public Bot(float skill) {
        this.skill = skill;
    }

    /// Sets the arena's controls for the next step.
    public void think(Arena a, float dt) {
        // A person does not re-read the page sixty times a second. Between
        // glances the bot holds what it decided, which is most of what makes it
        // lose: a snipper that starts its dash mid-glance is seen too late.
        react -= dt;
        a.bombPressed = false;
        if (react > 0) {
            return;
        }
        react = 0.14f + 0.36f * (1 - skill);
        float fleeX = 0;
        float fleeY = 0;
        float bestD = Float.MAX_VALUE;
        Enemy target = null;
        // Attention wears out over a long run, as a person's does.
        float look = (140 + 160 * skill) * Math.max(0.45f, 1 - a.time / 900);
        for (Enemy e : a.enemies) {
            if (e.dead) {
                continue;
            }
            float dx = a.shipX - e.x;
            float dy = a.shipY - e.y;
            float d2 = dx * dx + dy * dy;
            float d = (float) Math.sqrt(d2) + 0.01f;
            if (e.kind == Enemy.GATE) {
                // Fly through the middle of a gate, square on to the strip.
                if (a.mode == Arena.FOLD && d < bestD) {
                    bestD = d;
                    target = e;
                }
                continue;
            }
            float reach = look + e.r + (e.kind == Enemy.SNIPPER ? 140 : 0);
            if (d < reach) {
                float w = (reach - d) / reach;
                fleeX += dx / d * w * w * 3;
                fleeY += dy / d * w * w * 3;
            }
            if (a.mode != Arena.FOLD && e.live() && d < bestD) {
                bestD = d;
                target = e;
            }
        }
        // The walls are as dangerous as anything: being cornered is death.
        float m = 160;
        fleeX += edge(a.shipX, m) - edge(Arena.WIDTH - a.shipX, m);
        fleeY += edge(a.shipY, m) - edge(Arena.HEIGHT - a.shipY, m);

        float mx = fleeX;
        float my = fleeY;
        if (mx * mx + my * my < 0.05f) {
            Drop near = null;
            float nd = 400 * 400;
            for (Drop p : a.drops) {
                float dx = p.x - a.shipX;
                float dy = p.y - a.shipY;
                float d2 = dx * dx + dy * dy;
                if (d2 < nd) {
                    nd = d2;
                    near = p;
                }
            }
            if (a.mode == Arena.FOLD && target != null) {
                mx = target.x - a.shipX;
                my = target.y - a.shipY;
            } else if (near != null) {
                mx = near.x - a.shipX;
                my = near.y - a.shipY;
            } else {
                wanderT -= dt;
                if (wanderT <= 0) {
                    wanderT = 1.5f;
                    float t = a.rnd() * 6.2832f;
                    wanderX = (float) Math.cos(t);
                    wanderY = (float) Math.sin(t);
                }
                mx = wanderX + (Arena.WIDTH / 2 - a.shipX) / 900;
                my = wanderY + (Arena.HEIGHT / 2 - a.shipY) / 600;
            }
        }
        float len = (float) Math.sqrt(mx * mx + my * my) + 0.0001f;
        a.moveX = mx / len;
        a.moveY = my / len;
        if (target != null && a.mode != Arena.FOLD) {
            // Lead the target by how long the stroke takes to get there.
            float t = bestD / Arena.SHOT_SPEED * skill;
            a.aimX = target.x + target.vx * t - a.shipX;
            a.aimY = target.y + target.vy * t - a.shipY;
            a.firing = true;
        } else {
            a.aimX = a.moveX;
            a.aimY = a.moveY;
            a.firing = a.mode != Arena.FOLD;
        }
        // Erase the page when boxed in: three things within reach.
        int close = 0;
        for (Enemy e : a.enemies) {
            if (e.live() && e.kind != Enemy.GATE) {
                float dx = a.shipX - e.x;
                float dy = a.shipY - e.y;
                if (dx * dx + dy * dy < 90 * 90) {
                    close++;
                }
            }
        }
        a.bombPressed = close >= 3 && skill > 0.4f;
    }

    private static float edge(float dist, float margin) {
        return dist < margin ? (margin - dist) / margin * 2 : 0;
    }
}
