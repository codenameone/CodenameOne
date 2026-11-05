/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.splat.ui;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;
import com.codenameone.examples.splat.play.Type;
import com.codenameone.examples.splat.rules.Arena;
import com.codenameone.examples.splat.rules.Box;
import com.codenameone.examples.splat.rules.Mode;
import java.util.List;

/// A yard as the briefing shows it: an overhead plot on a grid, cover in
/// steel, the two spawns in blue and red and the objective in amber, with
/// grid references down the edges. Drawn from the same arena the rules play,
/// so the map is never out of date.
public final class TacticalMap extends Component {
    private Arena arena;
    private Mode mode;

    public TacticalMap(Arena arena, Mode mode) {
        this.arena = arena;
        this.mode = mode;
        getAllStyles().setBgTransparency(0);
        setName("tacticalMap");
    }

    public void show(Arena a, Mode m) {
        arena = a;
        mode = m;
        repaint();
    }

    @Override
    protected Dimension calcPreferredSize() {
        int s = CN.convertToPixels(40);
        return new Dimension(s, s * 3 / 4);
    }

    @Override
    public void paint(Graphics g) {
        int x0 = getX();
        int y0 = getY();
        int w = getWidth();
        int h = getHeight();
        g.setAntiAliased(true);
        g.setColor(0x0e1216);
        g.fillRect(x0, y0, w, h);
        // The field is drawn lying on its side, the long axis across.
        int span = Math.max(arena.width, arena.depth);
        int pad = CN.convertToPixels(3);
        float k = Math.min((w - pad * 2) / (float) arena.depth, (h - pad * 2) / (float) arena.width);
        int fw = (int) (arena.depth * k);
        int fh = (int) (arena.width * k);
        int ox = x0 + (w - fw) / 2;
        int oy = y0 + (h - fh) / 2;
        g.setColor(0x1f262d);
        int cell = Math.max(4, (int) (5000 * k));
        for (int gx = ox; gx <= ox + fw; gx += cell) {
            g.drawLine(gx, oy, gx, oy + fh);
        }
        for (int gy = oy; gy <= oy + fh; gy += cell) {
            g.drawLine(ox, gy, ox + fw, gy);
        }
        g.setColor(0x4f5862);
        g.drawRect(ox, oy, fw, fh);
        List<Box> boxes = arena.boxes();
        for (int i = 0; i < boxes.size(); i++) {
            Box b = boxes.get(i);
            int bx = ox + (int) (b.z0 * k);
            int by = oy + (int) ((arena.width - b.x1) * k);
            int bw = Math.max(2, (int) ((b.z1 - b.z0) * k));
            int bh = Math.max(2, (int) ((b.x1 - b.x0) * k));
            g.setColor(0x56606a);
            g.fillRect(bx, by, bw, bh);
            g.setColor(0x8a949e);
            g.drawRect(bx, by, bw, bh);
        }
        int r = CN.convertToPixels(1.4f);
        for (int t = 0; t < 2; t++) {
            int sx = ox + (int) (arena.spawnZ(t, 0) * k);
            int sy = oy + (int) ((arena.width - arena.spawnX(t, 0)) * k);
            g.setColor(t == 0 ? Type.FRIENDLY : Type.HOSTILE);
            g.fillRect(sx - r, sy - r, r * 2, r * 2);
            if (mode == Mode.CAPTURE_THE_FLAG) {
                int fx = ox + (int) (arena.baseZ(t) * k);
                int fy = oy + (int) ((arena.width - arena.baseX(t)) * k);
                g.setColor(Type.AMBER);
                g.drawRect(fx - r * 2, fy - r * 2, r * 4, r * 4);
                g.drawString(t == 0 ? "A" : "B", fx + r * 2 + 2, fy - r * 2);
            }
        }
        if (mode == Mode.SNACK_BAR) {
            int hr = (int) (arena.hillRadius * k);
            int hx = ox + (int) (arena.hillZ * k);
            int hy = oy + (int) ((arena.width - arena.hillX) * k);
            g.setColor(Type.AMBER);
            g.drawArc(hx - hr, hy - hr, hr * 2, hr * 2, 0, 360);
            g.drawArc(hx - hr + 1, hy - hr + 1, hr * 2 - 2, hr * 2 - 2, 0, 360);
        }
        Font f = Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        g.setFont(f);
        g.setColor(0x8a949e);
        for (int i = 0; i * cell <= fw; i += 2) {
            g.drawString(String.valueOf((char) ('A' + i / 2 % 26)), ox + i * cell + 2, oy - f.getHeight());
        }
        g.setColor(Type.AMBER);
        String tag = "SAT FEED // " + Type.caps(arena.name) + " // " + (span / 1000) + "M";
        g.drawString(tag, x0 + pad / 2, y0 + h - f.getHeight() - 2);
    }
}
