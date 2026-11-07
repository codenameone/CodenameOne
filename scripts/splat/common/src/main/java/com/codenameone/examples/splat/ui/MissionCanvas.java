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
import com.codenameone.examples.splat.rules.Mission;
import com.codenameone.examples.splat.rules.Mode;

/// The mission editor's field, from above: your half at the bottom in full,
/// the other half above the line as the mirror the game will build from it,
/// dimmed. Tap empty ground to drop the selected tool there, press on a prop,
/// spawn or the flag to pick it up and drag it.
///
/// Everything is in the mission's decimetres; a 1 m grid snaps what is placed.
final class MissionCanvas extends Component {
    /// What a tap on empty ground places.
    static final String SPAWN = "spawn";
    static final String FLAG = "flag";

    interface Listener {
        void changed();
    }

    private final Mission mission;
    private final Listener listener;
    private String tool = "sandbags";
    /// The prop index being dragged, -2 a spawn, -3 the flag, -1 nothing.
    private int held = -1;
    private int heldSpawn = -1;
    /// The last prop touched: what TURN and REMOVE act on.
    private int selected = -1;
    private float k = 1;
    private int ox;
    private int oy;

    MissionCanvas(Mission mission, Listener listener) {
        this.mission = mission;
        this.listener = listener;
        setName("missionCanvas");
        setFocusable(true);
        getAllStyles().setBgTransparency(0);
    }

    void setTool(String t) {
        tool = t;
    }

    String tool() {
        return tool;
    }

    int selected() {
        return selected;
    }

    void select(int i) {
        selected = i;
        repaint();
    }

    @Override
    protected Dimension calcPreferredSize() {
        // the whole field fits on screen with the tools around it
        int h = CN.getDisplayHeight() * (CN.isDesktop() ? 55 : 46) / 100;
        return new Dimension(Math.min(CN.getDisplayWidth(), h * Mission.WIDTH_M / Mission.DEPTH_M + 8), h);
    }

    private void layoutField() {
        int pad = CN.convertToPixels(2);
        k = Math.min((getWidth() - pad * 2) / (float) (Mission.WIDTH_M * 10),
                (getHeight() - pad * 2) / (float) (Mission.DEPTH_M * 10));
        ox = getX() + (getWidth() - (int) (Mission.WIDTH_M * 10 * k)) / 2;
        oy = getY() + (getHeight() - (int) (Mission.DEPTH_M * 10 * k)) / 2;
    }

    /// Screen x of a field x: mirrored, as the game draws it.
    private int sx(int x) {
        return ox + (int) ((Mission.WIDTH_M * 10 - x) * k);
    }

    private int sy(int z) {
        return oy + (int) ((Mission.DEPTH_M * 10 - z) * k);
    }

    private int fx(int px) {
        return Mission.WIDTH_M * 10 - (int) ((px - ox) / k);
    }

    private int fz(int py) {
        return Mission.DEPTH_M * 10 - (int) ((py - oy) / k);
    }

    private static int snap(int v) {
        return (v + 5) / 10 * 10;
    }

    @Override
    public void paint(Graphics g) {
        layoutField();
        g.setAntiAliased(true);
        int fw = (int) (Mission.WIDTH_M * 10 * k);
        int fh = (int) (Mission.DEPTH_M * 10 * k);
        g.setColor(0x0e1216);
        g.fillRect(getX(), getY(), getWidth(), getHeight());
        g.setColor(0x232a31);
        g.fillRect(ox, oy + fh / 2, fw, fh / 2);
        g.setColor(0x171c21);
        g.fillRect(ox, oy, fw, fh / 2);
        g.setColor(0x2c343c);
        for (int m = 0; m <= Mission.WIDTH_M; m += 4) {
            g.drawLine(sx(m * 10), oy, sx(m * 10), oy + fh);
        }
        for (int m = 0; m <= Mission.DEPTH_M; m += 4) {
            g.drawLine(ox, sy(m * 10), ox + fw, sy(m * 10));
        }
        g.setColor(Type.AMBER);
        g.drawLine(ox, oy + fh / 2, ox + fw, oy + fh / 2);
        g.drawLine(ox, oy + fh / 2 + 1, ox + fw, oy + fh / 2 + 1);
        g.setColor(0x4f5862);
        g.drawRect(ox, oy, fw, fh);
        int far = Mission.DEPTH_M * 10;
        int wide = Mission.WIDTH_M * 10;
        for (int pass = 0; pass < 2; pass++) {
            boolean mirror = pass == 0;
            for (int i = 0; i < mission.props.size(); i++) {
                Mission.Prop p = mission.props.get(i);
                int cx = mirror ? wide - p.x : p.x;
                int cz = mirror ? far - p.z : p.z;
                int x0 = sx(cx + p.width() / 2);
                int y0 = sy(cz + p.depth() / 2);
                int w = Math.max(3, (int) (p.width() * k));
                int h = Math.max(3, (int) (p.depth() * k));
                g.setColor(mirror ? 0x3a424a : (i == selected ? 0x7d6a3a : 0x5d6873));
                g.fillRect(x0, y0, w, h);
                g.setColor(mirror ? 0x4f5862 : (i == selected ? Type.AMBER : 0xa9b3bd));
                g.drawRect(x0, y0, w, h);
            }
        }
        Font f = Type.bold(2.2f);
        g.setFont(f);
        int r = Math.max(CN.convertToPixels(1.3f), (int) (8 * k));
        for (int i = 0; i < mission.spawns.size(); i++) {
            int[] s = mission.spawns.get(i);
            dot(g, sx(s[0]), sy(s[1]), r, Type.FRIENDLY, String.valueOf(i + 1));
            dot(g, sx(wide - s[0]), sy(far - s[1]), r, Type.HOSTILE, null);
        }
        if (mission.mode == Mode.CAPTURE_THE_FLAG) {
            flag(g, sx(mission.baseX), sy(mission.baseZ), r, Type.FRIENDLY);
            flag(g, sx(wide - mission.baseX), sy(far - mission.baseZ), r, Type.HOSTILE);
        } else if (mission.mode == Mode.SNACK_BAR) {
            int hr = (int) (40 * k);
            g.setColor(Type.AMBER);
            g.drawArc(sx(wide / 2) - hr, sy(far / 2) - hr, hr * 2, hr * 2, 0, 360);
        }
        g.setColor(Type.STEEL);
        g.drawString("ENEMY HALF (MIRRORED)", ox + 6, oy + 4);
        g.setColor(Type.AMBER);
        g.drawString("YOUR HALF", ox + 6, oy + fh - f.getHeight() - 4);
    }

