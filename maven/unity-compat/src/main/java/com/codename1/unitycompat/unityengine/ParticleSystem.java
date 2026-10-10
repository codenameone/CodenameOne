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
package com.codename1.unitycompat.unityengine;

import com.codename1.unitycompat.system.Struct;

/// `UnityEngine.ParticleSystem`: many small sprites, each born from a
/// shape, moved and faded over a short life.
///
/// The particles are simulated here, on the processor, in the plane the
/// 2D camera looks at: a particle that Unity would send towards or away
/// from the camera stays where it is on the screen, as it would look
/// there. Each system has its own sequence of random numbers, seeded from
/// the scene, so that a system emitting never changes what a script's
/// `Random` answers, and a run is the same every time and on every
/// target.
///
/// What is read from a scene: the main module's duration, looping, play
/// on awake, start delay, lifetime, speed, size, rotation and colour,
/// gravity, the simulation space and speed and the most particles;
/// emission by time, by distance and in bursts; the shapes sphere,
/// hemisphere, cone, box, circle, edge and rectangle; velocity, colour,
/// size and rotation over lifetime. The other modules -- force, noise,
/// collision, triggers, sub emitters, texture sheet animation, lights,
/// trails -- are not implemented, and the scene compiler says so for each
/// one a scene turns on.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class ParticleSystem extends Component {
    /// The curves a scene sets, by the number the scene compiler gives.
    public static final int START_DELAY = 0;
    public static final int START_LIFETIME = 1;
    public static final int START_SPEED = 2;
    public static final int START_SIZE = 3;
    public static final int START_ROTATION = 4;
    public static final int GRAVITY = 5;
    public static final int RATE_OVER_TIME = 6;
    public static final int RATE_OVER_DISTANCE = 7;
    public static final int VELOCITY_X = 8;
    public static final int VELOCITY_Y = 9;
    public static final int VELOCITY_Z = 10;
    public static final int SIZE_OVER_LIFETIME = 11;
    public static final int ROTATION_OVER_LIFETIME = 12;
    private static final int CURVES = 13;

    /// The modules that can be turned on, one bit each.
    public static final int EMISSION = 1;
    public static final int SHAPE = 2;
    public static final int VELOCITY_OVER_LIFETIME = 4;
    public static final int COLOR_OVER_LIFETIME = 8;
    public static final int SIZE_OVER_LIFETIME_MODULE = 16;
    public static final int ROTATION_OVER_LIFETIME_MODULE = 32;

    private static final float[] FLAT = {0f, 1f, 0f, 0f, 1f, 1f, 0f, 0f};
    private static int made;

    // ---------------------------------------------------------------- set up
    private float duration = 5f;
    private boolean looping = true;
    private boolean playOnAwake = true;
    /// 0 local, 1 world.
    private int space;
    private float simulationSpeed = 1f;
    private int maxParticles = 1000;
    /// 0 hierarchy, 1 local, 2 shape.
    private int scalingMode = 1;
    private int modules = EMISSION | SHAPE;
    private boolean velocityInWorld;

    private final int[] curveMode = new int[CURVES];
    private final float[] curveMax = new float[CURVES];
    private final float[] curveMin = new float[CURVES];
    private final float[][] curveKeys = new float[CURVES][];
    private final float[][] curveLowKeys = new float[CURVES][];

    /// A colour is a mode -- 0 one colour, 1 a gradient, 2 between two
    /// colours, 3 between two gradients -- two colours of four floats, and
    /// two gradients.
    private int startColorMode;
    private final float[] startColors = {1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f};
    private float[] startGradient;
    private float[] startLowGradient;
    private int lifeColorMode;
    private final float[] lifeColors = {1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f};
    private float[] lifeGradient;
    private float[] lifeLowGradient;

    private int shapeType = 4;
    private float shapeRadius = 1f;
    private float shapeThickness = 1f;
    private float shapeAngle = 25f;
    private float shapeArc = 360f;
    private float shapeScaleX = 1f;
    private float shapeScaleY = 1f;
    private float shapeScaleZ = 1f;
    private float shapeX;
    private float shapeY;
    private float shapeZ;
    /// The turn of the shape and of the object together, as the rows of a
    /// matrix that give x and y on the screen from a direction in the
    /// shape's own axes.
    private float m00 = 1f;
    private float m01;
    private float m02;
    private float m10;
    private float m11 = 1f;
    private float m12;

    /// Bursts, six floats each: when, the fewest and the most particles,
    /// how many times, how long between them, and how likely.
    private float[] bursts = new float[0];
    private int seed0;
    private boolean seeded;

    // ------------------------------------------------------------- particles
    private float[] px = new float[0];
    private float[] py = new float[0];
    private float[] vx;
    private float[] vy;
    private float[] age;
    private float[] life;
    private float[] size;
    private float[] turn;
    private float[] chance;
    private int[] tint;
    private int count;

    private int seed;
    private boolean playing;
    private boolean emitting;
    private boolean paused;
    private boolean begun;
    private float clock;
    private float delay;
    private float owed;
    private float lastX;
    private float lastY;
    private boolean placed;
    private final float[] rgba = new float[4];
    private final float[] spot = new float[4];

    public ParticleSystem() {
        for (int i = 0; i < CURVES; i++) {
            curveKeys[i] = FLAT;
            curveLowKeys[i] = FLAT;
        }
        curveMax[START_LIFETIME] = 5f;
        curveMax[START_SPEED] = 5f;
        curveMax[START_SIZE] = 1f;
        curveMax[RATE_OVER_TIME] = 10f;
        curveMax[SIZE_OVER_LIFETIME] = 1f;
        seed0 = nextSeed();
    }

    /// The seed of the next system made: a different one for each, and the
    /// same ones in the same order every run.
    private static int nextSeed() {
        made++;
        return 0x2545f491 + made * 0x9e3779b9;
    }

    static void reset() {
        made = 0;
    }

    // -------------------------------------------------------- generated code

    /// What a scene file sets of the main module. A `randomSeed` of zero
    /// leaves the seed the system was made with.
    public void $main(float lasts, boolean loops, boolean onAwake, int simulationSpace, float speed, int most,
            int scaling, int randomSeed) {
        duration = lasts;
        looping = loops;
        playOnAwake = onAwake;
        space = simulationSpace;
        simulationSpeed = speed;
        maxParticles = most;
        scalingMode = scaling;
        if (randomSeed != 0) {
            seed0 = randomSeed;
            seeded = true;
        }
    }

    /// Which modules are on, and whether velocity over lifetime is in the
    /// world's axes.
    public void $modules(int on, boolean velocityWorld) {
        modules = on;
        velocityInWorld = velocityWorld;
    }

    /// One curve: `mode` is 0 a constant, 1 a curve, 2 between two curves,
    /// 3 between two constants; the keys are four floats each -- time,
    /// value, slope in, slope out -- and are multiplied by the constant.
    public void $curve(int which, int mode, float max, float min, float[] keys, float[] lowKeys) {
        curveMode[which] = mode;
        curveMax[which] = max;
        curveMin[which] = min;
        curveKeys[which] = keys == null || keys.length < 4 ? FLAT : keys;
        curveLowKeys[which] = lowKeys == null || lowKeys.length < 4 ? FLAT : lowKeys;
    }

    /// The start colour, or with `overLife` the colour over lifetime. A
    /// gradient is its colour keys then its alpha keys: the count of the
    /// first, then for each a time and red, green and blue, then for each
    /// alpha key a time and an alpha; a negative count is a gradient
    /// whose keys do not blend.
    public void $color(boolean overLife, int mode, float[] colors, float[] gradient, float[] lowGradient) {
        if (overLife) {
            lifeColorMode = mode;
            System.arraycopy(colors, 0, lifeColors, 0, 8);
            lifeGradient = gradient;
            lifeLowGradient = lowGradient;
        } else {
            startColorMode = mode;
            System.arraycopy(colors, 0, startColors, 0, 8);
            startGradient = gradient;
            startLowGradient = lowGradient;
        }
    }

    /// The shape, by the number Unity's `ParticleSystemShapeType` has, and
    /// `matrix`: the first two rows of the turn of the shape and the
    /// object together.
    public void $shape(int type, float radius, float thickness, float angle, float arc, float scaleX,
            float scaleY, float scaleZ, float x, float y, float z, float[] matrix) {
        shapeType = type;
        shapeRadius = radius;
        shapeThickness = thickness;
        shapeAngle = angle;
        shapeArc = arc;
        shapeScaleX = scaleX;
        shapeScaleY = scaleY;
        shapeScaleZ = scaleZ;
        shapeX = x;
        shapeY = y;
        shapeZ = z;
        m00 = matrix[0];
        m01 = matrix[1];
        m02 = matrix[2];
        m10 = matrix[3];
        m11 = matrix[4];
        m12 = matrix[5];
    }

    public void $bursts(float[] six) {
        bursts = six;
    }

    @Override
    public Component $new() {
        return new ParticleSystem();
    }

    @Override
    public void $copyFrom(Component source) {
        ParticleSystem p = (ParticleSystem) source;
        duration = p.duration;
        looping = p.looping;
        playOnAwake = p.playOnAwake;
        space = p.space;
        simulationSpeed = p.simulationSpeed;
        maxParticles = p.maxParticles;
        scalingMode = p.scalingMode;
        modules = p.modules;
        velocityInWorld = p.velocityInWorld;
        for (int i = 0; i < CURVES; i++) {
            curveMode[i] = p.curveMode[i];
            curveMax[i] = p.curveMax[i];
            curveMin[i] = p.curveMin[i];
            curveKeys[i] = p.curveKeys[i];
            curveLowKeys[i] = p.curveLowKeys[i];
        }
        $color(false, p.startColorMode, p.startColors, p.startGradient, p.startLowGradient);
        $color(true, p.lifeColorMode, p.lifeColors, p.lifeGradient, p.lifeLowGradient);
        shapeType = p.shapeType;
        shapeRadius = p.shapeRadius;
        shapeThickness = p.shapeThickness;
        shapeAngle = p.shapeAngle;
        shapeArc = p.shapeArc;
        shapeScaleX = p.shapeScaleX;
        shapeScaleY = p.shapeScaleY;
        shapeScaleZ = p.shapeScaleZ;
        shapeX = p.shapeX;
        shapeY = p.shapeY;
        shapeZ = p.shapeZ;
        m00 = p.m00;
        m01 = p.m01;
        m02 = p.m02;
        m10 = p.m10;
        m11 = p.m11;
        m12 = p.m12;
        bursts = p.bursts;
        // A copy of a system whose seed the scene gave has that seed too,
        // as Unity's has; any other keeps the one it was made with.
        seeded = p.seeded;
        if (seeded) {
            seed0 = p.seed0;
        }
    }

    @Override
    public int $roles() {
        return TICKS;
    }

    // ---------------------------------------------------------------- random

    /// The next of this system's own random numbers, from 0 up to but not
    /// 1: xorshift, which is the same arithmetic on every target.
    private float next() {
        int x = seed;
        x ^= x << 13;
        x ^= x >>> 17;
        x ^= x << 5;
        seed = x;
        return (x >>> 8) * (1f / 16777216f);
    }

    /// A value for a new particle, or for the system at a time.
    private float pick(int which, float at) {
        switch (curveMode[which]) {
            case 1:
                return curveMax[which] * AnimationClip.sample(curveKeys[which], at);
            case 2: {
                float high = curveMax[which] * AnimationClip.sample(curveKeys[which], at);
                float low = curveMax[which] * AnimationClip.sample(curveLowKeys[which], at);
                float span = (high - low) * next();
                return low + span;
            }
            case 3: {
                float span = (curveMax[which] - curveMin[which]) * next();
                return curveMin[which] + span;
            }
            default:
                return curveMax[which];
        }
    }

    /// A value over a particle's life, where the random part is the
    /// number the particle drew when it was born.
    private float over(int which, float at, float drawn) {
        switch (curveMode[which]) {
            case 1:
                return curveMax[which] * AnimationClip.sample(curveKeys[which], at);
            case 2: {
                float high = curveMax[which] * AnimationClip.sample(curveKeys[which], at);
                float low = curveMax[which] * AnimationClip.sample(curveLowKeys[which], at);
                float span = (high - low) * drawn;
                return low + span;
            }
            case 3: {
                float span = (curveMax[which] - curveMin[which]) * drawn;
                return curveMin[which] + span;
            }
            default:
                return curveMax[which];
        }
    }

    /// A gradient at a time, into `rgba`.
    private void gradient(float[] g, float at) {
        rgba[0] = 1f;
        rgba[1] = 1f;
        rgba[2] = 1f;
        rgba[3] = 1f;
        if (g == null || g.length == 0) {
            return;
        }
        int colours = (int) g[0];
        boolean steps = colours < 0;
        if (steps) {
            colours = -colours;
        }
        int alphaFrom = 1 + colours * 4;
        int alphas = (g.length - alphaFrom) / 2;
        if (colours > 0) {
            int i = 0;
            while (i < colours - 1 && g[1 + (i + 1) * 4] <= at) {
                i++;
            }
            int a = 1 + i * 4;
            if (i == colours - 1 || at <= g[a] || steps) {
                int use = steps && i < colours - 1 && at > g[a] ? a + 4 : a;
                rgba[0] = g[use + 1];
                rgba[1] = g[use + 2];
                rgba[2] = g[use + 3];
            } else {
                float span = g[a + 4] - g[a];
                float k = span > 0f ? (at - g[a]) / span : 0f;
                for (int c = 1; c <= 3; c++) {
                    float d = (g[a + 4 + c] - g[a + c]) * k;
                    rgba[c - 1] = g[a + c] + d;
                }
            }
        }
        if (alphas > 0) {
            int i = 0;
            while (i < alphas - 1 && g[alphaFrom + (i + 1) * 2] <= at) {
                i++;
            }
            int a = alphaFrom + i * 2;
            if (i == alphas - 1 || at <= g[a] || steps) {
                rgba[3] = g[steps && i < alphas - 1 && at > g[a] ? a + 3 : a + 1];
            } else {
                float span = g[a + 2] - g[a];
                float k = span > 0f ? (at - g[a]) / span : 0f;
                float d = (g[a + 3] - g[a + 1]) * k;
                rgba[3] = g[a + 1] + d;
            }
        }
    }

    /// A colour by one of the four modes, into `rgba`.
    private void colour(int mode, float[] two, float[] high, float[] low, float at, float drawn) {
        if (mode == 1) {
            gradient(high, at);
        } else if (mode == 2) {
            for (int c = 0; c < 4; c++) {
                float d = (two[c] - two[4 + c]) * drawn;
                rgba[c] = two[4 + c] + d;
            }
        } else if (mode == 3) {
            gradient(low, at);
            float r = rgba[0];
            float g = rgba[1];
            float b = rgba[2];
            float a = rgba[3];
            gradient(high, at);
            float dr = (rgba[0] - r) * drawn;
            float dg = (rgba[1] - g) * drawn;
            float db = (rgba[2] - b) * drawn;
            float da = (rgba[3] - a) * drawn;
            rgba[0] = r + dr;
            rgba[1] = g + dg;
            rgba[2] = b + db;
            rgba[3] = a + da;
        } else {
            rgba[0] = two[0];
            rgba[1] = two[1];
            rgba[2] = two[2];
            rgba[3] = two[3];
        }
    }

    private static int channel(float v) {
        if (!(v > 0f)) { // NOPMD LogicInversion
            return 0;
        }
        return v >= 1f ? 255 : (int) (v * 255f + 0.5f);
    }

    // ------------------------------------------------------------------ life

    private void room(int n) {
        if (px.length >= n) {
            return;
        }
        int size2 = px.length == 0 ? 16 : px.length * 2;
        while (size2 < n) {
            size2 *= 2;
        }
        px = grown(px, size2);
        py = grown(py, size2);
        vx = grown(vx, size2);
        vy = grown(vy, size2);
        age = grown(age, size2);
        life = grown(life, size2);
        size = grown(size, size2);
        turn = grown(turn, size2);
        chance = grown(chance, size2);
        int[] t = new int[size2];
        if (tint != null) {
            System.arraycopy(tint, 0, t, 0, count);
        }
        tint = t;
    }

    private float[] grown(float[] from, int n) {
        float[] to = new float[n];
        if (from != null) {
            System.arraycopy(from, 0, to, 0, count);
        }
        return to;
    }

    /// The scale the particles and their speeds take, into `spot[2..3]`.
    private void scales(Transform t) {
        if (scalingMode == 0) {
            t.update();
            spot[2] = t.wsx < 0f ? -t.wsx : t.wsx;
            spot[3] = t.wsy < 0f ? -t.wsy : t.wsy;
        } else if (scalingMode == 1) {
            spot[2] = t.scaleX < 0f ? -t.scaleX : t.scaleX;
            spot[3] = t.scaleY < 0f ? -t.scaleY : t.scaleY;
        } else {
            spot[2] = 1f;
            spot[3] = 1f;
        }
    }

    /// Bears one particle. `along` is how far through the system's
    /// duration it is, from 0 to 1.
    private void bear(float along) {
        if (count >= maxParticles) {
            return;
        }
        room(count + 1);
        Transform t = gameObject.transform;
        t.update();
        // A place and a direction in the shape's own axes.
        float lx = 0f;
        float ly = 0f;
        float lz = 0f;
        float dx = 0f;
        float dy = 0f;
        float dz = 1f;
        if ((modules & SHAPE) != 0) {
            float thick = shapeThickness < 0f ? 0f : shapeThickness > 1f ? 1f : shapeThickness;
            switch (shapeType) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 10:
                case 11: {
                    // A sphere seen from in front is a disc, and a circle
                    // is one: out from the middle, around the arc.
                    boolean shell = shapeType == 1 || shapeType == 3 || shapeType == 11;
                    double around = next() * (shapeType >= 10 ? shapeArc : 360f) * Transform.DEG2RAD;
                    float inner = 1f - (shell ? 0f : thick);
                    float span = (1f - inner * inner) * next();
                    float out = (float) Math.sqrt(inner * inner + span);
                    float c = (float) Math.cos(around);
                    float s = (float) Math.sin(around);
                    lx = c * out * shapeRadius;
                    ly = s * out * shapeRadius;
                    dx = c;
                    dy = s;
                    dz = 0f;
                    break;
                }
                case 4:
                case 7:
                case 8:
                case 9: {
                    double around = next() * shapeArc * Transform.DEG2RAD;
                    float inner = 1f - (shapeType == 7 || shapeType == 9 ? 0f : thick);
                    float span = (1f - inner * inner) * next();
                    float out = (float) Math.sqrt(inner * inner + span);
                    float c = (float) Math.cos(around);
                    float s = (float) Math.sin(around);
                    lx = c * out * shapeRadius;
                    ly = s * out * shapeRadius;
                    // The further from the axis, the more it leans out.
                    double lean = shapeAngle * out * Transform.DEG2RAD;
                    float away = (float) Math.sin(lean);
                    dx = c * away;
                    dy = s * away;
                    dz = (float) Math.cos(lean);
                    break;
                }
                case 5:
                case 15:
                case 16:
                    lx = next() - 0.5f;
                    ly = next() - 0.5f;
                    lz = next() - 0.5f;
                    break;
                case 18:
                    lx = next() - 0.5f;
                    ly = next() - 0.5f;
                    break;
                case 12: {
                    float f = next() * 2f - 1f;
                    lx = f * shapeRadius;
                    dy = 1f;
                    dz = 0f;
                    break;
                }
                default:
                    break;
            }
            lx = lx * shapeScaleX + shapeX;
            ly = ly * shapeScaleY + shapeY;
            lz = lz * shapeScaleZ + shapeZ;
        }
        // Onto the screen's plane.
        float ax = m00 * lx;
        float bx = m01 * ly;
        float cx = m02 * lz;
        float ay = m10 * lx;
        float by = m11 * ly;
        float cy = m12 * lz;
        float sx = ax + bx + cx;
        float sy = ay + by + cy;
        ax = m00 * dx;
        bx = m01 * dy;
        cx = m02 * dz;
        ay = m10 * dx;
        by = m11 * dy;
        cy = m12 * dz;
        float ux = ax + bx + cx;
        float uy = ay + by + cy;
        scales(t);
        float speed = pick(START_SPEED, along);
        int i = count++;
        float wsx = scalingMode == 2 ? 1f : t.wsx;
        float wsy = scalingMode == 2 ? 1f : t.wsy;
        if (space == 1) {
            // In the world: where the object is now, turned as it is.
            float px0 = sx * wsx;
            float py0 = sy * wsy;
            float a = px0 * t.wcos;
            float b = py0 * t.wsin;
            float c = px0 * t.wsin;
            float d = py0 * t.wcos;
            px[i] = t.wx + (a - b);
            py[i] = t.wy + (c + d);
            float vx0 = ux * speed * spot[2];
            float vy0 = uy * speed * spot[3];
            a = vx0 * t.wcos;
            b = vy0 * t.wsin;
            c = vx0 * t.wsin;
            d = vy0 * t.wcos;
            vx[i] = a - b;
            vy[i] = c + d;
        } else {
            px[i] = sx;
            py[i] = sy;
            vx[i] = ux * speed;
            vy[i] = uy * speed;
        }
        age[i] = 0f;
        float lasts = pick(START_LIFETIME, along);
        life[i] = lasts > 0f ? lasts : 0f;
        size[i] = pick(START_SIZE, along);
        // The file has radians, clockwise; the screen wants degrees.
        turn[i] = pick(START_ROTATION, along) * (float) Transform.RAD2DEG;
        float drawn = next();
        chance[i] = drawn;
        colour(startColorMode, startColors, startGradient, startLowGradient, along, drawn);
        tint[i] = (channel(rgba[3]) << 24) | (channel(rgba[0]) << 16) | (channel(rgba[1]) << 8) | channel(rgba[2]);
    }

    private void restart() {
        seed = seed0 == 0 ? 0x2545f491 : seed0;
        clock = 0f;
        owed = 0f;
        placed = false;
        delay = pick(START_DELAY, 0f);
    }

    @Override
    public void $tick(float dt) {
        if (!begun) {
            begun = true;
            if (playOnAwake && !playing) {
                Play();
            }
        }
        if (paused || (!playing && count == 0)) {
            return;
        }
        float step = dt * simulationSpeed;
        if (!(step > 0f)) { // NOPMD LogicInversion
            return;
        }
        Transform t = gameObject.transform;
        t.update();
        // The particles there are grow older and move.
        float gravity = curveMode[GRAVITY] == 0 && curveMax[GRAVITY] == 0f ? 0f
                : over(GRAVITY, duration > 0f ? clock / duration : 0f, 0.5f) * PhysicsWorld.gravityY;
        boolean pushed = (modules & VELOCITY_OVER_LIFETIME) != 0;
        boolean spins = (modules & ROTATION_OVER_LIFETIME_MODULE) != 0;
        int kept = 0;
        for (int i = 0; i < count; i++) {
            float older = age[i] + step;
            if (older >= life[i]) {
                continue;
            }
            float at = life[i] > 0f ? older / life[i] : 1f;
            float nvy = vy[i] + gravity * step;
            float mx = vx[i];
            float my = nvy;
            if (pushed) {
                float ox = over(VELOCITY_X, at, chance[i]);
                float oy = over(VELOCITY_Y, at, chance[i]);
                if (velocityInWorld != (space == 1) && t.wrot != 0f) {
                    // Given in the other axes than the particles are in.
                    float sin = velocityInWorld ? -t.wsin : t.wsin;
                    float a = ox * t.wcos;
                    float b = oy * sin;
                    float c = ox * sin;
                    float d = oy * t.wcos;
                    ox = a - b;
                    oy = c + d;
                }
                mx += ox;
                my += oy;
            }
            float nx = px[i] + mx * step;
            float ny = py[i] + my * step;
            float nturn = turn[i];
            if (spins) {
                float spin = over(ROTATION_OVER_LIFETIME, at, chance[i]) * (float) Transform.RAD2DEG;
                nturn += spin * step;
            }
            px[kept] = nx;
            py[kept] = ny;
            vx[kept] = vx[i];
            vy[kept] = nvy;
            age[kept] = older;
            life[kept] = life[i];
            size[kept] = size[i];
            turn[kept] = nturn;
            chance[kept] = chance[i];
            tint[kept] = tint[i];
            kept++;
        }
        count = kept;
        if (!playing) {
            return;
        }
        if (delay > 0f) {
            delay -= step;
            if (delay > 0f) {
                return;
            }
        }
        float before = clock;
        float after = clock + step;
        if (emitting && (modules & EMISSION) != 0) {
            float along = duration > 0f ? before / duration : 0f;
            float moved = 0f;
            if (placed) {
                float dx = t.wx - lastX;
                float dy = t.wy - lastY;
                float xx = dx * dx;
                float yy = dy * dy;
                moved = (float) Math.sqrt(xx + yy);
            }
            float byTime = pick(RATE_OVER_TIME, along) * step;
            float byDistance = pick(RATE_OVER_DISTANCE, along) * moved;
            owed += byTime + byDistance;
            while (owed >= 1f) {
                owed -= 1f;
                bear(along);
            }
            // Bursts whose time this step reaches. A looping system that
            // wraps has its early bursts again in the next cycle.
            for (int b = 0; b + 5 < bursts.length; b += 6) {
                int cycles = (int) bursts[b + 3];
                if (cycles <= 0) {
                    // Zero is Unity's "for ever".
                    cycles = 1 + (int) ((duration - bursts[b]) / (bursts[b + 4] > 0.0001f ? bursts[b + 4] : 0.0001f));
                }
                for (int c = 0; c < cycles; c++) {
                    float gap = bursts[b + 4] * c;
                    float when = bursts[b] + gap;
                    if (when > duration) {
                        break;
                    }
                    boolean due = before == 0f ? when >= 0f && when <= after && when <= before + step
                            : when > before && when <= after;
                    if (!due) {
                        continue;
                    }
                    if (bursts[b + 5] < 1f && next() >= bursts[b + 5]) {
                        continue;
                    }
                    float span = (bursts[b + 2] - bursts[b + 1]) * next();
                    int n = (int) (bursts[b + 1] + span + 0.5f);
                    for (int k = 0; k < n; k++) {
                        bear(along);
                    }
                }
            }
        }
        lastX = t.wx;
        lastY = t.wy;
        placed = true;
        clock = after;
        if (clock >= duration) {
            if (looping) {
                // The next cycle starts on this frame's edge, so that a
                // burst is never stepped over by the wrap.
                clock = 0f;
            } else {
                emitting = false;
                if (count == 0) {
                    playing = false;
                }
            }
        }
    }

    /// Adds the particles to the frame, each as the renderer's sprite.
    void draw(DrawView view, Sprite sprite, int argb, float least, float most, int sortingOrder,
            int sortingLayerID) {
        if (count == 0 || sprite == null) {
            return;
        }
        Transform t = gameObject.transform;
        t.update();
        scales(t);
        float wide = sprite.width / sprite.pixelsPerUnit;
        float tall = sprite.height / sprite.pixelsPerUnit;
        if (!(wide > 0f) || !(tall > 0f)) { // NOPMD LogicInversion
            return;
        }
        // The most and the least a particle may be, as parts of the
        // height of the view.
        float cap = most > 0f ? most * view.$halfHeight() * 2f : 0f;
        float floor = least > 0f ? least * view.$halfHeight() * 2f : 0f;
        boolean sizes = (modules & SIZE_OVER_LIFETIME_MODULE) != 0;
        boolean fades = (modules & COLOR_OVER_LIFETIME) != 0;
        for (int i = 0; i < count; i++) {
            float at = life[i] > 0f ? age[i] / life[i] : 1f;
            float s = size[i];
            if (sizes) {
                s = s * over(SIZE_OVER_LIFETIME, at, chance[i]);
            }
            s = s * spot[2];
            if (cap > 0f && s > cap) {
                s = cap;
            }
            if (s < floor) {
                s = floor;
            }
            if (!(s > 0f)) { // NOPMD LogicInversion
                continue;
            }
            int c = tint[i];
            if (fades) {
                colour(lifeColorMode, lifeColors, lifeGradient, lifeLowGradient, at, chance[i]);
                c = (mul(c >>> 24, rgba[3]) << 24) | (mul(c >> 16, rgba[0]) << 16) | (mul(c >> 8, rgba[1]) << 8)
                        | mul(c, rgba[2]);
            }
            if (argb != 0xffffffff) {
                c = (both(c >>> 24, argb >>> 24) << 24) | (both(c >> 16, argb >> 16) << 16)
                        | (both(c >> 8, argb >> 8) << 8) | both(c, argb);
            }
            if ((c >>> 24) == 0) {
                continue;
            }
            float x = px[i];
            float y = py[i];
            if (space != 1) {
                float lx = x * (scalingMode == 2 ? 1f : t.wsx);
                float ly = y * (scalingMode == 2 ? 1f : t.wsy);
                float a = lx * t.wcos;
                float b = ly * t.wsin;
                float d = lx * t.wsin;
                float e = ly * t.wcos;
                x = t.wx + (a - b);
                y = t.wy + (d + e);
            }
            view.$sprite(sprite, x, y, t.wz, s / wide, s / wide, -turn[i], c, false, false, sortingOrder,
                    sortingLayerID);
        }
    }

    private static int mul(int channel, float by) {
        int v = (int) ((channel & 255) * by + 0.5f);
        return v < 0 ? 0 : v > 255 ? 255 : v;
    }

    private static int both(int a, int b) {
        return ((a & 255) * (b & 255) + 127) / 255;
    }

    // ------------------------------------------------------------------- API

    public boolean get_isPlaying() {
        return playing && !paused;
    }

    public boolean get_isEmitting() {
        return playing && emitting && !paused;
    }

    public boolean get_isStopped() {
        return !playing && !paused;
    }

    public boolean get_isPaused() {
        return paused;
    }

    public int get_particleCount() {
        return count;
    }

    public float get_time() {
        return clock;
    }

    public void set_time(float value) {
        clock = value < 0f ? 0f : value;
    }

    /// Starts the system, or carries on one that was paused. One that was
    /// stopped starts from the beginning, with the same random numbers.
    public void Play() {
        begun = true;
        if (paused) {
            paused = false;
            if (playing) {
                return;
            }
        }
        if (!playing) {
            restart();
        }
        playing = true;
        emitting = true;
    }

    public void Play(boolean withChildren) {
        Play();
        if (withChildren) {
            children(0);
        }
    }

    /// Stops emitting. The particles there are live out their lives.
    public void Stop() {
        begun = true;
        playing = false;
        emitting = false;
        paused = false;
    }

    public void Stop(boolean withChildren) {
        Stop();
        if (withChildren) {
            children(1);
        }
    }

    /// `stopBehavior` 0 removes the particles there are as well.
    public void Stop(boolean withChildren, int stopBehavior) {
        Stop();
        if (stopBehavior == 0) {
            count = 0;
        }
        if (withChildren) {
            children(stopBehavior == 0 ? 2 : 1);
        }
    }

    public void Pause() {
        paused = true;
    }

    public void Pause(boolean withChildren) {
        Pause();
        if (withChildren) {
            children(3);
        }
    }

    public void Clear() {
        count = 0;
    }

    public void Clear(boolean withChildren) {
        Clear();
        if (withChildren) {
            children(4);
        }
    }

    /// Bears particles now, whether or not the system is playing.
    public void Emit(int howMany) {
        if (seed == 0) {
            seed = seed0 == 0 ? 0x2545f491 : seed0;
        }
        float along = duration > 0f ? clock / duration : 0f;
        for (int i = 0; i < howMany; i++) {
            bear(along);
        }
    }

    private void children(int what) {
        Transform t = gameObject.transform;
        int n = t.get_childCount();
        for (int i = 0; i < n; i++) {
            java.lang.Object c = t.GetChild(i).gameObject.GetComponent(ParticleSystem.class);
            if (!(c instanceof ParticleSystem)) {
                continue;
            }
            ParticleSystem p = (ParticleSystem) c;
            if (what == 0) {
                p.Play(true);
            } else if (what == 1) {
                p.Stop(true);
            } else if (what == 2) {
                p.Stop(true, 0);
            } else if (what == 3) {
                p.Pause(true);
            } else {
                p.Clear(true);
            }
        }
    }

    public MainModule get_main(MainModule ret) {
        ret.system = this;
        return ret;
    }

    public EmissionModule get_emission(EmissionModule ret) {
        ret.system = this;
        return ret;
    }

    public VelocityOverLifetimeModule get_velocityOverLifetime(VelocityOverLifetimeModule ret) {
        ret.system = this;
        return ret;
    }

    void read(int which, MinMaxCurve ret) {
        ret.mode = curveMode[which];
        ret.max = curveMax[which];
        ret.min = curveMin[which];
    }

    /// A script's curve is a constant or two: the curves a scene gave are
    /// kept when the mode it sets is one that uses them.
    void write(int which, MinMaxCurve value) {
        curveMode[which] = value.mode;
        curveMax[which] = value.max;
        curveMin[which] = value.min;
    }

    // --------------------------------------------------------------- structs

    /// `ParticleSystem.MinMaxCurve`: a number, or a number between two.
    /// The curves of the two curve modes are a scene's alone; a script
    /// reads and sets the constants.
    public static final class MinMaxCurve implements Struct {
        int mode;
        float max;
        float min;

        public MinMaxCurve() {
        }

        public MinMaxCurve(float constant) {
            max = constant;
        }

        public MinMaxCurve(float low, float high) {
            mode = 3;
            min = low;
            max = high;
        }

        public static MinMaxCurve op_Implicit(float constant, MinMaxCurve ret) {
            ret.mode = 0;
            ret.max = constant;
            ret.min = 0f;
            return ret;
        }

        public int get_mode() {
            return mode;
        }

        public void set_mode(int value) {
            mode = value;
        }

        public float get_constant() {
            return max;
        }

        public void set_constant(float value) {
            max = value;
        }

        public float get_constantMax() {
            return max;
        }

        public void set_constantMax(float value) {
            max = value;
        }

        public float get_constantMin() {
            return min;
        }

        public void set_constantMin(float value) {
            min = value;
        }

        public float get_curveMultiplier() {
            return max;
        }

        public void set_curveMultiplier(float value) {
            max = value;
        }

        public MinMaxCurve $copy() {
            MinMaxCurve c = new MinMaxCurve();
            c.$assign(this);
            return c;
        }

        public void $assign(MinMaxCurve other) {
            mode = other.mode;
            max = other.max;
            min = other.min;
        }

        public static void $store(MinMaxCurve[] array, int index, MinMaxCurve value) {
            array[index].$assign(value);
        }

        public static MinMaxCurve[] $newArray(int length) {
            MinMaxCurve[] a = new MinMaxCurve[length];
            for (int i = 0; i < length; i++) {
                a[i] = new MinMaxCurve();
            }
            return a;
        }

        @Override
        public java.lang.Object $copyValue() {
            return $copy();
        }

        @Override
        public void $clear() {
            mode = 0;
            max = 0f;
            min = 0f;
        }

        @Override
        public boolean equals(java.lang.Object o) {
            if (!(o instanceof MinMaxCurve)) {
                return false;
            }
            MinMaxCurve c = (MinMaxCurve) o;
            return mode == c.mode && max == c.max && min == c.min;
        }

        @Override
        public int hashCode() {
            return mode * 31 + (int) (max * 1000f) + (int) (min * 1000f) * 17;
        }
    }

    /// What the module structs share: they are a way of naming the system
    /// they came from, and copying one copies the name.
    abstract static class Module implements Struct {
        ParticleSystem system;

        final ParticleSystem system() {
            if (system == null) {
                throw new NullPointerException("a particle system module that came from no system");
            }
            return system;
        }

        @Override
        public void $clear() {
            system = null;
        }

        @Override
        public boolean equals(java.lang.Object o) {
            return o instanceof Module && o.getClass() == getClass()
                    && ((Module) o).system == system; // NOPMD CompareObjectsWithEquals
        }

        @Override
        public int hashCode() {
            return system == null ? 0 : 1;
        }
    }

    /// `ParticleSystem.MainModule`.
    public static final class MainModule extends Module {
        public float get_duration() {
            return system().duration;
        }

        public void set_duration(float value) {
            system().duration = value;
        }

        public boolean get_loop() {
            return system().looping;
        }

        public void set_loop(boolean value) {
            system().looping = value;
        }

        public boolean get_playOnAwake() {
            return system().playOnAwake;
        }

        public void set_playOnAwake(boolean value) {
            system().playOnAwake = value;
        }

        public MinMaxCurve get_startLifetime(MinMaxCurve ret) {
            system().read(START_LIFETIME, ret);
            return ret;
        }

        public void set_startLifetime(MinMaxCurve value) {
            system().write(START_LIFETIME, value);
        }

        public MinMaxCurve get_startSpeed(MinMaxCurve ret) {
            system().read(START_SPEED, ret);
            return ret;
        }

        public void set_startSpeed(MinMaxCurve value) {
            system().write(START_SPEED, value);
        }

        public MinMaxCurve get_startSize(MinMaxCurve ret) {
            system().read(START_SIZE, ret);
            return ret;
        }

        public void set_startSize(MinMaxCurve value) {
            system().write(START_SIZE, value);
        }

        public MinMaxCurve get_gravityModifier(MinMaxCurve ret) {
            system().read(GRAVITY, ret);
            return ret;
        }

        public void set_gravityModifier(MinMaxCurve value) {
            system().write(GRAVITY, value);
        }

        public int get_maxParticles() {
            return system().maxParticles;
        }

        public void set_maxParticles(int value) {
            system().maxParticles = value;
        }

        public float get_simulationSpeed() {
            return system().simulationSpeed;
        }

        public void set_simulationSpeed(float value) {
            system().simulationSpeed = value;
        }

        public MainModule $copy() {
            MainModule m = new MainModule();
            m.system = system;
            return m;
        }

        public void $assign(MainModule other) {
            system = other.system;
        }

        public static void $store(MainModule[] array, int index, MainModule value) {
            array[index].$assign(value);
        }

        public static MainModule[] $newArray(int length) {
            MainModule[] a = new MainModule[length];
            for (int i = 0; i < length; i++) {
                a[i] = new MainModule();
            }
            return a;
        }

        @Override
        public java.lang.Object $copyValue() {
            return $copy();
        }
    }

    /// `ParticleSystem.EmissionModule`.
    public static final class EmissionModule extends Module {
        public boolean get_enabled() {
            return (system().modules & EMISSION) != 0;
        }

        public void set_enabled(boolean value) {
            ParticleSystem s = system();
            s.modules = value ? s.modules | EMISSION : s.modules & ~EMISSION;
        }

        public MinMaxCurve get_rateOverTime(MinMaxCurve ret) {
            system().read(RATE_OVER_TIME, ret);
            return ret;
        }

        public void set_rateOverTime(MinMaxCurve value) {
            system().write(RATE_OVER_TIME, value);
        }

        public MinMaxCurve get_rateOverDistance(MinMaxCurve ret) {
            system().read(RATE_OVER_DISTANCE, ret);
            return ret;
        }

        public void set_rateOverDistance(MinMaxCurve value) {
            system().write(RATE_OVER_DISTANCE, value);
        }

        public EmissionModule $copy() {
            EmissionModule m = new EmissionModule();
            m.system = system;
            return m;
        }

        public void $assign(EmissionModule other) {
            system = other.system;
        }

        public static void $store(EmissionModule[] array, int index, EmissionModule value) {
            array[index].$assign(value);
        }

        public static EmissionModule[] $newArray(int length) {
            EmissionModule[] a = new EmissionModule[length];
            for (int i = 0; i < length; i++) {
                a[i] = new EmissionModule();
            }
            return a;
        }

        @Override
        public java.lang.Object $copyValue() {
            return $copy();
        }
    }

    /// `ParticleSystem.VelocityOverLifetimeModule`.
    public static final class VelocityOverLifetimeModule extends Module {
        public boolean get_enabled() {
            return (system().modules & VELOCITY_OVER_LIFETIME) != 0;
        }

        public void set_enabled(boolean value) {
            ParticleSystem s = system();
            s.modules = value ? s.modules | VELOCITY_OVER_LIFETIME : s.modules & ~VELOCITY_OVER_LIFETIME;
        }

        public MinMaxCurve get_x(MinMaxCurve ret) {
            system().read(VELOCITY_X, ret);
            return ret;
        }

        public void set_x(MinMaxCurve value) {
            system().write(VELOCITY_X, value);
        }

        public MinMaxCurve get_y(MinMaxCurve ret) {
            system().read(VELOCITY_Y, ret);
            return ret;
        }

        public void set_y(MinMaxCurve value) {
            system().write(VELOCITY_Y, value);
        }

        public MinMaxCurve get_z(MinMaxCurve ret) {
            system().read(VELOCITY_Z, ret);
            return ret;
        }

        public void set_z(MinMaxCurve value) {
            system().write(VELOCITY_Z, value);
        }

        public VelocityOverLifetimeModule $copy() {
            VelocityOverLifetimeModule m = new VelocityOverLifetimeModule();
            m.system = system;
            return m;
        }

        public void $assign(VelocityOverLifetimeModule other) {
            system = other.system;
        }

        public static void $store(VelocityOverLifetimeModule[] array, int index, VelocityOverLifetimeModule value) {
            array[index].$assign(value);
        }

        public static VelocityOverLifetimeModule[] $newArray(int length) {
            VelocityOverLifetimeModule[] a = new VelocityOverLifetimeModule[length];
            for (int i = 0; i < length; i++) {
                a[i] = new VelocityOverLifetimeModule();
            }
            return a;
        }

        @Override
        public java.lang.Object $copyValue() {
            return $copy();
        }
    }
}
