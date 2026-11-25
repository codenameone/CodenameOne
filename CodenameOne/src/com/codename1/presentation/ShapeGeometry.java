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
package com.codename1.presentation;

import com.codename1.ui.geom.GeneralPath;
import com.codename1.util.MathUtil;

/// The outlines of the preset shapes, as `com.codename1.ui.geom.GeneralPath`s.
///
/// Names are OOXML's preset geometry names (ECMA-376 part 1, 20.1.10.56), which every reader
/// maps its own shapes to. The same path fills and strokes on the screen and in PDF export,
/// so a shape looks identical on both. Unknown names draw as a rectangle.
public final class ShapeGeometry {
    private static final String[] PRESETS = {"rect", "roundRect", "ellipse", "triangle",
        "rtTriangle", "diamond", "parallelogram", "trapezoid", "pentagon", "hexagon", "octagon",
        "star4", "star5", "star6", "rightArrow", "leftArrow", "upArrow", "downArrow",
        "leftRightArrow", "chevron", "homePlate", "plus", "can", "heart", "foldedCorner",
        "flowChartProcess", "flowChartDecision", "flowChartTerminator", "flowChartDocument",
        "flowChartPredefinedProcess", "wedgeRectCallout", "wedgeRoundRectCallout", "cloud",
        "donut", "line", "straightConnector1", "bentConnector3", "curvedConnector3"};

    private ShapeGeometry() {
    }

    /// The preset names drawn exactly; any other name is drawn as a rectangle.
    ///
    /// #### Returns
    ///
    /// a copy of the names
    public static String[] presets() {
        String[] c = new String[PRESETS.length];
        System.arraycopy(PRESETS, 0, c, 0, c.length);
        return c;
    }

    /// True when the name is one of the presets drawn exactly.
    ///
    /// #### Parameters
    ///
    /// - `name`: a preset name
    ///
    /// #### Returns
    ///
    /// true when known
    public static boolean isKnown(String name) {
        for (String p : PRESETS) {
            if (p.equals(name)) {
                return true;
            }
        }
        return false;
    }

    /// True for the line and connector presets, which are stroked and never filled.
    ///
    /// #### Parameters
    ///
    /// - `name`: a preset name
    ///
    /// #### Returns
    ///
    /// true for a line
    public static boolean isLine(String name) {
        return "line".equals(name) || name != null && name.endsWith("Connector1")
                || name != null && (name.startsWith("bentConnector")
                || name.startsWith("curvedConnector"));
    }

    /// The outline of a shape in its own box, from (0, 0) to (`w`, `h`), before rotation and
    /// flipping.
    ///
    /// #### Parameters
    ///
    /// - `shape`: the shape
    ///
    /// - `w`: the box width in any unit
    ///
    /// - `h`: the box height in the same unit
    ///
    /// #### Returns
    ///
    /// the path
    public static GeneralPath path(ShapeElement shape, float w, float h) {
        return path(shape.geometry, w, h, shape);
    }

    /// The outline of a preset with its default adjustments.
    ///
    /// #### Parameters
    ///
    /// - `name`: the preset name
    ///
    /// - `w`: the box width
    ///
    /// - `h`: the box height
    ///
    /// #### Returns
    ///
    /// the path
    public static GeneralPath path(String name, float w, float h) {
        return path(name, w, h, null);
    }

    private static float adj(ShapeElement s, int i, float fallback) {
        float v = s == null ? fallback : s.adjust(i, fallback);
        // whatever a file says, a path stays finite: no preset reads an adjustment as more
        // than twice the size it scales
        return Float.isNaN(v) || Float.isInfinite(v) ? fallback : Math.max(0, Math.min(2, v));
    }

