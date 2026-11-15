/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.rbx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/// Computes `AbsolutePosition` and `AbsoluteSize` of every 2D GUI object
/// under the local player's `PlayerGui`, honouring the list and grid
/// layouts, padding and size constraints. The renderer reads the results.
public final class GuiLayout {
    /// The top bar inset a ScreenGui keeps clear unless it ignores it.
    public static final double TOP_INSET = 36;

    private GuiLayout() {
    }

    static void layout(Engine e) {
        Instance lp = e.getLocalPlayer();
        if (lp == null) {
            return;
        }
        Instance pg = lp.findFirstChildOfClass("PlayerGui", false);
        if (pg == null) {
            return;
        }
        Vector2 vp = viewport(e);
        List<Instance> guis = pg.getChildren();
        for (int i = 0; i < guis.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            Instance g = guis.get(i);
            if (g.isA("ScreenGui")) {
                double top = g.bool("IgnoreGuiInset") ? 0 : TOP_INSET;
                setAbs(g, 0, top, vp.x, vp.y - top);
                layoutChildren(g, 0, top, vp.x, vp.y - top);
            }
        }
    }

    /// The camera's viewport, which the host keeps current.
    public static Vector2 viewport(Engine e) {
        Instance cam = e.getWorkspace().findFirstChildOfClass("Camera", false);
        Object v = cam == null ? null : cam.prop("ViewportSize");
        return v instanceof Vector2 ? (Vector2) v : new Vector2(800, 600);
    }

    static void setAbs(Instance g, double x, double y, double w, double h) {
        Vector2 p = new Vector2(x, y);
        Vector2 s = new Vector2(w, h);
        if (!p.equals(g.prop("AbsolutePosition"))) {
            g.put("AbsolutePosition", p);
        }
        if (!s.equals(g.prop("AbsoluteSize"))) {
            g.put("AbsoluteSize", s);
        }
    }

    static double pad(Instance p, String k, double extent) {
        Object v = p.prop(k);
        return v instanceof UDim ? ((UDim) v).scale * extent + ((UDim) v).offset : 0;
    }

    static void layoutChildren(Instance parent, double px, double py, double pw, double ph) {
        Instance padding = parent.findFirstChildOfClass("UIPadding", false);
        if (padding != null) {
            double l = pad(padding, "PaddingLeft", pw);
            double r = pad(padding, "PaddingRight", pw);
            double t = pad(padding, "PaddingTop", ph);
            double b = pad(padding, "PaddingBottom", ph);
            px += l;
            py += t;
            pw = Math.max(0, pw - l - r);
            ph = Math.max(0, ph - t - b);
        }
        if (parent.isA("ScrollingFrame")) {
            Vector2 cp = (Vector2) parent.prop("CanvasPosition");
            UDim2 cs = (UDim2) parent.prop("CanvasSize");
            px -= cp.x;
            py -= cp.y;
            pw = Math.max(pw, cs.x.scale * pw + cs.x.offset);
            ph = Math.max(ph, cs.y.scale * ph + cs.y.offset);
        }
        List<Instance> kids = new ArrayList<Instance>();
        List<Instance> all = parent.getChildren();
        Instance list = null;
        Instance grid = null;
        for (int i = 0; i < all.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            Instance c = all.get(i);
            if (c.isA("UIListLayout")) {
                list = c;
            } else if (c.isA("UIGridLayout")) {
                grid = c;
            } else if (c.isA("GuiObject")) {
                kids.add(c);
            }
        }
        if (list != null || grid != null) {
            sortByLayoutOrder(kids, list != null ? list : grid);
        }
        double cursorX = 0;
        double cursorY = 0;
        double rowH = 0;
        for (int i = 0; i < kids.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            Instance c = kids.get(i);
            double[] sz = size(c, pw, ph);
            double w = sz[0];
            double h = sz[1];
            double x;
            double y;
            if (grid != null) {
                UDim2 cell = (UDim2) grid.prop("CellSize");
                UDim2 gap = (UDim2) grid.prop("CellPadding");
                w = cell.x.scale * pw + cell.x.offset;
                h = cell.y.scale * ph + cell.y.offset;
                double gx = gap.x.scale * pw + gap.x.offset;
                double gy = gap.y.scale * ph + gap.y.offset;
                if (cursorX > 0 && cursorX + w > pw) {
                    cursorX = 0;
                    cursorY += h + gy;
                }
                x = px + cursorX;
                y = py + cursorY;
                cursorX += w + gx;
            } else if (list != null) {
                double gapPx = pad(list, "Padding", horizontal(list) ? pw : ph);
                if (horizontal(list)) {
                    x = px + cursorX;
                    y = py + align(list, "VerticalAlignment", ph, h);
                    cursorX += w + gapPx;
                    rowH = Math.max(rowH, h);
                } else {
                    x = px + align(list, "HorizontalAlignment", pw, w);
                    y = py + cursorY;
                    cursorY += h + gapPx;
                }
            } else {
                UDim2 pos = (UDim2) c.prop("Position");
                Vector2 anchor = (Vector2) c.prop("AnchorPoint");
                x = px + pos.x.scale * pw + pos.x.offset - anchor.x * w;
                y = py + pos.y.scale * ph + pos.y.offset - anchor.y * h;
            }
            setAbs(c, x, y, w, h);
            layoutChildren(c, x, y, w, h);
        }
        if (list != null) {
            Vector2 content = horizontal(list) ? new Vector2(cursorX, rowH) : new Vector2(pw, cursorY);
            if (!content.equals(list.prop("AbsoluteContentSize"))) {
                list.put("AbsoluteContentSize", content);
            }
        }
    }