    private static void dot(Graphics g, int x, int y, int r, int color, String label) {
        g.setColor(color);
        g.fillArc(x - r, y - r, r * 2, r * 2, 0, 360);
        if (label != null) {
            g.setColor(Type.INK);
            g.drawString(label, x - g.getFont().stringWidth(label) / 2, y - g.getFont().getHeight() / 2);
        }
    }

    private static void flag(Graphics g, int x, int y, int r, int color) {
        g.setColor(Type.AMBER);
        g.fillRect(x - 1, y - r * 2, 3, r * 2);
        g.setColor(color);
        g.fillTriangle(x + 2, y - r * 2, x + 2, y - r, x + r * 2, y - r * 3 / 2);
    }

    @Override
    public void pointerPressed(int ax, int ay) {
        // pointer events are absolute; the field is laid out in paint's
        // parent-relative coordinates
        int px = ax - getAbsoluteX() + getX();
        int py = ay - getAbsoluteY() + getY();
        layoutField();
        int x = fx(px);
        int z = fz(py);
        held = -1;
        heldSpawn = -1;
        int grab = (int) (CN.convertToPixels(3) / k);
        for (int i = 0; i < mission.spawns.size(); i++) {
            int[] s = mission.spawns.get(i);
            if (Math.abs(s[0] - x) < grab && Math.abs(s[1] - z) < grab) {
                held = -2;
                heldSpawn = i;
                return;
            }
        }
        if (mission.mode == Mode.CAPTURE_THE_FLAG && Math.abs(mission.baseX - x) < grab
                && Math.abs(mission.baseZ - z) < grab) {
            held = -3;
            return;
        }
        int hit = mission.propAt(x, z, 0);
        if (hit >= 0) {
            held = hit;
            selected = hit;
            repaint();
            return;
        }
        if (z > Mission.DEPTH_M * 5) {
            return;
        }
        x = Math.max(10, Math.min(Mission.WIDTH_M * 10 - 10, x));
        z = Math.max(10, z);
        if (SPAWN.equals(tool)) {
            if (mission.spawns.size() < Mission.MAX_SPAWNS) {
                mission.spawns.add(new int[] {snap(x), snap(z)});
                held = -2;
                heldSpawn = mission.spawns.size() - 1;
            }
        } else if (FLAG.equals(tool)) {
            mission.baseX = snap(x);
            mission.baseZ = snap(z);
            held = -3;
        } else if (mission.props.size() < Mission.MAX_PROPS) {
            mission.props.add(inside(new Mission.Prop(tool, snap(x), snap(z), false)));
            held = mission.props.size() - 1;
            selected = held;
        }
        changed();
    }

    @Override
    public void pointerDragged(int ax, int ay) {
        if (held == -1) {
            return;
        }
        int px = ax - getAbsoluteX() + getX();
        int py = ay - getAbsoluteY() + getY();
        layoutField();
        int x = snap(Math.max(0, Math.min(Mission.WIDTH_M * 10, fx(px))));
        int z = snap(Math.max(0, Math.min(Mission.DEPTH_M * 5, fz(py))));
        if (held == -2) {
            mission.spawns.set(heldSpawn, new int[] {x, z});
        } else if (held == -3) {
            mission.baseX = x;
            mission.baseZ = z;
        } else {
            Mission.Prop p = mission.props.get(held);
            mission.props.set(held, inside(new Mission.Prop(p.kind, x, z, p.turned)));
        }
        changed();
    }

    @Override
    public void pointerReleased(int px, int py) {
        held = -1;
        heldSpawn = -1;
    }

    /// The prop moved, if need be, so all of it is inside your half.
    private static Mission.Prop inside(Mission.Prop p) {
        int hw = (p.width() + 1) / 2;
        int hd = (p.depth() + 1) / 2;
        int x = Math.max(hw, Math.min(Mission.WIDTH_M * 10 - hw, p.x));
        int z = Math.max(hd, Math.min(Mission.DEPTH_M * 5 - hd, p.z));
        return x == p.x && z == p.z ? p : new Mission.Prop(p.kind, x, z, p.turned);
    }

    /// Turns the selected prop a quarter.
    void turnSelected() {
        if (selected >= 0 && selected < mission.props.size()) {
            Mission.Prop p = mission.props.get(selected);
            mission.props.set(selected, inside(new Mission.Prop(p.kind, p.x, p.z, !p.turned)));
            changed();
        }
    }

    /// Takes away the selected prop, or the last spawn when no prop is selected.
    void removeSelected() {
        if (selected >= 0 && selected < mission.props.size()) {
            mission.props.remove(selected);
            selected = -1;
        } else if (mission.spawns.size() > 1) {
            mission.spawns.remove(mission.spawns.size() - 1);
        }
        changed();
    }

    private void changed() {
        repaint();
        listener.changed();
    }
}
