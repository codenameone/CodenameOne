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

import UnityEngine.Vector2;

/// `UnityEngine.BoxCollider2D`.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class BoxCollider2D extends Collider2D {
    public float $width = 1f;
    public float $height = 1f;
    // Kept between calls on purpose: a local would allocate every time the
    // collider is rebuilt.
    @SuppressWarnings("PMD.SingularField")
    private final float[] corners = new float[8];

    public Vector2 get_size(Vector2 ret) {
        ret.x = $width;
        ret.y = $height;
        return ret;
    }

    public void set_size(Vector2 value) {
        $width = value.x;
        $height = value.y;
        PhysicsWorld.changed(gameObject);
    }

    @Override
    void shape(PhysicsWorld.Shapes out) {
        float hw = $width * 0.5f;
        float hh = $height * 0.5f;
        corners[0] = $offsetX - hw;
        corners[1] = $offsetY - hh;
        corners[2] = $offsetX + hw;
        corners[3] = $offsetY - hh;
        corners[4] = $offsetX + hw;
        corners[5] = $offsetY + hh;
        corners[6] = $offsetX - hw;
        corners[7] = $offsetY + hh;
        out.convex(corners, 0f, 0f);
    }

    @Override
    public Component $new() {
        return new BoxCollider2D();
    }

    @Override
    public void $copyFrom(Component source) {
        super.$copyFrom(source);
        $width = ((BoxCollider2D) source).$width;
        $height = ((BoxCollider2D) source).$height;
    }
}
