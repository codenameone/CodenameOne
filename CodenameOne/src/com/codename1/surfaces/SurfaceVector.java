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
package com.codename1.surfaces;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// A retained vector drawing node: a small catalog of fill/stroke/text operations recorded in
/// paint order and replayed natively by every platform renderer (SwiftUI `Canvas` on iOS, an
/// in-process bitmap on Android, Codename One `Graphics` on desktop). It exists for the widgets
/// the sealed template catalog cannot express -- clocks, gauges, dials and similar custom art --
/// without shipping pre-rendered images for every state.
///
/// Operations use a logical coordinate space (the *view box* passed to the constructor) that is
/// scaled to the node's laid-out bounds preserving aspect ratio and centered, so the same op list
/// renders correctly at every widget size.
///
/// #### Angles: the clock convention
///
/// All angles in this class are degrees where **0 points up (12 o'clock) and positive angles
/// advance clockwise** -- the natural convention for clock hands and gauge needles. Renderers
/// convert internally to each platform's native arc convention.
///
/// #### Rotation groups
///
/// Operations added between `beginRotation(...)` and `endRotation()` rotate together around a
/// pivot. The angle is either fixed or read from the entry state map by key, which is what makes
/// an analog clock cheap: publish the face layout once and drive the hands with per-entry state:
///
/// ```java
/// SurfaceVector face = new SurfaceVector(200, 200)
///         .fillEllipse(100, 100, 96, 96, SurfaceColor.BACKGROUND)
///         .strokeEllipse(100, 100, 96, 96, 4, SurfaceColor.LABEL)
///         .beginRotation("hourAngle", 100, 100)
///             .line(100, 100, 100, 52, 8, SurfaceColor.LABEL)
///         .endRotation()
///         .beginRotation("minuteAngle", 100, 100)
///             .line(100, 100, 100, 24, 5, SurfaceColor.ACCENT)
///         .endRotation()
///         .fillEllipse(100, 100, 6, 6, SurfaceColor.ACCENT);
/// // one timeline entry per minute; the OS flips entries on schedule without waking the app
/// WidgetTimeline t = new WidgetTimeline().setContent(face);
/// for (int m = 0; m < 60; m++) {
///     int totalMinutes = hourOfDay * 60 + minute + m;
///     Map<String, Object> state = new HashMap<String, Object>();
///     state.put("minuteAngle", Float.valueOf(totalMinutes % 60 * 6f));
///     state.put("hourAngle", Float.valueOf(totalMinutes % 720 * 0.5f));
///     t.addEntry(new Date(startOfMinute + m * 60000L), state);
/// }
/// ```
///
/// A state-driven angle does **not** tick by itself -- state is per timeline entry, so a clock
/// publishes one entry per minute (as above) and the OS flips them on its own schedule.
///
/// Descriptors cap the total operation count at 512 per vector node; exceeding it fails at
/// serialization time. Unbalanced `beginRotation`/`endRotation` pairs also fail at serialization
/// time with `IllegalStateException`.
public class SurfaceVector extends SurfaceNode {
    /// The maximum number of drawing operations (rotation groups included) in one vector node.
    static final int MAX_OPS = 512;

    private final int viewBoxWidth;
    private final int viewBoxHeight;
    private final List<Map<String, Object>> ops = new ArrayList<Map<String, Object>>();
    /// Stack of op-list targets; index 0 is the root list, deeper entries are open rotation
    /// groups. Ops append to the deepest entry.
    private final List<List<Map<String, Object>>> targets =
            new ArrayList<List<Map<String, Object>>>();
    private int opCount;

    /// Creates a vector node with a logical coordinate space. The view box is scaled to the
    /// node's laid-out bounds preserving aspect ratio (centered); it also provides the node's
    /// natural size in dips when no fixed size or weight applies.
    ///
    /// #### Parameters
    ///
    /// - `viewBoxWidth`: the logical width of the drawing coordinate space
    /// - `viewBoxHeight`: the logical height of the drawing coordinate space
    public SurfaceVector(int viewBoxWidth, int viewBoxHeight) {
        if (viewBoxWidth <= 0 || viewBoxHeight <= 0) {
            throw new IllegalArgumentException("The view box dimensions must be positive");
        }
        this.viewBoxWidth = viewBoxWidth;
        this.viewBoxHeight = viewBoxHeight;
        targets.add(ops);
    }