    static boolean horizontal(Instance layout) {
        Object d = layout.prop("FillDirection");
        return d instanceof Enums.EnumItem && "Horizontal".equals(((Enums.EnumItem) d).name);
    }

    static double align(Instance layout, String prop, double extent, double size) {
        Object a = layout.prop(prop);
        String n = a instanceof Enums.EnumItem ? ((Enums.EnumItem) a).name : "";
        if ("Center".equals(n)) {
            return (extent - size) / 2;
        }
        if ("Right".equals(n) || "Bottom".equals(n)) {
            return extent - size;
        }
        return 0;
    }

    static double[] size(Instance c, double pw, double ph) {
        UDim2 s = (UDim2) c.prop("Size");
        double w = s.x.scale * pw + s.x.offset;
        double h = s.y.scale * ph + s.y.offset;
        Object sc = c.prop("SizeConstraint");
        String scn = sc instanceof Enums.EnumItem ? ((Enums.EnumItem) sc).name : "RelativeXY";
        if ("RelativeXX".equals(scn)) {
            h = s.y.scale * pw + s.y.offset;
        } else if ("RelativeYY".equals(scn)) {
            w = s.x.scale * ph + s.x.offset;
        }
        Instance ar = c.findFirstChildOfClass("UIAspectRatioConstraint", false);
        if (ar != null) {
            double ratio = Math.max(1e-6, ar.num("AspectRatio"));
            if (w / Math.max(h, 1e-6) > ratio) {
                w = h * ratio;
            } else {
                h = w / ratio;
            }
        }
        Instance lim = c.findFirstChildOfClass("UISizeConstraint", false);
        if (lim != null) {
            Vector2 mn = (Vector2) lim.prop("MinSize");
            Vector2 mx = (Vector2) lim.prop("MaxSize");
            w = Math.max(mn.x, Math.min(mx.x, w));
            h = Math.max(mn.y, Math.min(mx.y, h));
        }
        return new double[] {w, h};
    }

    static void sortByLayoutOrder(List<Instance> kids, Instance layout) {
        Object so = layout.prop("SortOrder");
        final boolean byName = so instanceof Enums.EnumItem && "Name".equals(((Enums.EnumItem) so).name);
        Collections.sort(kids, new Comparator<Instance>() {
            @Override
            public int compare(Instance a, Instance b) {
                if (byName) {
                    return a.getName().compareTo(b.getName());
                }
                return Double.compare(a.num("LayoutOrder"), b.num("LayoutOrder"));
            }
        });
    }
}