    private static GeneralPath path(String name, float w, float h, ShapeElement s) {
        GeneralPath p = new GeneralPath();
        float m = Math.min(w, h);
        String n = name == null ? "rect" : name;
        if ("roundRect".equals(n) || "wedgeRoundRectCallout".equals(n)) {
            float r = Math.min(m / 2, m * adj(s, 0, 0.1667f));
            roundRect(p, 0, 0, w, h, r);
            if ("wedgeRoundRectCallout".equals(n)) {
                callout(p, w, h);
            }
        } else if ("ellipse".equals(n) || "cloud".equals(n)) {
            p.arc(0, 0, w, h, 0, Math.PI * 2);
            p.closePath();
        } else if ("donut".equals(n)) {
            p.setWindingRule(GeneralPath.WIND_EVEN_ODD);
            p.arc(0, 0, w, h, 0, Math.PI * 2);
            p.closePath();
            float d = m * adj(s, 0, 0.25f);
            p.arc(d, d, w - 2 * d, h - 2 * d, 0, Math.PI * 2);
            p.closePath();
        } else if ("triangle".equals(n)) {
            poly(p, w * adj(s, 0, 0.5f), 0, w, h, 0, h);
        } else if ("rtTriangle".equals(n)) {
            poly(p, 0, 0, w, h, 0, h);
        } else if ("diamond".equals(n) || "flowChartDecision".equals(n)) {
            poly(p, w / 2, 0, w, h / 2, w / 2, h, 0, h / 2);
        } else if ("parallelogram".equals(n)) {
            float o = m * adj(s, 0, 0.25f);
            poly(p, o, 0, w, 0, w - o, h, 0, h);
        } else if ("trapezoid".equals(n)) {
            float o = m * adj(s, 0, 0.25f);
            poly(p, o, 0, w - o, 0, w, h, 0, h);
        } else if ("pentagon".equals(n)) {
            regular(p, w, h, 5, 0);
        } else if ("hexagon".equals(n)) {
            float o = m * adj(s, 0, 0.25f);
            poly(p, o, 0, w - o, 0, w, h / 2, w - o, h, o, h, 0, h / 2);
        } else if ("octagon".equals(n)) {
            float o = m * adj(s, 0, 0.2929f);
            poly(p, o, 0, w - o, 0, w, o, w, h - o, w - o, h, o, h, 0, h - o, 0, o);
        } else if ("star4".equals(n)) {
            star(p, w, h, 4, adj(s, 0, 0.25f));
        } else if ("star5".equals(n)) {
            star(p, w, h, 5, adj(s, 0, 0.382f));
        } else if ("star6".equals(n)) {
            star(p, w, h, 6, adj(s, 0, 0.5f));
        } else if ("rightArrow".equals(n)) {
            float t = h * adj(s, 0, 0.5f) / 2;
            float head = Math.min(w, m * adj(s, 1, 0.5f));
            poly(p, 0, h / 2 - t, w - head, h / 2 - t, w - head, 0, w, h / 2, w - head, h,
                    w - head, h / 2 + t, 0, h / 2 + t);
        } else if ("leftArrow".equals(n)) {
            float t = h * adj(s, 0, 0.5f) / 2;
            float head = Math.min(w, m * adj(s, 1, 0.5f));
            poly(p, w, h / 2 - t, head, h / 2 - t, head, 0, 0, h / 2, head, h, head, h / 2 + t,
                    w, h / 2 + t);
        } else if ("upArrow".equals(n)) {
            float t = w * adj(s, 0, 0.5f) / 2;
            float head = Math.min(h, m * adj(s, 1, 0.5f));
            poly(p, w / 2 - t, h, w / 2 - t, head, 0, head, w / 2, 0, w, head, w / 2 + t, head,
                    w / 2 + t, h);
        } else if ("downArrow".equals(n)) {
            float t = w * adj(s, 0, 0.5f) / 2;
            float head = Math.min(h, m * adj(s, 1, 0.5f));
            poly(p, w / 2 - t, 0, w / 2 + t, 0, w / 2 + t, h - head, w, h - head, w / 2, h, 0,
                    h - head, w / 2 - t, h - head);
        } else if ("leftRightArrow".equals(n)) {
            float t = h * adj(s, 0, 0.5f) / 2;
            float head = Math.min(w / 2, m * adj(s, 1, 0.5f));
            poly(p, 0, h / 2, head, 0, head, h / 2 - t, w - head, h / 2 - t, w - head, 0, w,
                    h / 2, w - head, h, w - head, h / 2 + t, head, h / 2 + t, head, h);
        } else if ("chevron".equals(n)) {
            float o = Math.min(w, m * adj(s, 0, 0.5f));
            poly(p, 0, 0, w - o, 0, w, h / 2, w - o, h, 0, h, o, h / 2);
        } else if ("homePlate".equals(n)) {
            float o = Math.min(w, m * adj(s, 0, 0.5f));
            poly(p, 0, 0, w - o, 0, w, h / 2, w - o, h, 0, h);
        } else if ("plus".equals(n)) {
            float o = m * adj(s, 0, 0.25f);
            poly(p, o, 0, w - o, 0, w - o, o, w, o, w, h - o, w - o, h - o, w - o, h, o, h, o,
                    h - o, 0, h - o, 0, o, o, o);
        } else if ("can".equals(n)) {
            float e = Math.min(h / 2, m * adj(s, 0, 0.25f));
            p.moveTo(0, e / 2);
            p.arc(0, 0, w, e, Math.PI, -Math.PI, true);
            p.lineTo(w, h - e / 2);
            p.arc(0, h - e, w, e, 0, -Math.PI, true);
            p.closePath();
            p.moveTo(0, e / 2);
            p.arc(0, 0, w, e, Math.PI, Math.PI, true);
        } else if ("heart".equals(n)) {
            p.moveTo(w / 2, h * 0.25f);
            p.curveTo(w / 2, 0, 0, 0, 0, h * 0.3f);
            p.curveTo(0, h * 0.6f, w / 2, h * 0.75f, w / 2, h);
            p.curveTo(w / 2, h * 0.75f, w, h * 0.6f, w, h * 0.3f);
            p.curveTo(w, 0, w / 2, 0, w / 2, h * 0.25f);
            p.closePath();
        } else if ("foldedCorner".equals(n)) {
            float f = m * adj(s, 0, 0.1667f);
            poly(p, 0, 0, w, 0, w, h - f, w - f, h, 0, h);
            p.moveTo(w, h - f);
            p.lineTo(w - f, h - f * 0.8f);
            p.lineTo(w - f, h);
        } else if ("flowChartTerminator".equals(n)) {
            roundRect(p, 0, 0, w, h, h / 2);
        } else if ("flowChartDocument".equals(n)) {
            p.moveTo(0, 0);
            p.lineTo(w, 0);
            p.lineTo(w, h * 0.83f);
            p.curveTo(w * 0.75f, h * 0.7f, w * 0.5f, h * 1.05f, 0, h * 0.9f);
            p.closePath();
        } else if ("flowChartPredefinedProcess".equals(n)) {
            poly(p, 0, 0, w, 0, w, h, 0, h);
            float i = w / 8;
            p.moveTo(i, 0);
            p.lineTo(i, h);
            p.moveTo(w - i, 0);
            p.lineTo(w - i, h);
        } else if ("wedgeRectCallout".equals(n)) {
            poly(p, 0, 0, w, 0, w, h, 0, h);
            callout(p, w, h);
        } else if ("bentConnector3".equals(n)) {
            p.moveTo(0, 0);
            p.lineTo(w / 2, 0);
            p.lineTo(w / 2, h);
            p.lineTo(w, h);
        } else if ("curvedConnector3".equals(n)) {
            p.moveTo(0, 0);
            p.curveTo(w / 2, 0, w / 2, h, w, h);
        } else if (isLine(n)) {
            p.moveTo(0, 0);
            p.lineTo(w, h);
        } else {
            poly(p, 0, 0, w, 0, w, h, 0, h);
        }
        return p;
    }