    /// Fills an axis-aligned rectangle.
    ///
    /// #### Parameters
    ///
    /// - `x`: left edge in view-box units
    /// - `y`: top edge in view-box units
    /// - `w`: width in view-box units
    /// - `h`: height in view-box units
    /// - `c`: the fill color
    ///
    /// #### Returns
    ///
    /// this vector node, for chaining
    public SurfaceVector fillRect(float x, float y, float w, float h, SurfaceColor c) {
        Map<String, Object> op = op("fillRect", c);
        op.put("x", Float.valueOf(x));
        op.put("y", Float.valueOf(y));
        op.put("w", Float.valueOf(w));
        op.put("h", Float.valueOf(h));
        return add(op);
    }

    /// Fills a rectangle with rounded corners.
    ///
    /// #### Parameters
    ///
    /// - `x`: left edge in view-box units
    /// - `y`: top edge in view-box units
    /// - `w`: width in view-box units
    /// - `h`: height in view-box units
    /// - `corner`: the corner radius in view-box units
    /// - `c`: the fill color
    ///
    /// #### Returns
    ///
    /// this vector node, for chaining
    public SurfaceVector fillRoundRect(float x, float y, float w, float h, float corner,
            SurfaceColor c) {
        Map<String, Object> op = op("fillRoundRect", c);
        op.put("x", Float.valueOf(x));
        op.put("y", Float.valueOf(y));
        op.put("w", Float.valueOf(w));
        op.put("h", Float.valueOf(h));
        op.put("corner", Float.valueOf(corner));
        return add(op);
    }

    /// Fills an ellipse.
    ///
    /// #### Parameters
    ///
    /// - `cx`: center x in view-box units
    /// - `cy`: center y in view-box units
    /// - `rx`: horizontal radius in view-box units
    /// - `ry`: vertical radius in view-box units
    /// - `c`: the fill color
    ///
    /// #### Returns
    ///
    /// this vector node, for chaining
    public SurfaceVector fillEllipse(float cx, float cy, float rx, float ry, SurfaceColor c) {
        Map<String, Object> op = op("fillEllipse", c);
        putEllipse(op, cx, cy, rx, ry);
        return add(op);
    }

    /// Fills a pie slice: the elliptical arc between the two angles joined to the center. Angles
    /// use the clock convention (degrees, 0 = 12 o'clock, clockwise positive).
    ///
    /// #### Parameters
    ///
    /// - `cx`: center x in view-box units
    /// - `cy`: center y in view-box units
    /// - `rx`: horizontal radius in view-box units
    /// - `ry`: vertical radius in view-box units
    /// - `startDeg`: the start angle in clock degrees
    /// - `sweepDeg`: the sweep in degrees, clockwise positive
    /// - `c`: the fill color
    ///
    /// #### Returns
    ///
    /// this vector node, for chaining
    public SurfaceVector fillArc(float cx, float cy, float rx, float ry, float startDeg,
            float sweepDeg, SurfaceColor c) {
        Map<String, Object> op = op("fillArc", c);
        putEllipse(op, cx, cy, rx, ry);
        op.put("start", Float.valueOf(startDeg));
        op.put("sweep", Float.valueOf(sweepDeg));
        return add(op);
    }

    /// Strokes the outline of an ellipse.
    ///
    /// #### Parameters
    ///
    /// - `cx`: center x in view-box units
    /// - `cy`: center y in view-box units
    /// - `rx`: horizontal radius in view-box units
    /// - `ry`: vertical radius in view-box units
    /// - `strokeWidth`: the stroke width in view-box units
    /// - `c`: the stroke color
    ///
    /// #### Returns
    ///
    /// this vector node, for chaining
    public SurfaceVector strokeEllipse(float cx, float cy, float rx, float ry, float strokeWidth,
            SurfaceColor c) {
        Map<String, Object> op = op("strokeEllipse", c);
        putEllipse(op, cx, cy, rx, ry);
        op.put("sw", Float.valueOf(strokeWidth));
        return add(op);
    }

