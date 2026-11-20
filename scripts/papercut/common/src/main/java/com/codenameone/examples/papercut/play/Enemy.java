/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.play;

/// One creature on the sheet. Every kind shares this class and differs only in
/// how [Arena] steers it; the fields are public because the arena and the view
/// are the only readers and a getter per field would be noise.
public final class Enemy {
    /// Folds toward the quill and keeps folding: the simplest chaser.
    public static final int DART = 0;
    /// Drifts and bounces, spinning; harmless alone, deadly in a crowd.
    public static final int PINWHEEL = 1;
    /// Glides in curves; cut it and it unfolds into two cranelets.
    public static final int CRANE = 2;
    /// What a crane leaves behind: small, fast, orbiting.
    public static final int CRANELET = 3;
    /// Lines up with the quill, then snips across the sheet in a straight dash.
    public static final int SNIPPER = 4;
    /// A pool of ink that pulls everything toward it, and bursts into darts
    /// when it has drunk too much.
    public static final int INKWELL = 5;
    /// The gate of the Fold mode: a strip of paper that bursts when the quill
    /// flies between its ends.
    public static final int GATE = 6;

    public static final int KINDS = 7;

    /// Collision radius, by kind, in sheet units.
    static final float[] RADIUS = {18, 22, 28, 14, 22, 34, 60};
    /// Points, by kind, before the multiplier.
    static final int[] POINTS = {50, 25, 100, 50, 150, 200, 0};
    /// Hits it takes, by kind.
    static final int[] HP = {1, 1, 1, 1, 2, 12, 1};
    /// The paper colour, by kind, which the bursts and the ink stains take.
    public static final int[] COLOUR = {0xe24a3c, 0xf4b434, 0x8c70c4, 0x8c70c4, 0xe86034, 0x262c42, 0x40a0c4};

    public int kind;
    public float x;
    public float y;
    public float vx;
    public float vy;
    /// Facing, in degrees clockwise from the +x axis.
    public float angle;
    public float r;
    public int hp;
    /// Seconds since it was drawn in. Under [Arena#SPAWN_TIME] it is still
    /// being drawn, harmless and untouchable.
    public float age;
    /// Per kind scratch: the snipper's phase timer, the inkwell's fill.
    public float t;
    public float u;
    public boolean dead;

    Enemy set(int kind, float x, float y) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        vx = 0;
        vy = 0;
        angle = 0;
        r = RADIUS[kind];
        hp = HP[kind];
        age = 0;
        t = 0;
        u = 0;
        dead = false;
        return this;
    }

    /// True once drawn in fully, when it collides and can be shot.
    public boolean live() {
        return age >= Arena.SPAWN_TIME && !dead;
    }
}
