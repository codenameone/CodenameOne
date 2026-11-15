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

import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Paints the local player's 2D GUI -- every enabled `ScreenGui` under
/// `PlayerGui` -- over the 3D view, from the absolute layout [GuiLayout]
/// computed this frame. GUI units are scaled to device pixels by `scale`.
public final class GuiPainter {
    private static final Comparator<Instance> ORDER = new Comparator<Instance>() {
        @Override
        public int compare(Instance a, Instance b) {
            return Double.compare(a.num("DisplayOrder"), b.num("DisplayOrder"));
        }
    };
    private static final Comparator<Instance> Z = new Comparator<Instance>() {
        @Override
        public int compare(Instance a, Instance b) {
            return Double.compare(z(a), z(b));
        }
    };
    private final Map<Integer, Font> fonts = new HashMap<Integer, Font>();

    private static double z(Instance i) {
        return i.isA("GuiObject") ? i.num("ZIndex") : 0;
    }

    /// Paints at (0,0) of `g`'s current translation.
    public void paint(Graphics g, Engine e, float scale) {
        Instance lp = e.getLocalPlayer();
        Instance pg = lp == null ? null : lp.findFirstChildOfClass("PlayerGui", false);
        if (pg == null) {
            return;
        }
        List<Instance> guis = new ArrayList<Instance>();
        for (Instance c : pg.getChildren()) {
            if (c.isA("ScreenGui") && c.bool("Enabled")) {
                guis.add(c);
            }
        }
        Collections.sort(guis, ORDER);
        int alpha = g.getAlpha();
        for (Instance s : guis) {
            children(g, s, scale);
        }
        g.setAlpha(alpha);
    }

    private void children(Graphics g, Instance parent, float scale) {
        List<Instance> kids = new ArrayList<Instance>(parent.getChildren());
        Collections.sort(kids, Z);
        for (Instance k : kids) {
            if (k.isA("GuiObject") && k.bool("Visible")) {
                paintObject(g, k, scale);
                children(g, k, scale);
            }
        }
    }

    private void paintObject(Graphics g, Instance o, float scale) {
        Vector2 p = (Vector2) o.prop("AbsolutePosition");
        Vector2 s = (Vector2) o.prop("AbsoluteSize");
        int x = Math.round((float) p.x * scale);
        int y = Math.round((float) p.y * scale);
        int w = Math.round((float) s.x * scale);
        int h = Math.round((float) s.y * scale);
        double fade = 1 - o.num("Transparency");
        int radius = 0;
        Instance corner = o.findFirstChildOfClass("UICorner", false);
        if (corner != null) {
            UDim cr = (UDim) corner.prop("CornerRadius");
            radius = Math.round((float) (cr.offset + cr.scale * Math.min(s.x, s.y)) * scale);
        }
        double bt = o.num("BackgroundTransparency");
        if (bt < 1) {
            g.setColor(SceneSnapshot.rgb(o.prop("BackgroundColor3")));
            g.setAlpha(alpha((1 - bt) * fade));
            if (radius > 0) {
                g.fillRoundRect(x, y, w, h, radius * 2, radius * 2);
            } else {
                g.fillRect(x, y, w, h);
            }
        }
        if (o.isA("ImageLabel") || o.isA("ImageButton")) {
            // Images reach a place as asset ids nothing here can fetch; a tint
            // keeps the layout readable.
            double it = o.num("ImageTransparency");
            if (it < 1) {
                g.setColor(SceneSnapshot.rgb(o.prop("ImageColor3")));
                g.setAlpha(alpha((1 - it) * fade * 0.25));
                g.fillRect(x, y, w, h);
            }
        }
        Instance stroke = o.findFirstChildOfClass("UIStroke", false);
        if (stroke != null && stroke.bool("Enabled")) {
            g.setColor(SceneSnapshot.rgb(stroke.prop("Color")));
            g.setAlpha(alpha((1 - stroke.num("Transparency")) * fade));
            int t = Math.max(1, Math.round((float) stroke.num("Thickness") * scale));
            for (int i = 0; i < t; i++) {
                g.drawRoundRect(x - i, y - i, w + 2 * i, h + 2 * i, radius * 2, radius * 2);
            }
        }
        if (o.isA("TextLabel") || o.isA("TextButton") || o.isA("TextBox")) {
            text(g, o, x, y, w, h, scale, fade);
        }
    }

    private void text(Graphics g, Instance o, int x, int y, int w, int h, float scale, double fade) {
        String t = o.str("Text");
        if (o.isA("TextBox") && t.length() == 0) {
            t = o.str("PlaceholderText");
        }
        if (t.length() == 0 || o.num("TextTransparency") >= 1) {
            return;
        }
        int px = Math.round((float) o.num("TextSize") * scale);
        if (o.bool("TextScaled")) {
            px = Math.max(6, Math.min(h * 4 / 5, w * 2 / Math.max(1, t.length())));
        }
        Font f = font(px);
        g.setFont(f);
        g.setColor(SceneSnapshot.rgb(o.prop("TextColor3")));
        g.setAlpha(alpha((1 - o.num("TextTransparency")) * fade));
        int tw = f.stringWidth(t);
        String xa = SceneSnapshot.enumName(o.prop("TextXAlignment"), "Center");
        String ya = SceneSnapshot.enumName(o.prop("TextYAlignment"), "Center");
        int tx = "Left".equals(xa) ? x : "Right".equals(xa) ? x + w - tw : x + (w - tw) / 2;
        int th = f.getHeight();
        int ty = "Top".equals(ya) ? y : "Bottom".equals(ya) ? y + h - th : y + (h - th) / 2;
        int[] clip = g.getClip();
        g.clipRect(x, y, w, h);
        g.drawString(t, tx, ty);
        g.setClip(clip);
    }

    private Font font(int px) {
        Integer k = Integer.valueOf(px);
        Font f = fonts.get(k);
        if (f == null) {
            f = Font.isNativeFontSchemeSupported() ? Font.createTrueTypeFont("native:MainBold", px, com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS)
                    : Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
            fonts.put(k, f);
        }
        return f;
    }

    private static int alpha(double a) {
        int v = (int) Math.round(a * 255);
        return v < 0 ? 0 : Math.min(255, v);
    }
}
