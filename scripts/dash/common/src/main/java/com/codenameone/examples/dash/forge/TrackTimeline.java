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
package com.codenameone.examples.dash.forge;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;
import com.codenameone.examples.dash.rules.Catalog;
import com.codenameone.examples.dash.rules.Clearance;
import com.codenameone.examples.dash.rules.LevelDef;
import com.codenameone.examples.dash.rules.ObstacleDef;
import com.codenameone.examples.dash.rules.PickupDef;
import com.codenameone.examples.dash.rules.PickupKind;
import com.codenameone.examples.dash.rules.Placement;
import com.codenameone.examples.dash.rules.Track;
import com.codenameone.examples.dash.rules.ViewCue;
import com.codenameone.examples.dash.rules.ViewMode;

/// The level as a strip you can paint on: distance runs left to right, one
/// row per lane and an "all lanes" row on top, camera cues as flags along the
/// bottom. Scroll it sideways; tap a cell to put the selected piece there,
/// tap a piece to remove it, and with a camera tool tap anywhere to drop a
/// cue -- the swing from the side-on view to the chase view is a flag here.
// tag::dash-timeline[]
public final class TrackTimeline extends Component {
    /// What a tap does.
    public interface Tool {
        /// Applies the tool at `metres` in `lane` ([Placement#ALL_LANES] for
        /// the top row); returns true when the level changed.
        boolean apply(LevelDef level, int metres, int lane);
    }

    /// Hears about every change, to refresh the problems list.
    public interface Changed {
        void changed();
    }

    private static final int STEP_METRES = 5;
    private final LevelDef level;
    private final Catalog catalog;
    private Tool tool;
    private Changed changed;
    private int pressX;
    private int pressY;

    public TrackTimeline(LevelDef level, Catalog catalog) {
        this.level = level;
        this.catalog = catalog;
        setUIID("Timeline");
        setFocusable(true);
        setName("timeline");
    }

    public void setTool(Tool tool) {
        this.tool = tool;
    }

    public void setChanged(Changed changed) {
        this.changed = changed;
    }

    private int ppm() {
        return Math.max(4, CN.convertToPixels(1.6f));
    }

    private int rowH() {
        return CN.convertToPixels(6);
    }

    private int lanes() {
        return Math.max(1, level.lanes);
    }

    /// Metres the strip shows: the level's length, or the hand-built part of
    /// an endless one plus room to keep building.
    private int metres() {
        int end = level.length > 0 ? level.length / Track.UNIT : 0;
        for (Placement p : level.placements) {
            end = Math.max(end, p.z / Track.UNIT + 40);
        }
        for (ViewCue c : level.cues) {
            end = Math.max(end, c.at / Track.UNIT + 40);
        }
        return Math.max(end, 200);
    }

    @Override
    protected Dimension calcPreferredSize() {
        return new Dimension(metres() * ppm() + rowH(), rowH() * (lanes() + 2));
    }

    @Override
    public void paint(Graphics g) {
        int x0 = getX();
        int y0 = getY();
        int ppm = ppm();
        int rh = rowH();
        int w = metres() * ppm;
        g.setColor(0x1d2233);
        g.fillRect(x0, y0, w + rh, rh * (lanes() + 2));
        Font small = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        g.setFont(small);
        for (int r = 0; r <= lanes(); r++) {
            g.setColor(r == 0 ? 0x2b3350 : r % 2 == 0 ? 0x252b40 : 0x2a3148);
            g.fillRect(x0, y0 + r * rh, w + rh, rh - 1);
        }
        for (int m = 0; m <= metres(); m += 10) {
            g.setColor(m % 50 == 0 ? 0x5a6488 : 0x3a4262);
            g.drawLine(x0 + m * ppm, y0, x0 + m * ppm, y0 + rh * (lanes() + 1));
            if (m % 50 == 0) {
                g.setColor(0xaab4d8);
                g.drawString(m + " m", x0 + m * ppm + 2, y0 + rh * (lanes() + 1) + 2);
            }
        }
        if (level.length > 0) {
            int fx = x0 + level.length / Track.UNIT * ppm;
            g.setColor(0xffffff);
            g.fillRect(fx, y0, 3, rh * (lanes() + 1));
        }
        for (Placement p : level.placements) {
            int px = x0 + p.z / Track.UNIT * ppm;
            int row = p.lane == Placement.ALL_LANES ? 0 : 1 + Math.min(p.lane, lanes() - 1);
            ObstacleDef o = catalog.obstacle(p.defId);
            int width = o == null ? ppm * 2 : Math.max(ppm, o.depth / Track.UNIT * ppm);
            g.setColor(color(p.defId));
            if (row == 0) {
                g.fillRect(px, y0 + 2, width, rh * (lanes() + 1) - 4);
                g.setColor(0x000000);
            } else if (o == null) {
                g.fillArc(px, y0 + row * rh + rh / 4, rh / 2, rh / 2, 0, 360);
            } else {
                g.fillRect(px, y0 + row * rh + 3, width, rh - 6);
            }
        }
        for (ViewCue c : level.cues) {
            int cx = x0 + c.at / Track.UNIT * ppm;
            int by = y0 + rh * (lanes() + 1);
            g.setColor(0xffd34d);
            g.drawLine(cx, y0, cx, by + rh);
            int bw = Math.max(ppm, c.blendMillis / Track.UNIT * ppm);
            g.setAlpha(90);
            g.fillRect(cx, by, bw, rh / 2);
            g.setAlpha(255);
            g.drawString(modeName(c.mode), cx + 3, by + rh / 2);
        }
    }

    private int color(String defId) {
        ObstacleDef o = catalog.obstacle(defId);
        if (o != null) {
            if (o.clearance == Clearance.JUMP) {
                return o.height == 0 ? 0x101018 : 0xff9a3c;
            }
            return o.clearance == Clearance.SLIDE ? 0xe0405a : 0x4a78c8;
        }
        PickupDef k = catalog.pickup(defId);
        return k != null && k.kind == PickupKind.COIN ? 0xffcc33 : 0x3ce0ff;
    }

    static String modeName(ViewMode m) {
        return m == ViewMode.SIDE_2D ? "2D" : m == ViewMode.TOP_DOWN ? "Top" : "3D";
    }

    @Override
    public void pointerPressed(int x, int y) {
        super.pointerPressed(x, y);
        pressX = x;
        pressY = y;
    }

    @Override
    public void pointerReleased(int x, int y) {
        super.pointerReleased(x, y);
        int slop = CN.convertToPixels(2);
        if (tool == null || Math.abs(x - pressX) > slop || Math.abs(y - pressY) > slop) {
            return;
        }
        int lx = x - getAbsoluteX() + getScrollX();
        int ly = y - getAbsoluteY() + getScrollY();
        int metres = (lx / ppm()) / STEP_METRES * STEP_METRES;
        int row = ly / rowH();
        int lane = row == 0 ? Placement.ALL_LANES : Math.min(lanes() - 1, row - 1);
        if (tool.apply(level, metres, lane)) {
            setShouldCalcPreferredSize(true);
            getParent().revalidateLater();
            repaint();
            if (changed != null) {
                changed.changed();
            }
        }
    }
}
// end::dash-timeline[]