    /// Strokes an open elliptical arc with round caps -- the primitive for gauge tracks and
    /// progress rings. Angles use the clock convention (degrees, 0 = 12 o'clock, clockwise
    /// positive).
    ///
    /// #### Parameters
    ///
    /// - `cx`: center x in view-box units
    /// - `cy`: center y in view-box units
    /// - `rx`: horizontal radius in view-box units
    /// - `ry`: vertical radius in view-box units
    /// - `startDeg`: the start angle in clock degrees
    /// - `sweepDeg`: the sweep in degrees, clockwise positive
    /// - `strokeWidth`: the stroke width in view-box units
    /// - `c`: the stroke color
    ///
    /// #### Returns
    ///
    /// this vector node, for chaining
    public SurfaceVector strokeArc(float cx, float cy, float rx, float ry, float startDeg,
            float sweepDeg, float strokeWidth, SurfaceColor c) {
        Map<String, Object> op = op("strokeArc", c);
        putEllipse(op, cx, cy, rx, ry);
        op.put("start", Float.valueOf(startDeg));
        op.put("sweep", Float.valueOf(sweepDeg));
        op.put("sw", Float.valueOf(strokeWidth));
        return add(op);
    }

    /// Draws a line with round caps.
    ///
    /// #### Parameters
    ///
    /// - `x1`: start x in view-box units
    /// - `y1`: start y in view-box units
    /// - `x2`: end x in view-box units
    /// - `y2`: end y in view-box units
    /// - `strokeWidth`: the stroke width in view-box units
    /// - `c`: the stroke color
    ///
    /// #### Returns
    ///
    /// this vector node, for chaining
    public SurfaceVector line(float x1, float y1, float x2, float y2, float strokeWidth,
            SurfaceColor c) {
        Map<String, Object> op = op("line", c);
        op.put("x1", Float.valueOf(x1));
        op.put("y1", Float.valueOf(y1));
        op.put("x2", Float.valueOf(x2));
        op.put("y2", Float.valueOf(y2));
        op.put("sw", Float.valueOf(strokeWidth));
        return add(op);
    }

    /// Fills a polygon built from x,y coordinate pairs. The path is implicitly closed for
    /// filling regardless of `close`.
    ///
    /// #### Parameters
    ///
    /// - `xy`: coordinate pairs `x0, y0, x1, y1, ...` in view-box units, at least three points
    /// - `close`: whether the serialized path is marked closed (kept for renderer symmetry with
    ///   `strokePath`)
    /// - `c`: the fill color
    ///
    /// #### Returns
    ///
    /// this vector node, for chaining
    public SurfaceVector fillPath(float[] xy, boolean close, SurfaceColor c) {
        Map<String, Object> op = op("fillPath", c);
        op.put("pts", copyPoints(xy));
        op.put("close", Boolean.valueOf(close));
        return add(op);
    }

    /// Strokes a poly-line built from x,y coordinate pairs, with round caps and joins.
    ///
    /// #### Parameters
    ///
    /// - `xy`: coordinate pairs `x0, y0, x1, y1, ...` in view-box units, at least two points
    /// - `close`: whether the last point connects back to the first
    /// - `strokeWidth`: the stroke width in view-box units
    /// - `c`: the stroke color
    ///
    /// #### Returns
    ///
    /// this vector node, for chaining
    public SurfaceVector strokePath(float[] xy, boolean close, float strokeWidth, SurfaceColor c) {
        Map<String, Object> op = op("strokePath", c);
        op.put("pts", copyPoints(xy));
        op.put("close", Boolean.valueOf(close));
        op.put("sw", Float.valueOf(strokeWidth));
        return add(op);
    }

