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
package com.codename1.ui.graph;

import com.codename1.graph.GraphShapes;
import com.codename1.ui.geom.GeneralPath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The outlines behind the names of `GraphShapes`, and the place to add
/// outlines of your own.
///
/// The registry is shared by every graph of the application, and like the
/// components that read it, it belongs to the event dispatch thread.
public final class GraphShapeRegistry {
    private static final float KAPPA = 0.5523f;
    private static final int KEPT_PER_SHAPE = 12;
    private static final Map<String, GraphShape> SHAPES = new HashMap<String, GraphShape>();
    // per shape name, the outlines built so far at the origin, each with its size
    private static final Map<String, List<Sized>> PATHS = new HashMap<String, List<Sized>>();

    static {
        SHAPES.put(GraphShapes.RECT, new Builtin(0));
        SHAPES.put(GraphShapes.ROUNDED, new Builtin(1));
        SHAPES.put(GraphShapes.TERMINATOR, new Builtin(2));
        SHAPES.put(GraphShapes.DECISION, new Builtin(3));
        SHAPES.put(GraphShapes.DATA, new Builtin(4));
        SHAPES.put(GraphShapes.DOCUMENT, new Builtin(5));
        SHAPES.put(GraphShapes.PREPARATION, new Builtin(6));
        SHAPES.put(GraphShapes.DATABASE, new Builtin(7));
        SHAPES.put(GraphShapes.SUBPROCESS, new Builtin(8));
        SHAPES.put(GraphShapes.ELLIPSE, new Builtin(9));
        SHAPES.put(GraphShapes.NOTE, new Builtin(10));
    }

    private GraphShapeRegistry() {
    }

    /// Adds an outline under a name, or replaces the one that had it.
    public static void register(String name, GraphShape shape) {
        if (name == null) {
            return;
        }
        if (shape == null) {
            SHAPES.remove(name);
        } else {
            SHAPES.put(name, shape);
        }
        PATHS.remove(name);
    }

    /// The outline registered under a name, or null. A node whose shape has
    /// no outline is drawn as the ordinary box.
    public static GraphShape get(String name) {
        return name == null ? null : SHAPES.get(name);
    }

    /// The outline of a shape for a size, at the origin; kept, as hit tests
    /// and edges ask for the same few sizes again and again.
    private static GeneralPath path(String name, float w, float h) {
        GraphShape shape = SHAPES.get(name);
        if (shape == null) {
            return null;
        }
        List<Sized> kept = PATHS.get(name);
        if (kept == null) {
            kept = new ArrayList<Sized>();
            PATHS.put(name, kept);
        }
        int n = kept.size();
        for (int i = 0; i < n; i++) {
            Sized s = kept.get(i);
            if (s.w == w && s.h == h) {
                return s.path;
            }
        }
        if (n >= KEPT_PER_SHAPE) {
            kept.remove(0);
        }
        GeneralPath p = new GeneralPath();
        shape.outline(p, 0, 0, w, h);
        kept.add(new Sized(w, h, p));
        return p;
    }

    /// True when a point lies inside a shape drawn in the given bounds. A
    /// name with no outline is its bounds.
    public static boolean contains(String name, double px, double py, double x, double y, double w, double h) {
        if (px < x || py < y || px >= x + w || py >= y + h) {
            return false;
        }
        GeneralPath p = name == null ? null : path(name, (float) w, (float) h);
        return p == null || p.contains((float) (px - x), (float) (py - y));
    }

    /// Pulls a point on the bounds of a shape back along the line to the
    /// centre until it lies on the outline, so an edge ends on the diamond
    /// and not on the box around it.
    static void pullToOutline(String name, double[] rect, double[] ox, double[] oy, int i) {
        GeneralPath p = name == null ? null : path(name, (float) rect[2], (float) rect[3]);
        if (p == null) {
            return;
        }
        double cx = rect[2] / 2;
        double cy = rect[3] / 2;
        if (!p.contains((float) cx, (float) cy)) {
            return;
        }
        double tx = ox[i] - rect[0];
        double ty = oy[i] - rect[1];
        double in = 0;
        double out = 1;
        for (int k = 0; k < 14; k++) {
            double mid = (in + out) / 2;
            if (p.contains((float) (cx + (tx - cx) * mid), (float) (cy + (ty - cy) * mid))) {
                in = mid;
            } else {
                out = mid;
            }
        }
        ox[i] = rect[0] + cx + (tx - cx) * out;
        oy[i] = rect[1] + cy + (ty - cy) * out;
    }

    private static final class Sized {
        final float w;
        final float h;
        final GeneralPath path;

        Sized(float w, float h, GeneralPath path) {
            this.w = w;
            this.h = h;
            this.path = path;
        }
    }

    private static void roundRect(GeneralPath p, float x, float y, float w, float h, float r) {
        float k = r * (1 - KAPPA);
        p.moveTo(x + r, y);
        p.lineTo(x + w - r, y);
        p.curveTo(x + w - k, y, x + w, y + k, x + w, y + r);
        p.lineTo(x + w, y + h - r);
        p.curveTo(x + w, y + h - k, x + w - k, y + h, x + w - r, y + h);
        p.lineTo(x + r, y + h);
        p.curveTo(x + k, y + h, x, y + h - k, x, y + h - r);
        p.lineTo(x, y + r);
        p.curveTo(x, y + k, x + k, y, x + r, y);
        p.closePath();
    }

