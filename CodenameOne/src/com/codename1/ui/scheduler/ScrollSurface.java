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
package com.codename1.ui.scheduler;

import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.animations.Motion;
import com.codename1.ui.events.WheelEvent;
import com.codename1.ui.geom.Dimension;

/// A painted surface that scrolls on two axes under a pinned header row and a
/// pinned header column.
///
/// The scheduling views draw every item themselves, so a schedule of ten
/// thousand items costs what is on screen and nothing more; a component per
/// item would not scroll. That leaves this class to do what a scrollable
/// `Container` does for components: panning, the fling after it, the wheel,
/// pinch, and telling a tap from a drag from a long press.
///
/// A subclass says how big its content is, paints the four regions, and
/// answers three questions about a pointer: is an edit starting here, where
/// has it been dragged to, and has it ended.
abstract class ScrollSurface extends Component {
    private static final int NONE = 0;
    private static final int PAN = 1;
    private static final int EDIT = 2;
    private static final int DOUBLE_TAP_MS = 350;

    /// The width of the pinned column and the height of the pinned row. A
    /// subclass sets them in `prepare`.
    int headerW;
    int headerH;

    private int offX;
    private int offY;
    private Motion flingX;
    private Motion flingY;
    private boolean registered;
    private int gesture;
    private boolean pressed;
    private int pressX;
    private int pressY;
    private int lastX;
    private int lastY;
    private long lastTapTime;
    private int lastTapX;
    private int lastTapY;
    private int autoX;
    private int autoY;
    private boolean pinching;
    private boolean dragEdits = Display.getInstance().isDesktop();

    ScrollSurface() {
        setFocusable(true);
        setGrabsPointerEvents(true);
    }

    // ------------------------------------------------------------ content

    /// Lays the surface out for a size. Called before every paint and every
    /// pointer event, so it has to return at once when nothing changed.
    abstract void prepare(int width, int height);

    /// The size of what scrolls, in pixels, without the headers.
    abstract int contentWidth();

    abstract int contentHeight();

    /// Paints what scrolls. `x` and `y` are where the content's own origin is
    /// on screen, which is off screen once it has scrolled.
    abstract void paintBody(Graphics g, int x, int y);

    /// Paints the pinned row. `x` is where the content's origin is; `y` is
    /// the top of the row.
    void paintTopHeader(Graphics g, int x, int y) {
    }

    /// Paints the pinned column. `x` is its left; `y` is where the content's
    /// origin is.
    void paintLeftHeader(Graphics g, int x, int y) {
    }

    void paintCorner(Graphics g, int x, int y) {
    }

    /// Painted last, over everything, in the component's own bounds.
    void paintOverlay(Graphics g, int x, int y) {
    }

    /// Asked when a drag or a long press begins at a point, in the
    /// component's coordinates. `handlesOnly` is true for a plain drag on a
    /// touch screen, which pans unless it starts on something made to be
    /// dragged, such as the resize handle of the selected item.
    ///
    /// #### Returns
    ///
    /// true when an edit has begun and the drag belongs to it
    boolean beginEdit(int x, int y, boolean handlesOnly) {
        return false;
    }

    void editDragged(int x, int y) {
    }

    /// The edit is over: `commit` is false when it was cancelled.
    void endEdit(boolean commit) {
    }

    void tapped(int x, int y, boolean doubleTap) {
    }

    /// A long press that started no edit.
    void longPressed(int x, int y) {
    }

    /// The scroll position changed.
    void scrolled() {
    }

    /// Zooms by `factor` around a point. False when the surface has no zoom.
    boolean zoomBy(float factor, int x, int y) {
        return false;
    }

    /// Whether a plain drag on an item or on empty space edits instead of
    /// panning. On by default where there is a mouse, which has a wheel to
    /// scroll with.
    void setDragEdits(boolean v) {
        dragEdits = v;
    }

    boolean isDragEdits() {
        return dragEdits;
    }

    // ------------------------------------------------------------- scroll

    int bodyWidth() {
        return Math.max(0, getWidth() - headerW);
    }

    int bodyHeight() {
        return Math.max(0, getHeight() - headerH);
    }

    int maxOffX() {
        return Math.max(0, contentWidth() - bodyWidth());
    }

    int maxOffY() {
        return Math.max(0, contentHeight() - bodyHeight());
    }

    int getOffX() {
        return offX;
    }

    int getOffY() {
        return offY;
    }

    /// Scrolls to a position, held inside the content.
    void scrollTo(int x, int y) {
        int nx = Math.max(0, Math.min(maxOffX(), x));
        int ny = Math.max(0, Math.min(maxOffY(), y));
        if (nx != offX || ny != offY) {
            offX = nx;
            offY = ny;
            scrolled();
            repaint();
        }
    }

    /// Sets the position without holding it inside the content, for a
    /// subclass whose content moves under the scroll position.
    void setOffsets(int x, int y) {
        offX = x;
        offY = y;
    }