    private static void callout(GeneralPath p, float w, float h) {
        p.moveTo(w * 0.2f, h);
        p.lineTo(w * 0.1f, h * 1.3f);
        p.lineTo(w * 0.4f, h);
        p.closePath();
    }

    private static void roundRect(GeneralPath p, float x, float y, float w, float h, float r) {
        if (r <= 0) {
            poly(p, x, y, x + w, y, x + w, y + h, x, y + h);
            return;
        }
        float k = r * 0.5523f;
        p.moveTo(x + r, y);
        p.lineTo(x + w - r, y);
        p.curveTo(x + w - r + k, y, x + w, y + r - k, x + w, y + r);
        p.lineTo(x + w, y + h - r);
        p.curveTo(x + w, y + h - r + k, x + w - r + k, y + h, x + w - r, y + h);
        p.lineTo(x + r, y + h);
        p.curveTo(x + r - k, y + h, x, y + h - r + k, x, y + h - r);
        p.lineTo(x, y + r);
        p.curveTo(x, y + r - k, x + r - k, y, x + r, y);
        p.closePath();
    }

    private static void poly(GeneralPath p, float... xy) {
        p.moveTo(xy[0], xy[1]);
        for (int i = 2; i + 1 < xy.length; i += 2) {
            p.lineTo(xy[i], xy[i + 1]);
        }
        p.closePath();
    }