    /// The symbols of a flow chart.
    private static final class Builtin implements GraphShape {
        private final int kind;

        Builtin(int kind) {
            this.kind = kind;
        }

        @Override
        public void outline(GeneralPath p, float x, float y, float w, float h) {
            switch (kind) {
                case 1:
                    roundRect(p, x, y, w, h, Math.min(w, h) * 0.18f);
                    break;
                case 2:
                    roundRect(p, x, y, w, h, Math.min(w, h) / 2);
                    break;
                case 3:
                    p.moveTo(x + w / 2, y);
                    p.lineTo(x + w, y + h / 2);
                    p.lineTo(x + w / 2, y + h);
                    p.lineTo(x, y + h / 2);
                    p.closePath();
                    break;
                case 4: {
                    float skew = Math.min(w * 0.2f, h * 0.5f);
                    p.moveTo(x + skew, y);
                    p.lineTo(x + w, y);
                    p.lineTo(x + w - skew, y + h);
                    p.lineTo(x, y + h);
                    p.closePath();
                    break;
                }
                case 5: {
                    float base = y + h * 0.86f;
                    p.moveTo(x, y);
                    p.lineTo(x + w, y);
                    p.lineTo(x + w, base);
                    p.quadTo(x + w * 0.75f, y + h * 0.70f, x + w * 0.5f, base);
                    p.quadTo(x + w * 0.25f, y + h * 1.02f, x, base);
                    p.closePath();
                    break;
                }
                case 6: {
                    float inset = Math.min(w * 0.18f, h / 2);
                    p.moveTo(x + inset, y);
                    p.lineTo(x + w - inset, y);
                    p.lineTo(x + w, y + h / 2);
                    p.lineTo(x + w - inset, y + h);
                    p.lineTo(x + inset, y + h);
                    p.lineTo(x, y + h / 2);
                    p.closePath();
                    break;
                }
                case 7: {
                    float ry = Math.min(h * 0.14f, w * 0.25f);
                    float rx = w / 2;
                    float top = y + ry;
                    float bottom = y + h - ry;
                    p.moveTo(x, top);
                    p.curveTo(x, top - ry * KAPPA, x + rx - rx * KAPPA, y, x + rx, y);
                    p.curveTo(x + rx + rx * KAPPA, y, x + w, top - ry * KAPPA, x + w, top);
                    p.lineTo(x + w, bottom);
                    p.curveTo(x + w, bottom + ry * KAPPA, x + rx + rx * KAPPA, y + h, x + rx, y + h);
                    p.curveTo(x + rx - rx * KAPPA, y + h, x, bottom + ry * KAPPA, x, bottom);
                    p.closePath();
                    break;
                }
                case 9: {
                    float rx = w / 2;
                    float ry = h / 2;
                    float cx = x + rx;
                    float cy = y + ry;
                    p.moveTo(x, cy);
                    p.curveTo(x, cy - ry * KAPPA, cx - rx * KAPPA, y, cx, y);
                    p.curveTo(cx + rx * KAPPA, y, x + w, cy - ry * KAPPA, x + w, cy);
                    p.curveTo(x + w, cy + ry * KAPPA, cx + rx * KAPPA, y + h, cx, y + h);
                    p.curveTo(cx - rx * KAPPA, y + h, x, cy + ry * KAPPA, x, cy);
                    p.closePath();
                    break;
                }
                case 10: {
                    float fold = Math.min(w, h) * 0.25f;
                    p.moveTo(x, y);
                    p.lineTo(x + w - fold, y);
                    p.lineTo(x + w, y + fold);
                    p.lineTo(x + w, y + h);
                    p.lineTo(x, y + h);
                    p.closePath();
                    break;
                }
                default:
                    p.moveTo(x, y);
                    p.lineTo(x + w, y);
                    p.lineTo(x + w, y + h);
                    p.lineTo(x, y + h);
                    p.closePath();
                    break;
            }
        }

        @Override
        public void detail(GeneralPath p, float x, float y, float w, float h) {
            if (kind == 7) {
                float ry = Math.min(h * 0.14f, w * 0.25f);
                float rx = w / 2;
                float top = y + ry;
                p.moveTo(x, top);
                p.curveTo(x, top + ry * KAPPA, x + rx - rx * KAPPA, top + ry, x + rx, top + ry);
                p.curveTo(x + rx + rx * KAPPA, top + ry, x + w, top + ry * KAPPA, x + w, top);
            } else if (kind == 8) {
                float inset = Math.min(w * 0.1f, h * 0.25f);
                p.moveTo(x + inset, y);
                p.lineTo(x + inset, y + h);
                p.moveTo(x + w - inset, y);
                p.lineTo(x + w - inset, y + h);
            } else if (kind == 10) {
                float fold = Math.min(w, h) * 0.25f;
                p.moveTo(x + w - fold, y);
                p.lineTo(x + w - fold, y + fold);
                p.lineTo(x + w, y + fold);
            }
        }
    }
}