    /// Lays out for the current size and holds the scroll position inside the
    /// content.
    void layoutNow() {
        prepare(getWidth(), getHeight());
        int nx = Math.max(0, Math.min(maxOffX(), offX));
        int ny = Math.max(0, Math.min(maxOffY(), offY));
        if (nx != offX || ny != offY) {
            offX = nx;
            offY = ny;
            scrolled();
        }
    }

    void stopFling() {
        flingX = null;
        flingY = null;
    }

    private void animateNow() {
        if (!registered) {
            Form f = getComponentForm();
            if (f != null) {
                f.registerAnimated(this);
                registered = true;
            }
        }
    }

    /// Asks for `animate` to be called on the next frames, for a subclass
    /// with a motion of its own.
    void requestAnimation() {
        animateNow();
    }

    /// Whether the subclass still has something moving. Called every frame
    /// while the surface animates.
    boolean animateSurface() {
        return false;
    }

    @Override
    public boolean animate() {
        boolean busy = false;
        int nx = offX;
        int ny = offY;
        if (flingX != null) {
            nx = flingX.getValue();
            if (flingX.isFinished()) {
                flingX = null;
            }
            busy = true;
        }
        if (flingY != null) {
            ny = flingY.getValue();
            if (flingY.isFinished()) {
                flingY = null;
            }
            busy = true;
        }
        if (gesture == EDIT && (autoX != 0 || autoY != 0)) {
            nx += autoX;
            ny += autoY;
            busy = true;
        }
        if (busy) {
            int bx = offX;
            int by = offY;
            scrollTo(nx, ny);
            if (gesture == EDIT && (bx != offX || by != offY)) {
                editDragged(lastX, lastY);
            }
        }
        if (animateSurface()) {
            busy = true;
        }
        if (!busy && registered) {
            Form f = getComponentForm();
            if (f != null) {
                f.deregisterAnimated(this);
            }
            registered = false;
        }
        return busy;
    }

    @Override
    protected void deinitialize() {
        stopFling();
        if (registered) {
            Form f = getComponentForm();
            if (f != null) {
                f.deregisterAnimated(this);
            }
            registered = false;
        }
        super.deinitialize();
    }

    // -------------------------------------------------------------- paint

    @Override
    public void paint(Graphics g) {
        layoutNow();
        int x0 = getX();
        int y0 = getY();
        int cx = g.getClipX();
        int cy = g.getClipY();
        int cw = g.getClipWidth();
        int ch = g.getClipHeight();
        int bx = x0 + headerW;
        int by = y0 + headerH;
        int bw = bodyWidth();
        int bh = bodyHeight();
        g.clipRect(bx, by, bw, bh);
        paintBody(g, bx - offX, by - offY);
        g.setClip(cx, cy, cw, ch);
        if (headerH > 0) {
            g.clipRect(bx, y0, bw, headerH);
            paintTopHeader(g, bx - offX, y0);
            g.setClip(cx, cy, cw, ch);
        }
        if (headerW > 0) {
            g.clipRect(x0, by, headerW, bh);
            paintLeftHeader(g, x0, by - offY);
            g.setClip(cx, cy, cw, ch);
        }
        if (headerW > 0 && headerH > 0) {
            g.clipRect(x0, y0, headerW, headerH);
            paintCorner(g, x0, y0);
            g.setClip(cx, cy, cw, ch);
        }
        g.clipRect(x0, y0, getWidth(), getHeight());
        paintOverlay(g, x0, y0);
        g.setClip(cx, cy, cw, ch);
    }

    @Override
    protected Dimension calcPreferredSize() {
        // A view is given its size by the layout around it; this is what it
        // asks for when nothing constrains it.
        return new Dimension(Look.mm(60), Look.mm(60));
    }

    // ------------------------------------------------------------ pointer

    @Override
    public void pointerPressed(int x, int y) {
        super.pointerPressed(x, y);
        layoutNow();
        stopFling();
        pressed = true;
        gesture = NONE;
        autoX = 0;
        autoY = 0;
        pressX = x - getAbsoluteX();
        pressY = y - getAbsoluteY();
        lastX = pressX;
        lastY = pressY;
    }

    @Override
    public void pointerDragged(int x, int y) {
        if (!pressed || pinching) {
            return;
        }
        int lx = x - getAbsoluteX();
        int ly = y - getAbsoluteY();
        if (gesture == NONE) {
            int slop = Look.mm(1.5f);
            if (Math.abs(lx - pressX) < slop && Math.abs(ly - pressY) < slop) {
                return;
            }
            gesture = beginEdit(pressX, pressY, !dragEdits) ? EDIT : PAN;
        }
        if (gesture == EDIT) {
            editDragged(lx, ly);
            autoScroll(lx, ly);
            repaint();
        } else {
            scrollTo(offX - (lx - lastX), offY - (ly - lastY));
        }
        lastX = lx;
        lastY = ly;
    }

