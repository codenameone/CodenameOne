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

/// `UnityEngine.PolygonCollider2D`: one or more closed outlines, each of
/// which may be concave.
///
/// Box2D collides convex polygons of at most eight corners, so an outline is
/// cut into such pieces (see [PhysicsWorld#decompose]) and each piece becomes
/// a fixture. The pieces depend only on the outline, not on where the object
/// is or how it is scaled, so they are worked out once per outline and
/// shared by every copy of the collider: a path array is never modified,
/// only replaced.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class PolygonCollider2D extends Collider2D {
    private static final float[][] NONE = new float[0][];
    /// Each path as x0, y0, x1, y1, ...
    private float[][] paths = NONE;
    /// The convex pieces of [#paths], or null until they are needed.
    private float[][] pieces;

    /// What generated code sets: every path, as x and y pairs. The arrays
    /// are kept, not copied, and must not be written to afterwards.
    public void $paths(float[][] value) {
        paths = value;
        pieces = PhysicsWorld.sharedPieces(value);
    }

    public int get_pathCount() {
        return paths.length;
    }

    public void set_pathCount(int value) {
        float[][] grown = new float[value < 0 ? 0 : value][];
        for (int i = 0; i < grown.length; i++) {
            grown[i] = i < paths.length ? paths[i] : new float[0];
        }
        replace(grown);
    }

    private void replace(float[][] value) {
        paths = value;
        pieces = null;
        PhysicsWorld.changed(gameObject);
    }

    private static Vector2[] unpack(float[] path) {
        Vector2[] out = new Vector2[path.length / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = new Vector2();
            out[i].x = path[i * 2];
            out[i].y = path[i * 2 + 1];
        }
        return out;
    }

    private static float[] pack(Vector2[] points) {
        float[] out = new float[points.length * 2];
        for (int i = 0; i < points.length; i++) {
            out[i * 2] = points[i].x;
            out[i * 2 + 1] = points[i].y;
        }
        return out;
    }

    /// The first path.
    public Vector2[] get_points() {
        return paths.length == 0 ? new Vector2[0] : unpack(paths[0]);
    }

    public void set_points(Vector2[] value) {
        SetPath(0, value);
    }

    public Vector2[] GetPath(int index) {
        return unpack(paths[index]);
    }

    public void SetPath(int index, Vector2[] points) {
        float[][] next = new float[index < paths.length ? paths.length : index + 1][];
        for (int i = 0; i < next.length; i++) {
            next[i] = i < paths.length ? paths[i] : new float[0];
        }
        next[index] = pack(points);
        replace(next);
    }

    public int GetTotalPointCount() {
        int n = 0;
        for (int i = 0; i < paths.length; i++) { // NOPMD ForLoopCanBeForeach
            n += paths[i].length / 2;
        }
        return n;
    }

    @Override
    void shape(PhysicsWorld.Shapes out) {
        if (pieces == null) {
            pieces = PhysicsWorld.decompose(paths);
        }
        for (int i = 0; i < pieces.length; i++) { // NOPMD ForLoopCanBeForeach
            out.convex(pieces[i], $offsetX, $offsetY);
        }
    }

    @Override
    public Component $new() {
        return new PolygonCollider2D();
    }

    @Override
    public void $copyFrom(Component source) {
        super.$copyFrom(source);
        PolygonCollider2D p = (PolygonCollider2D) source;
        paths = p.paths;
        pieces = p.pieces;
    }
}
