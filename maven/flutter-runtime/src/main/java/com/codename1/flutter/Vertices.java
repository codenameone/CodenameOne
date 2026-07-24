package com.codename1.flutter;

import dart.core.DartList;

/**
 * A raw triangle mesh handed to {@code Canvas.drawVertices} — dart:ui's
 * {@code Vertices}. The 2D-transformations demo builds one per hexagon of its
 * board. Holds the geometry structurally; actual mesh rasterization is a later
 * rendering milestone.
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
}
