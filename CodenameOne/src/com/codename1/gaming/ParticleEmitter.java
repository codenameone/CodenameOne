/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.gaming;

import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.IndexBuffer;
import com.codename1.gpu.Material;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.PrimitiveType;
import com.codename1.gpu.RenderState;
import com.codename1.gpu.Texture;
import com.codename1.gpu.VertexBuffer;
import com.codename1.gpu.VertexFormat;

import java.util.Random;

/// Camera facing particles for 3D effects: explosions, sparks, dust, magic.
/// Every particle of an emitter is drawn in one call as a soft round billboard
/// that brightens in, fades out and changes size over its life.
///
/// An emitter is a pool of at most `maxParticles` live particles. `burst`
/// spawns a batch at a point; `update` moves them and turns them toward the
/// camera. Add it to a view with `addTo(GameView)` and keep it for the life of
/// the scene -- an emitter with no live particles draws nothing visible.
///
/// ```java
/// ParticleEmitter fire = new ParticleEmitter(device, 64)
///         .setColor(0xffff8a2a).setAdditive(true)
///         .setSpeed(1.5f, 4f).setLifetime(0.3f, 0.7f).setSize(0.6f, 1.6f)
///         .setGravity(-2f);
/// fire.addTo(gameView);
/// fire.burst(x, 0.5f, z, 40);
/// // each frame:
/// fire.update(deltaSeconds, gameView.getCamera());
/// ```
///
/// Additive emitters suit light (fire, sparks, magic); alpha blended ones suit
/// matter (smoke, dust, debris).
public class ParticleEmitter {
    private static final int CELLS = 8;
    private static final int CELL = 32;

    private final int max;
    private final float[] px;
    private final float[] py;
    private final float[] pz;
    private final float[] vx;
    private final float[] vy;
    private final float[] vz;
    private final float[] life;
    private final float[] span;
    private final int[] live = new int[1];
    private final Model model;
    private final Material material;
    private final RenderState state;
    private final Random random = new Random();

    private float minSpeed = 1f;
    private float maxSpeed = 3f;
    private float minLife = 0.4f;
    private float maxLife = 0.9f;
    private float startSize = 0.3f;
    private float endSize = 0.1f;
    private float gravity = -9.8f;
    private float drag = 1.5f;
    private float dirX;
    private float dirY = 1f;
    private float dirZ;
    private float cone = 180f;

    /// Creates an emitter able to hold `maxParticles` live particles.
    ///
    /// #### Parameters
    ///
    /// - `device`: the device that will draw it; uploads the particle texture
    ///
    /// - `maxParticles`: the pool size; a burst beyond it reuses the oldest
    public ParticleEmitter(GraphicsDevice device, int maxParticles) {
        max = Math.max(1, maxParticles);
        px = new float[max];
        py = new float[max];
        pz = new float[max];
        vx = new float[max];
        vy = new float[max];
        vz = new float[max];
        life = new float[max];
        span = new float[max];
        VertexBuffer vb = new VertexBuffer(VertexFormat.POSITION_NORMAL_TEXCOORD, max * 4);
        IndexBuffer ib = new IndexBuffer(max * 6);
        int[] idx = new int[max * 6];
        for (int i = 0; i < max; i++) {
            int v = i * 4;
            int o = i * 6;
            idx[o] = v;
            idx[o + 1] = v + 1;
            idx[o + 2] = v + 2;
            idx[o + 3] = v;
            idx[o + 4] = v + 2;
            idx[o + 5] = v + 3;
        }
        ib.setData(idx);
        state = new RenderState().setDepthTest(true).setDepthWrite(false)
                .setCullMode(RenderState.CullMode.NONE).setBlendMode(RenderState.BlendMode.ADDITIVE);
        Texture t = device.createTexture(CELLS * CELL, CELL, dotStrip());
        t.setFilter(Texture.Filter.LINEAR);
        material = new Material(Material.Type.UNLIT).setTexture(t).setColor(0xffffffff).setRenderState(state);
        model = new Model(new Mesh(vb, ib, PrimitiveType.TRIANGLES), material);
    }