    /// Draws text anchored at the middle of `x` with its baseline at `y`. The text supports
    /// `${key}` interpolation from the entry state map, like `SurfaceText`.
    ///
    /// #### Parameters
    ///
    /// - `text`: the text, may embed `${key}` placeholders
    /// - `x`: the horizontal anchor (text centers on it) in view-box units
    /// - `y`: the text baseline in view-box units
    /// - `fontSize`: the font size in view-box units
    /// - `w`: the font weight
    /// - `c`: the text color
    ///
    /// #### Returns
    ///
    /// this vector node, for chaining
    public SurfaceVector text(String text, float x, float y, float fontSize, SurfaceFontWeight w,
            SurfaceColor c) {
        if (text == null) {
            throw new IllegalArgumentException("Vector text must not be null");
        }
        Map<String, Object> op = op("text", c);
        op.put("text", text);
        op.put("x", Float.valueOf(x));
        op.put("y", Float.valueOf(y));
        op.put("size", Float.valueOf(fontSize));
        op.put("fw", w == null ? SurfaceFontWeight.REGULAR.getJsonName() : w.getJsonName());
        return add(op);
    }

    /// Opens a rotation group with a fixed angle: every op added until the matching
    /// `endRotation()` rotates by `degrees` (clock convention: clockwise positive) around the
    /// pivot. Groups nest.
    ///
    /// #### Parameters
    ///
    /// - `degrees`: the rotation in degrees, clockwise positive
    /// - `pivotX`: the pivot x in view-box units
    /// - `pivotY`: the pivot y in view-box units
    ///
    /// #### Returns
    ///
    /// this vector node, for chaining
    public SurfaceVector beginRotation(float degrees, float pivotX, float pivotY) {
        Map<String, Object> op = rotationOp(pivotX, pivotY);
        op.put("deg", Float.valueOf(degrees));
        return openRotation(op);
    }

    /// Opens a rotation group whose angle is read from the entry state map: the state value is a
    /// `Number` in degrees, clockwise positive. This is how clock hands and gauge needles animate
    /// -- a per-entry timeline updates the angle without republishing the layout.
    ///
    /// #### Parameters
    ///
    /// - `degreesStateKey`: the state-map key holding the angle in degrees
    /// - `pivotX`: the pivot x in view-box units
    /// - `pivotY`: the pivot y in view-box units
    ///
    /// #### Returns
    ///
    /// this vector node, for chaining
    public SurfaceVector beginRotation(String degreesStateKey, float pivotX, float pivotY) {
        if (degreesStateKey == null || degreesStateKey.length() == 0) {
            throw new IllegalArgumentException("The rotation state key must not be empty");
        }
        Map<String, Object> op = rotationOp(pivotX, pivotY);
        op.put("degKey", degreesStateKey);
        return openRotation(op);
    }

    /// Closes the most recently opened rotation group.
    ///
    /// #### Returns
    ///
    /// this vector node, for chaining
    public SurfaceVector endRotation() {
        if (targets.size() == 1) {
            throw new IllegalStateException("endRotation() without a matching beginRotation(); "
                    + "rotation groups must be balanced");
        }
        targets.remove(targets.size() - 1);
        return this;
    }

    /// Returns the logical view-box width.
    public int getViewBoxWidth() {
        return viewBoxWidth;
    }

    /// Returns the logical view-box height.
    public int getViewBoxHeight() {
        return viewBoxHeight;
    }

    /// Returns the number of recorded operations, rotation groups included.
    public int getOpCount() {
        return opCount;
    }

    @Override
    public SurfaceVector setPadding(int all) {
        super.setPadding(all);
        return this;
    }

    @Override
    public SurfaceVector setPadding(int top, int right, int bottom, int left) {
        super.setPadding(top, right, bottom, left);
        return this;
    }

    @Override
    public SurfaceVector setBackground(SurfaceColor background) {
        super.setBackground(background);
        return this;
    }

    @Override
    public SurfaceVector setCornerRadius(int radius) {
        super.setCornerRadius(radius);
        return this;
    }

    @Override
    public SurfaceVector setAlignment(SurfaceAlignment alignment) {
        super.setAlignment(alignment);
        return this;
    }

    @Override
    public SurfaceVector setWeight(int weight) {
        super.setWeight(weight);
        return this;
    }

