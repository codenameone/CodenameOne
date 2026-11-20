/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.play;

/// A drop of ink a creature leaves when it is cut up. Fly through it and the
/// multiplier goes up by one; leave it and it soaks into the page.
public final class Drop {
    public static final float LIFE = 7;
    public float x;
    public float y;
    public float vx;
    public float vy;
    public float life = LIFE;
    public boolean dead;
}