    @Override
    public void longPointerPress(int x, int y) {
        if (!pressed || gesture != NONE) {
            return;
        }
        int lx = x - getAbsoluteX();
        int ly = y - getAbsoluteY();
        if (beginEdit(lx, ly, false)) {
            gesture = EDIT;
            lastX = lx;
            lastY = ly;
            editDragged(lx, ly);
            repaint();
        } else {
            // A press this long is not a tap, whatever follows.
            gesture = PAN;
            longPressed(lx, ly);
        }
    }

    @Override
    public void pointerReleased(int x, int y) {
        super.pointerReleased(x, y);
        if (!pressed) {
            return;
        }
        pressed = false;
        pinching = false;
        pinchBase = 0;
        autoX = 0;
        autoY = 0;
        int g = gesture;
        gesture = NONE;
        if (g == EDIT) {
            endEdit(true);
            repaint();
        } else if (g == PAN) {
            fling();
        } else {
            int lx = x - getAbsoluteX();
            int ly = y - getAbsoluteY();
            long now = System.currentTimeMillis();
            int near = Look.mm(4);
            boolean twice = now - lastTapTime < DOUBLE_TAP_MS && Math.abs(lx - lastTapX) < near
                    && Math.abs(ly - lastTapY) < near;
            lastTapTime = twice ? 0 : now;
            lastTapX = lx;
            lastTapY = ly;
            tapped(lx, ly, twice);
        }
    }

    /// Cancels an edit in progress, as Escape does.
    void cancelEdit() {
        if (gesture == EDIT) {
            gesture = PAN;
            autoX = 0;
            autoY = 0;
            endEdit(false);
            repaint();
        }
    }

    boolean isDragEditing() {
        return gesture == EDIT;
    }

    private void fling() {
        float vx = Display.getInstance().getDragSpeed(false);
        float vy = Display.getInstance().getDragSpeed(true);
        boolean any = false;
        if (Math.abs(vx) > 0.1f && maxOffX() > 0) {
            flingX = Motion.createFrictionMotion(offX, vx < 0 ? 0 : maxOffX(), vx, 0.0007f);
            flingX.start();
            any = true;
        }
        if (Math.abs(vy) > 0.1f && maxOffY() > 0) {
            flingY = Motion.createFrictionMotion(offY, vy < 0 ? 0 : maxOffY(), vy, 0.0007f);
            flingY.start();
            any = true;
        }
        if (any) {
            animateNow();
        }
    }

    // While an edit is dragged near an edge the surface scrolls under it, and
    // faster the nearer the edge.
    private void autoScroll(int lx, int ly) {
        int band = Look.mm(6);
        int step = Look.mm(2.5f);
        autoX = edgeSpeed(lx, headerW, getWidth(), band, step);
        autoY = edgeSpeed(ly, headerH, getHeight(), band, step);
        if (autoX != 0 || autoY != 0) {
            animateNow();
        }
    }

    private static int edgeSpeed(int p, int from, int to, int band, int step) {
        if (p < from + band) {
            return -step * Math.min(band, from + band - p) / band;
        }
        if (p > to - band) {
            return step * Math.min(band, p - (to - band)) / band;
        }
        return 0;
    }

    @Override
    protected boolean mouseWheel(WheelEvent ev) {
        layoutNow();
        if (ev.isControlDown() || ev.isMetaDown()) {
            int d = ev.getDeltaY() != 0 ? ev.getDeltaY() : ev.getDeltaX();
            if (d != 0 && zoomBy(d < 0 ? 1.15f : 1 / 1.15f, ev.getX() - getAbsoluteX(), ev.getY() - getAbsoluteY())) {
                return true;
            }
        }
        int dx = ev.getDeltaX();
        int dy = ev.getDeltaY();
        if (ev.isShiftDown() && dx == 0) {
            dx = dy;
            dy = 0;
        }
        int bx = offX;
        int by = offY;
        stopFling();
        scrollTo(offX + dx, offY + dy);
        // An event that moved nothing is left for whatever is around this.
        return bx != offX || by != offY;
    }

    private float pinchBase;

    @Override
    protected boolean pinch(float scale) {
        // The scale is the fingers' distance over what it was when the pinch
        // began, so every call is applied as the change since the last one.
        if (pinchBase <= 0) {
            pinchBase = 1f;
        }
        float step = scale / pinchBase;
        if (zoomBy(step, lastX, lastY)) {
            pinchBase = scale;
            pinching = true;
            gesture = PAN;
            return true;
        }
        return false;
    }

    @Override
    protected void pinchReleased(int x, int y) {
        pinchBase = 0;
        pinching = false;
    }

    @Override
    protected boolean shouldBlockSideSwipe() {
        return true;
    }
}
