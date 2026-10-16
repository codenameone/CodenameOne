/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter;

import dart.core.DartList;

/**
 * A raw triangle mesh handed to {@code Canvas.drawVertices} — dart:ui's
 * {@code Vertices}. The 2D-transformations demo builds one per hexagon of its
 * board.
 */
public class Vertices {

    private final VertexMode mode;
    private final DartList<Offset> positions;
    private DartList<Color> colors;
    private DartList<Integer> indices;
    private DartList<Offset> textureCoordinates;

    public Vertices(VertexMode mode, DartList<Offset> positions) {
        this.mode = mode;
        this.positions = positions;
    }

    public void colors(DartList<Color> v) {
        this.colors = v;
    }

    public void indices(DartList<Integer> v) {
        this.indices = v;
    }

    public void textureCoordinates(DartList<Offset> v) {
        this.textureCoordinates = v;
    }

    public VertexMode getMode() {
        return mode;
    }

    public DartList<Offset> getPositions() {
        return positions;
    }

    public DartList<Color> getColors() {
        return colors;
    }

    /** The index buffer, or null when the positions are used in order. */
    public DartList<Integer> getIndices() {
        return indices;
    }
}
