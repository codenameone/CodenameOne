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
package com.codenameone.examples.skirmish.battle;

import com.codename1.gaming.GameCamera;
import com.codename1.gaming.GameView;
import com.codename1.gaming.ParticleEmitter;
import com.codename1.gpu.GraphicsDevice;

/// The particle effects of a battle, built on `ParticleEmitter`: fire and
/// smoke for explosions, sparks for blows, a shimmer for spawns and magic,
/// dust for footfalls. Purely cosmetic -- the rules never see any of it.
// tag::skirmish-effects[]
final class Effects {
    private final ParticleEmitter fire;
    private final ParticleEmitter smoke;
    private final ParticleEmitter sparks;
    private final ParticleEmitter magic;
    private final ParticleEmitter dust;
    private final ParticleEmitter arrows;

    Effects(GraphicsDevice device, GameView view) {
        fire = new ParticleEmitter(device, 220).setColor(0xffff8a2a).setAdditive(true)
                .setSpeed(2f, 7f).setLifetime(0.25f, 0.6f).setSize(0.9f, 0.2f)
                .setGravity(1.5f).setDrag(3f);
        smoke = new ParticleEmitter(device, 140).setColor(0xc0585450).setAdditive(false)
                .setSpeed(0.6f, 2.2f).setLifetime(0.8f, 1.6f).setSize(0.8f, 2.2f)
                .setGravity(1.2f).setDrag(1.5f);
        sparks = new ParticleEmitter(device, 160).setColor(0xfffff1b0).setAdditive(true)
                .setSpeed(2.5f, 6f).setLifetime(0.12f, 0.3f).setSize(0.22f, 0.05f)
                .setGravity(-9f).setDrag(1f);
        magic = new ParticleEmitter(device, 160).setColor(0xff7fb4ff).setAdditive(true)
                .setSpeed(0.4f, 1.6f).setLifetime(0.4f, 0.9f).setSize(0.45f, 0.1f)
                .setGravity(2.5f).setDrag(1f);
        dust = new ParticleEmitter(device, 120).setColor(0x90c2a77a).setAdditive(false)
                .setSpeed(0.6f, 1.8f).setLifetime(0.4f, 0.8f).setSize(0.35f, 0.9f)
                .setGravity(0.4f).setDrag(3f).setDirection(0f, 0.3f, 0f, 85f);
        arrows = new ParticleEmitter(device, 160).setColor(0xffe8e0c8).setAdditive(true)
                .setSpeed(14f, 18f).setLifetime(0.35f, 0.55f).setSize(0.18f, 0.12f)
                .setGravity(-12f).setDrag(0f).setDirection(0f, -1f, 0f, 12f);
        smoke.addTo(view);
        dust.addTo(view);
        fire.addTo(view);
        sparks.addTo(view);
        magic.addTo(view);
        arrows.addTo(view);
    }

    /// A fireball or a falling tower: flame, then smoke, then sparks.
    void explosion(float x, float y, float z, float size) {
        int n = (int) (14 + size * 10);
        fire.setSize(0.6f * size, 0.15f * size);
        fire.burst(x, y + 0.3f, z, n);
        smoke.burst(x, y + 0.4f, z, n / 2);
        sparks.burst(x, y + 0.4f, z, n / 2);
        dust.burst(x, 0.1f, z, n / 3);
    }

    /// A blow landing.
    void hit(float x, float y, float z) {
        sparks.burst(x, y, z, 6);
    }

    /// Something appearing on the field, tinted by team.
    void spawn(float x, float z, boolean mine) {
        magic.setColor(mine ? 0xff7fb4ff : 0xffff8f7f);
        magic.burst(x, 0.2f, z, 18);
        dust.burst(x, 0.05f, z, 8);
    }

    /// A spell cast: the cast flash.
    void cast(float x, float y, float z, int argb) {
        magic.setColor(argb);
        magic.burst(x, y, z, 10);
    }

    /// One trail puff behind a fireball.
    void trail(float x, float y, float z) {
        fire.setSize(0.5f, 0.1f);
        fire.burst(x, y, z, 2);
    }

    /// Arrows dropping from the sky onto `(x, z)`, spread over `radius`.
    void volley(float x, float z, float radius, int count, java.util.Random r) {
        for (int i = 0; i < count; i++) {
            float ox = (r.nextFloat() * 2f - 1f) * radius;
            float oz = (r.nextFloat() * 2f - 1f) * radius;
            arrows.burst(x + ox, 6f, z + oz, 1);
        }
    }

    void dust(float x, float z) {
        dust.burst(x, 0.05f, z, 3);
    }

    void update(float dt, GameCamera camera) {
        fire.update(dt, camera);
        smoke.update(dt, camera);
        sparks.update(dt, camera);
        magic.update(dt, camera);
        dust.update(dt, camera);
        arrows.update(dt, camera);
    }
}
// end::skirmish-effects[]