    /// A strip of soft dots, brightest on the left, so a particle fades by
    /// sliding its texture coordinates rather than needing a color per vertex.
    private static int[] dotStrip() {
        int w = CELLS * CELL;
        int[] argb = new int[w * CELL];
        float r = CELL / 2f;
        for (int cell = 0; cell < CELLS; cell++) {
            float intensity = 1f - cell / (float) CELLS;
            for (int y = 0; y < CELL; y++) {
                for (int x = 0; x < CELL; x++) {
                    float dx = (x + 0.5f - r) / r;
                    float dy = (y + 0.5f - r) / r;
                    float d = 1f - (float) Math.sqrt(dx * dx + dy * dy);
                    float a = d <= 0f ? 0f : d * d * intensity;
                    int c = (int) (a * 255f + 0.5f);
                    argb[y * w + cell * CELL + x] = (c << 24) | (c << 16) | (c << 8) | c;
                }
            }
        }
        return argb;
    }

    /// The model that draws the particles.
    public Model getModel() {
        return model;
    }

    /// Adds the emitter to `view`.
    public void addTo(GameView view) {
        view.addModel(model);
    }

    /// Removes the emitter from `view`.
    public void removeFrom(GameView view) {
        view.removeModel(model);
    }

    /// Sets the particle color. The alpha channel scales the whole effect.
    public ParticleEmitter setColor(int argb) {
        material.setColor(argb);
        return this;
    }

    /// True to add light (fire, sparks), false to alpha blend (smoke, dust).
    public ParticleEmitter setAdditive(boolean additive) {
        state.setBlendMode(additive ? RenderState.BlendMode.ADDITIVE : RenderState.BlendMode.ALPHA);
        return this;
    }

    /// The initial speed range in world units per second.
    public ParticleEmitter setSpeed(float min, float max) {
        minSpeed = min;
        maxSpeed = max;
        return this;
    }

    /// The lifetime range in seconds.
    public ParticleEmitter setLifetime(float min, float max) {
        minLife = min;
        maxLife = Math.max(min, max);
        return this;
    }

    /// The billboard size at birth and at death, in world units.
    public ParticleEmitter setSize(float start, float end) {
        startSize = start;
        endSize = end;
        return this;
    }

    /// Acceleration along y in units per second squared; negative falls,
    /// positive rises (smoke, embers).
    public ParticleEmitter setGravity(float gravity) {
        this.gravity = gravity;
        return this;
    }

    /// The fraction of velocity lost per second.
    public ParticleEmitter setDrag(float drag) {
        this.drag = drag;
        return this;
    }

    /// Emits within `coneDegrees` of the direction `(x, y, z)`; 180 is every
    /// direction, 0 a straight jet.
    public ParticleEmitter setDirection(float x, float y, float z, float coneDegrees) {
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        if (len > 0f) {
            dirX = x / len;
            dirY = y / len;
            dirZ = z / len;
        }
        cone = coneDegrees;
        return this;
    }

    /// The number of live particles.
    public int getLiveCount() {
        return live[0];
    }

    /// True while any particle is alive.
    public boolean isActive() {
        return live[0] > 0;
    }

    /// Removes every live particle.
    public void clear() {
        live[0] = 0;
    }

    /// Spawns `count` particles at `(x, y, z)`. When the pool is full the
    /// oldest particles are replaced.
    public void burst(float x, float y, float z, int count) {
        double coneCos = Math.cos(Math.toRadians(Math.min(180f, Math.max(0f, cone))));
        for (int n = 0; n < count; n++) {
            int i;
            if (live[0] < max) {
                i = live[0]++;
            } else {
                i = oldest();
            }
            // a direction uniformly distributed over the cone around dir
            float cz = (float) (coneCos + random.nextDouble() * (1.0 - coneCos));
            float sz = (float) Math.sqrt(Math.max(0f, 1f - cz * cz));
            float phi = (float) (random.nextDouble() * Math.PI * 2.0);
            float lx = sz * (float) Math.cos(phi);
            float ly = sz * (float) Math.sin(phi);
            // basis around dir
            float ax = Math.abs(dirX) < 0.9f ? 1f : 0f;
            float ay = ax == 1f ? 0f : 1f;
            float ux = -dirZ * ay;
            float uy = dirZ * ax;
            float uz = dirX * ay - dirY * ax;
            float ul = (float) Math.sqrt(ux * ux + uy * uy + uz * uz);
            ux /= ul;
            uy /= ul;
            uz /= ul;
            float wx = dirY * uz - dirZ * uy;
            float wy = dirZ * ux - dirX * uz;
            float wz = dirX * uy - dirY * ux;
            float speed = minSpeed + random.nextFloat() * (maxSpeed - minSpeed);
            vx[i] = (dirX * cz + ux * lx + wx * ly) * speed;
            vy[i] = (dirY * cz + uy * lx + wy * ly) * speed;
            vz[i] = (dirZ * cz + uz * lx + wz * ly) * speed;
            px[i] = x;
            py[i] = y;
            pz[i] = z;
            span[i] = minLife + random.nextFloat() * (maxLife - minLife);
            life[i] = 0f;
        }
    }

