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

import UnityEngine.Vector3;
import UnityEngine.Vector3Int;

/// `UnityEngine.GridLayout`: a grid of cells laid over the plane of an
/// object -- the arithmetic between a place and the cell it is in.
///
/// The cells are rectangles: a cell is `cellSize` across and the next one
/// starts a `cellGap` after it. Hexagonal and isometric layouts are not
/// implemented, and the scene compiler says so of a grid that asks for one.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public class GridLayout extends Behaviour {
    float cellWidth = 1f;
    float cellHeight = 1f;
    float gapX;
    float gapY;

    /// What a scene file sets.
    public void $cells(float width, float height, float gapWidth, float gapHeight) {
        cellWidth = width;
        cellHeight = height;
        gapX = gapWidth;
        gapY = gapHeight;
    }

    /// The grid whose cells these are: this one, or for a tilemap the
    /// [Grid] above it.
    GridLayout layout() {
        return this;
    }

    /// How far apart the corners of two neighbouring cells are.
    final float pitchX() {
        GridLayout g = layout();
        return g.cellWidth + g.gapX;
    }

    final float pitchY() {
        GridLayout g = layout();
        return g.cellHeight + g.gapY;
    }

    static int floor(float v) {
        int whole = (int) v;
        return whole > v ? whole - 1 : whole;
    }

    public Vector3 get_cellSize(Vector3 ret) {
        GridLayout g = layout();
        ret.x = g.cellWidth;
        ret.y = g.cellHeight;
        ret.z = 0f;
        return ret;
    }

    public Vector3 get_cellGap(Vector3 ret) {
        GridLayout g = layout();
        ret.x = g.gapX;
        ret.y = g.gapY;
        ret.z = 0f;
        return ret;
    }

    public Vector3Int LocalToCell(Vector3 localPosition, Vector3Int ret) {
        float px = pitchX();
        float py = pitchY();
        ret.x = px != 0f ? floor(localPosition.x / px) : 0;
        ret.y = py != 0f ? floor(localPosition.y / py) : 0;
        ret.z = 0;
        return ret;
    }

    /// The corner of a cell nearest the origin.
    public Vector3 CellToLocal(Vector3Int cellPosition, Vector3 ret) {
        ret.x = cellPosition.x * pitchX();
        ret.y = cellPosition.y * pitchY();
        ret.z = 0f;
        return ret;
    }

    public Vector3Int WorldToCell(Vector3 worldPosition, Vector3Int ret) {
        Vector3 local = new Vector3();
        gameObject.transform.InverseTransformPoint(worldPosition, local);
        return LocalToCell(local, ret);
    }

    public Vector3 CellToWorld(Vector3Int cellPosition, Vector3 ret) {
        CellToLocal(cellPosition, ret);
        return gameObject.transform.TransformPoint(ret, ret);
    }

    public Vector3 GetCellCenterLocal(Vector3Int position, Vector3 ret) {
        GridLayout g = layout();
        float cx = position.x * pitchX();
        float cy = position.y * pitchY();
        float hx = g.cellWidth * 0.5f;
        float hy = g.cellHeight * 0.5f;
        ret.x = cx + hx;
        ret.y = cy + hy;
        ret.z = 0f;
        return ret;
    }

    public Vector3 GetCellCenterWorld(Vector3Int position, Vector3 ret) {
        GetCellCenterLocal(position, ret);
        return gameObject.transform.TransformPoint(ret, ret);
    }

    @Override
    public void $copyFrom(Component source) {
        super.$copyFrom(source);
        GridLayout g = (GridLayout) source;
        cellWidth = g.cellWidth;
        cellHeight = g.cellHeight;
        gapX = g.gapX;
        gapY = g.gapY;
    }
}
