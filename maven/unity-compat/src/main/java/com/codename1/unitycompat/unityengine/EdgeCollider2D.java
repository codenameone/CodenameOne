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

/// `UnityEngine.EdgeCollider2D`: an open line through points, a surface
/// with no inside. Things collide with it from either side and a ray is
/// stopped by it, but no point is ever in it.
///
/// Not implemented: `edgeRadius`, which gives the line a thickness; the
/// line is always thin.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class EdgeCollider2D extends Collider2D {
    /// The points as x and y pairs. Unity's default is a line one unit
    /// long across the object.
    private float[] line = {-0.5f, 0f, 0.5f, 0f};

    /// The points as x and y pairs; generated code sets them from a scene.
    public void $points(float[] points) {
        line = points;
        PhysicsWorld.changed(gameObject);
    }

    public Vector2[] get_points() {
        Vector2[] out = new Vector2[line.length / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = new Vector2();
            out[i].x = line[i * 2];
            out[i].y = line[i * 2 + 1];
        }
        return out;
    }

    public void set_points(Vector2[] value) {
        float[] next = new float[value.length * 2];
        for (int i = 0; i < value.length; i++) {
            next[i * 2] = value[i].x;
            next[i * 2 + 1] = value[i].y;
        }
        line = next;
        PhysicsWorld.changed(gameObject);
    }

    public int get_pointCount() {
        return line.length / 2;
    }

    public int get_edgeCount() {
        int n = line.length / 2;
        return n > 0 ? n - 1 : 0;
    }

    @Override
    void shape(PhysicsWorld.Shapes out) {
        out.chain(line, $offsetX, $offsetY);
    }

    @Override
    public Component $new() {
        return new EdgeCollider2D();
    }

    @Override
    public void $copyFrom(Component source) {
        super.$copyFrom(source);
        // Never written in place, so the two may share it.
        line = ((EdgeCollider2D) source).line;
    }
}