    private static void regular(GeneralPath p, float w, float h, int sides, double start) {
        for (int i = 0; i < sides; i++) {
            double a = -Math.PI / 2 + start + 2 * Math.PI * i / sides;
            float px = (float) (w / 2 + w / 2 * Math.cos(a));
            float py = (float) (h / 2 + h / 2 * Math.sin(a));
            if (i == 0) {
                p.moveTo(px, py);
            } else {
                p.lineTo(px, py);
            }
        }
        p.closePath();
    }

    private static void star(GeneralPath p, float w, float h, int points, float inner) {
        for (int i = 0; i < points * 2; i++) {
            double a = -Math.PI / 2 + Math.PI * i / points;
            float r = i % 2 == 0 ? 1 : Math.max(0.05f, Math.min(1, inner));
            float px = (float) (w / 2 + w / 2 * r * Math.cos(a));
            float py = (float) (h / 2 + h / 2 * r * Math.sin(a));
            if (i == 0) {
                p.moveTo(px, py);
            } else {
                p.lineTo(px, py);
            }
        }
        p.closePath();
    }

    /// The outline of an arrowhead at the end of a line, for drawing `LineStyle` heads and
    /// tails.
    ///
    /// #### Parameters
    ///
    /// - `kind`: a `LineStyle.ARROW_` constant
    ///
    /// - `tipX`: the tip
    ///
    /// - `tipY`: the tip
    ///
    /// - `fromX`: a point the line comes from
    ///
    /// - `fromY`: a point the line comes from
    ///
    /// - `size`: the head's length
    ///
    /// #### Returns
    ///
    /// the path, or null for no head
    public static GeneralPath arrowHead(String kind, float tipX, float tipY, float fromX,
            float fromY, float size) {
        if (kind == null || LineStyle.ARROW_NONE.equals(kind)) {
            return null;
        }
        double a = MathUtil.atan2(tipY - fromY, tipX - fromX);
        GeneralPath p = new GeneralPath();
        if (LineStyle.ARROW_OVAL.equals(kind)) {
            p.arc(tipX - size / 2, tipY - size / 2, size, size, 0, Math.PI * 2);
            p.closePath();
            return p;
        }
        double spread = 0.45;
        float lx = (float) (tipX - size * Math.cos(a - spread));
        float ly = (float) (tipY - size * Math.sin(a - spread));
        float rx = (float) (tipX - size * Math.cos(a + spread));
        float ry = (float) (tipY - size * Math.sin(a + spread));
        p.moveTo(lx, ly);
        p.lineTo(tipX, tipY);
        p.lineTo(rx, ry);
        if (LineStyle.ARROW_TRIANGLE.equals(kind)) {
            p.closePath();
        }
        return p;
    }
}
