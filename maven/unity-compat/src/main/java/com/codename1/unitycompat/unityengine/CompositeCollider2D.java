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
import java.util.ArrayList;

/// `UnityEngine.CompositeCollider2D`: makes one collider of the others on
/// its body that are marked as used by it.
///
/// Those colliders make no shapes of their own. This one makes theirs, as
/// outlines -- closed chains of edges, hollow inside -- or as filled
/// polygons, and it is the collider a collision with any of them names.
///
/// A tilemap's tiles are merged: neighbouring tiles become one outline
/// with nothing where they meet. Other colliders are each turned into an
/// outline or a polygon of their own and are not merged with one another;
/// two boxes side by side keep the seam between them.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class CompositeCollider2D extends Collider2D {
    /// Geometry types, by the numbers a scene file uses.
    private static final int OUTLINES = 0;
    private int geometryType;
    /// The paths made the last time the shapes were, for a script that
    /// asks: every outline's points, in the body's units.
    private final ArrayList paths = new ArrayList();

    /// What a scene file sets.
    public void $setup(int geometry) {
        geometryType = geometry;
    }

    @Override
    public Component $new() {
        return new CompositeCollider2D();
    }

    @Override
    public void $copyFrom(Component source) {
        super.$copyFrom(source);
        geometryType = ((CompositeCollider2D) source).geometryType;
    }

    @Override
    void shape(PhysicsWorld.Shapes out) {
        paths.clear();
        GameObject body = owner;
        if (body == null || body.fixturesOf == null) {
            return;
        }
        Transform mine = out.at;
        ArrayList members = body.fixturesOf;
        out.paths = paths;
        for (int i = 0; i < members.size(); i++) { // NOPMD ForLoopCanBeForeach
            Collider2D c = (Collider2D) members.get(i);
            if (c == this || !c.usedByComposite) { // NOPMD CompareObjectsWithEquals
                continue;
            }
            Transform ct = c.gameObject.transform;
            ct.update();
            out.at = ct;
            c.compose(out, geometryType == OUTLINES);
        }
        out.paths = null;
        out.at = mine;
    }

    /// Makes the shapes again from the colliders as they are now.
    public void GenerateGeometry() {
        PhysicsWorld.changed(gameObject);
        PhysicsWorld.flush();
    }

    public int get_pathCount() {
        if (gameObject.registered) {
            PhysicsWorld.flush();
        }
        return paths.size();
    }

    public int get_pointCount() {
        int n = 0;
        int count = get_pathCount();
        for (int i = 0; i < count; i++) {
            n += ((float[]) paths.get(i)).length / 2;
        }
        return n;
    }

    public int GetPathPointCount(int index) {
        return index < 0 || index >= get_pathCount() ? 0 : ((float[]) paths.get(index)).length / 2;
    }

    public int GetPath(int index, Vector2[] points) {
        if (index < 0 || index >= get_pathCount()) {
            return 0;
        }
        float[] path = (float[]) paths.get(index);
        int n = path.length / 2 < points.length ? path.length / 2 : points.length;
        for (int i = 0; i < n; i++) {
            points[i].x = path[i * 2];
            points[i].y = path[i * 2 + 1];
        }
        return n;
    }
}