    @Override
    public SurfaceVector setSize(int widthDips, int heightDips) {
        super.setSize(widthDips, heightDips);
        return this;
    }

    @Override
    public SurfaceVector setAction(String actionId) {
        super.setAction(actionId);
        return this;
    }

    @Override
    public SurfaceVector setAction(String actionId, Map<String, Object> params) {
        super.setAction(actionId, params);
        return this;
    }

    @Override
    String getType() {
        return "vec";
    }

    @Override
    void serializeContent(Map<String, Object> out, Map<String, byte[]> images, int depth) {
        if (targets.size() != 1) {
            throw new IllegalStateException("A SurfaceVector has " + (targets.size() - 1)
                    + " unclosed rotation group(s); every beginRotation() needs a matching "
                    + "endRotation() before the descriptor serializes");
        }
        if (opCount > MAX_OPS) {
            throw new IllegalArgumentException("A SurfaceVector is limited to " + MAX_OPS
                    + " drawing operations so widget renderers stay within their budgets; this "
                    + "node has " + opCount);
        }
        out.put("vw", Integer.valueOf(viewBoxWidth));
        out.put("vh", Integer.valueOf(viewBoxHeight));
        out.put("ops", serializeOps(ops));
    }

    // --- internals --------------------------------------------------------------

    private static List<Object> serializeOps(List<Map<String, Object>> src) {
        List<Object> out = new ArrayList<Object>(src.size());
        for (Map<String, Object> op : src) {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            for (Map.Entry<String, Object> e : op.entrySet()) {
                Object v = e.getValue();
                if (v instanceof Float) {
                    m.put(e.getKey(), Double.valueOf(((Float) v).doubleValue()));
                } else if (v instanceof SurfaceColor) {
                    m.put(e.getKey(), SurfaceSerializer.colorMap((SurfaceColor) v));
                } else if (v instanceof float[]) {
                    float[] pts = (float[]) v;
                    List<Object> list = new ArrayList<Object>(pts.length);
                    for (float pt : pts) {
                        list.add(Double.valueOf(pt));
                    }
                    m.put(e.getKey(), list);
                } else if (v instanceof List) {
                    // the nested op list of a rotation group
                    m.put(e.getKey(), serializeOps(castOps(v)));
                } else {
                    m.put(e.getKey(), v);
                }
            }
            out.add(m);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castOps(Object v) {
        return (List<Map<String, Object>>) v;
    }

    private static Map<String, Object> op(String name, SurfaceColor c) {
        if (c == null) {
            throw new IllegalArgumentException("Every vector op needs a color");
        }
        Map<String, Object> op = new LinkedHashMap<String, Object>();
        op.put("o", name);
        op.put("c", c);
        return op;
    }

    private static void putEllipse(Map<String, Object> op, float cx, float cy, float rx,
            float ry) {
        op.put("cx", Float.valueOf(cx));
        op.put("cy", Float.valueOf(cy));
        op.put("rx", Float.valueOf(rx));
        op.put("ry", Float.valueOf(ry));
    }

    private static float[] copyPoints(float[] xy) {
        if (xy == null || xy.length < 4 || xy.length % 2 != 0) {
            throw new IllegalArgumentException("Path points must be x,y pairs with at least "
                    + "two points");
        }
        float[] copy = new float[xy.length];
        System.arraycopy(xy, 0, copy, 0, xy.length);
        return copy;
    }

    private static Map<String, Object> rotationOp(float pivotX, float pivotY) {
        Map<String, Object> op = new LinkedHashMap<String, Object>();
        op.put("o", "rot");
        op.put("px", Float.valueOf(pivotX));
        op.put("py", Float.valueOf(pivotY));
        return op;
    }

    private SurfaceVector openRotation(Map<String, Object> op) {
        List<Map<String, Object>> nested = new ArrayList<Map<String, Object>>();
        op.put("ops", nested);
        add(op);
        targets.add(nested);
        return this;
    }

    private SurfaceVector add(Map<String, Object> op) {
        targets.get(targets.size() - 1).add(op);
        opCount++;
        return this;
    }
}