    private int oldest() {
        int best = 0;
        float most = -1f;
        for (int i = 0; i < max; i++) {
            float f = span[i] <= 0f ? 1f : life[i] / span[i];
            if (f > most) {
                most = f;
                best = i;
            }
        }
        return best;
    }

    /// Moves the particles by `deltaSeconds`, retires the expired ones and
    /// turns the rest toward `camera`.
    public void update(float deltaSeconds, GameCamera camera) {
        int n = live[0];
        float damp = Math.max(0f, 1f - drag * deltaSeconds);
        for (int i = 0; i < n; i++) {
            life[i] += deltaSeconds;
            if (life[i] >= span[i]) {
                n--;
                copy(n, i);
                i--;
                continue;
            }
            vy[i] += gravity * deltaSeconds;
            vx[i] *= damp;
            vy[i] *= damp;
            vz[i] *= damp;
            px[i] += vx[i] * deltaSeconds;
            py[i] += vy[i] * deltaSeconds;
            pz[i] += vz[i] * deltaSeconds;
        }
        live[0] = n;
        build(camera);
    }

    private void copy(int from, int to) {
        px[to] = px[from];
        py[to] = py[from];
        pz[to] = pz[from];
        vx[to] = vx[from];
        vy[to] = vy[from];
        vz[to] = vz[from];
        life[to] = life[from];
        span[to] = span[from];
    }

    private void build(GameCamera camera) {
        // camera right and up from the view direction, world up = +y
        float fx = camera.getTargetX() - camera.getEyeX();
        float fy = camera.getTargetY() - camera.getEyeY();
        float fz = camera.getTargetZ() - camera.getEyeZ();
        float rx = -fz;
        float rz = fx;
        float rl = (float) Math.sqrt(rx * rx + rz * rz);
        if (rl == 0f) {
            rx = 1f;
            rz = 0f;
        } else {
            rx /= rl;
            rz /= rl;
        }
        // up = right x forward
        float ux = -rz * fy;
        float uy = rz * fx - rx * fz;
        float uz = rx * fy;
        float ul = (float) Math.sqrt(ux * ux + uy * uy + uz * uz);
        if (ul > 0f) {
            ux /= ul;
            uy /= ul;
            uz /= ul;
        }
        float nx = -fx;
        float ny = -fy;
        float nz = -fz;
        VertexBuffer vb = model.getMesh().getVertices();
        float[] d = vb.getData();
        int n = live[0];
        float cellW = 1f / CELLS;
        for (int i = 0; i < max; i++) {
            int o = i * 32;
            if (i >= n) {
                for (int k = 0; k < 32; k++) {
                    d[o + k] = 0f;
                }
                continue;
            }
            float f = span[i] <= 0f ? 1f : life[i] / span[i];
            // quick swell in the first 15% of life, then shrink toward endSize
            float size = f < 0.15f ? startSize * (0.4f + 4f * f) : startSize + (endSize - startSize) * (f - 0.15f) / 0.85f;
            float h = size * 0.5f;
            int cell = Math.min(CELLS - 1, (int) (f * CELLS));
            float u0 = cell * cellW + 0.5f / (CELLS * CELL);
            float u1 = (cell + 1) * cellW - 0.5f / (CELLS * CELL);
            corner(d, o, px[i] - (rx + ux) * h, py[i] - uy * h, pz[i] - (rz + uz) * h, nx, ny, nz, u0, 1f);
            corner(d, o + 8, px[i] + (rx - ux) * h, py[i] - uy * h, pz[i] + (rz - uz) * h, nx, ny, nz, u1, 1f);
            corner(d, o + 16, px[i] + (rx + ux) * h, py[i] + uy * h, pz[i] + (rz + uz) * h, nx, ny, nz, u1, 0f);
            corner(d, o + 24, px[i] - (rx - ux) * h, py[i] + uy * h, pz[i] - (rz - uz) * h, nx, ny, nz, u0, 0f);
        }
        vb.setDirty();
    }

    private static void corner(float[] d, int o, float x, float y, float z, float nx, float ny, float nz,
                               float u, float v) {
        d[o] = x;
        d[o + 1] = y;
        d[o + 2] = z;
        d[o + 3] = nx;
        d[o + 4] = ny;
        d[o + 5] = nz;
        d[o + 6] = u;
        d[o + 7] = v;
    }
}
