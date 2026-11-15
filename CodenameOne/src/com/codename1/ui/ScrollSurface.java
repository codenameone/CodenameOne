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
package com.codename1.ui;

import com.codename1.ui.animations.Motion;
import com.codename1.ui.events.WheelEvent;
import com.codename1.ui.geom.Dimension;

/// A painted component that scrolls on two axes, under an optional pinned
/// header row and pinned header column.
///
/// A view that draws thousands of items itself -- a calendar, a Gantt chart,
/// a diagram -- cannot afford a component per item, and so cannot use a
/// scrollable `Container`. This class does for such a view what a scrollable
/// container does for components: panning and the fling after it, the mouse
/// wheel, pinch to zoom, auto scrolling while something is dragged near an
/// edge, and telling a tap from a double tap, a drag and a long press. The
/// view only says how big its content is and paints it.
///
/// ```java
/// class Sheet extends ScrollSurface {
///     protected void layoutSurface(int width, int height) {
///         setHeaderSize(80, 30);
///     }
///     protected int getContentWidth() { return 5000; }
///     protected int getContentHeight() { return 20000; }
///     protected void paintContent(Graphics g, int x, int y) {
///         // the content's origin is at x, y; only the visible part is clipped in
///     }
/// }
/// ```
///
/// #### Regions
///
/// The component is divided into the content, which scrolls on both axes; the
/// top header, which scrolls only horizontally with it; the left header, which
/// scrolls only vertically; and the corner where the two meet, which does not
/// scroll. Each is painted clipped to its own region. `paintOverlay` paints
/// last, over all four. A view without headers leaves their size at zero.
///
/// #### Edits
///
/// A drag either pans or edits. When a drag or a long press begins, the
/// surface asks `editStarted` whether it begins an edit, telling it what kind
/// of gesture it is (`EditTrigger`). From then on the pointer goes to
/// `editMoved` and, when it lifts, to `editEnded`. `isDragEdits` decides
/// whether a plain drag may start an edit anywhere, which is the default where
/// there is a mouse, or only on things made to be dragged (`EditTrigger.HANDLE_DRAG`),
/// which leaves a finger free to pan.
///
/// #### Bounded and unbounded content
///
/// By default the scroll position stays inside the content. A surface whose
/// content has no edges, such as an infinite canvas, turns that off with
/// `setBounded(false)` and reads the moves through `scrollChanged`.
public abstract class ScrollSurface extends Component {
    /// What started a possible edit, as `editStarted` is told.
    public enum EditTrigger {
        /// A drag where an edit may start on anything editable: the default
        /// with a mouse, or wherever `setDragEdits(true)` was called.
        DRAG,

        /// A drag on a touch screen: it pans unless it starts on something
        /// made to be dragged, such as the resize handle of a selected item.
        HANDLE_DRAG,

        /// A long press.
        LONG_PRESS
    }

    /// How a surface's content divides into pages, from `getPageGrid`.
    public static final class PageGrid {
        private final int columns;
        private final int rows;
        private final int pageContentWidth;
        private final int pageContentHeight;

        PageGrid(int columns, int rows, int pageContentWidth, int pageContentHeight) {
            this.columns = columns;
            this.rows = rows;
            this.pageContentWidth = pageContentWidth;
            this.pageContentHeight = pageContentHeight;
        }

        /// The number of pages across.
        public int getColumns() {
            return columns;
        }

        /// The number of pages down.
        public int getRows() {
            return rows;
        }

        /// The width of content each page holds, in pixels.
        public int getPageContentWidth() {
            return pageContentWidth;
        }

        /// The height of content each page holds, in pixels.
        public int getPageContentHeight() {
            return pageContentHeight;
        }
    }

    /// How long a hover takes to come up and to fade, in milliseconds.
    protected static final int HOVER_MS = 140;

    /// How long an item that is picked up takes to rise, in milliseconds.
    protected static final int LIFT_MS = 160;

    /// How long a dropped item takes to travel to its place, in milliseconds.
    protected static final int SETTLE_MS = 220;

    private static final int NONE = 0;
    private static final int PAN = 1;
    private static final int EDIT = 2;
    private static final int DOUBLE_TAP_MS = 350;

    // An unbounded fling has nowhere to stop but where friction stops it.
    private static final int FAR = 1000000000;

    private int headerWidth;
    private int headerHeight;
    private boolean bounded = true;
    private boolean dragEdits = Display.getInstance().isDesktop();

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
    private float pinchBase;
    // The gesture under way ends without a fling.
    private boolean still;
    private boolean printing;

    private long fixedClock = -1;
    private Object hoverKey;
    private long hoverSince;
    private Object leftKey;
    private long leftSince;
    private int hoverX = -1;
    private int hoverY = -1;
    private long liftSince;
    private Object settleKey;
    private final int[] settleFrom = new int[4];
    private long settleSince;

    /// A focusable surface that takes every pointer event.
    protected ScrollSurface() {
        setFocusable(true);
        setGrabsPointerEvents(true);
    }

    // ------------------------------------------------------------ content

    /// Lays the surface out for a size: sets the header sizes and whatever
    /// the content's size depends on. Called before every paint and every
    /// pointer event, so it has to return at once when nothing has changed.
    protected abstract void layoutSurface(int width, int height);

    /// The width of what scrolls, in pixels, without the left header.
    protected abstract int getContentWidth();

    /// The height of what scrolls, in pixels, without the top header.
    protected abstract int getContentHeight();

    /// Paints what scrolls. `x` and `y` are where the content's own origin is
    /// on screen, which is off screen once it has scrolled. The graphics is
    /// clipped to the content region.
    protected abstract void paintContent(Graphics g, int x, int y);

    /// Paints the pinned top row. `x` is where the content's origin is; `y`
    /// is the top of the row.
    protected void paintTopHeader(Graphics g, int x, int y) {
    }

    /// Paints the pinned left column. `x` is its left; `y` is where the
    /// content's origin is.
    protected void paintLeftHeader(Graphics g, int x, int y) {
    }

    /// Paints the corner where the two headers meet, at its top left.
    protected void paintCorner(Graphics g, int x, int y) {
    }

    /// Paints over everything, in the component's own bounds, whose top left
    /// is `x`, `y`. Not painted on a printed page: it is for what a gesture in
    /// progress shows.
    protected void paintOverlay(Graphics g, int x, int y) {
    }

    /// Sets the width of the pinned left column and the height of the pinned
    /// top row, in pixels. Called from `layoutSurface`.
    protected final void setHeaderSize(int width, int height) {
        headerWidth = Math.max(0, width);
        headerHeight = Math.max(0, height);
    }

    /// The width of the pinned left column, in pixels.
    public final int getHeaderWidth() {
        return headerWidth;
    }

    /// The height of the pinned top row, in pixels.
    public final int getHeaderHeight() {
        return headerHeight;
    }

    // ---------------------------------------------------------- gestures

    /// The pointer went down at a point, in the component's coordinates.
    /// Whatever it turns into -- a tap, a drag, a long press -- follows.
    protected void pressStarted(int x, int y) {
    }

    /// Asked when a drag or a long press begins, at the point where the
    /// pointer went down, in the component's coordinates.
    ///
    /// #### Parameters
    ///
    /// - `trigger`: what kind of gesture it is
    ///
    /// #### Returns
    ///
    /// true when an edit has begun and the drag belongs to it; false to pan
    protected boolean editStarted(int x, int y, EditTrigger trigger) {
        return false;
    }

    /// The pointer of an edit moved, or the surface scrolled under it.
    protected void editMoved(int x, int y) {
    }

    /// The edit is over. `committed` is false when it was cancelled: by
    /// `cancelEdit`, a second finger, or the surface leaving the form.
    protected void editEnded(boolean committed) {
    }

    /// Whether the surface scrolls under an edit dragged near its edge.
    /// Asked on every move of an edit; true by default.
    protected boolean isEditAutoScroll(int x, int y) {
        return true;
    }

    /// A tap that was not a drag or a long press.
    protected void tapped(int x, int y, boolean doubleTap) {
    }

    /// A long press that started no edit.
    protected void longPressed(int x, int y) {
    }

    /// The scroll position changed.
    protected void scrollChanged() {
    }

    /// Zooms by `factor` around a point, for a pinch or Control/Command and
    /// the wheel.
    ///
    /// #### Returns
    ///
    /// false when the surface has no zoom, so the gesture is left alone
    protected boolean zoomBy(float factor, int x, int y) {
        return false;
    }

    /// Whether a plain drag may start an edit anywhere instead of only on
    /// handles. On by default where there is a mouse, which has a wheel to
    /// scroll with.
    public void setDragEdits(boolean dragEdits) {
        this.dragEdits = dragEdits;
    }

    /// See `setDragEdits`.
    public boolean isDragEdits() {
        return dragEdits;
    }

    /// Cancels an edit in progress, as Escape does; `editEnded(false)`
    /// follows.
    public void cancelEdit() {
        if (gesture == EDIT) {
            gesture = PAN;
            still = true;
            autoX = 0;
            autoY = 0;
            editEnded(false);
            repaint();
        }
    }

    /// Whether an edit is being dragged.
    public boolean isEditInProgress() {
        return gesture == EDIT;
    }

    // ------------------------------------------------------------- scroll

    /// Whether the scroll position stays inside the content; true by default.
    public boolean isBounded() {
        return bounded;
    }

    /// Lets the scroll position go anywhere, for content without edges. The
    /// sizes `getContentWidth` and `getContentHeight` report are then not
    /// used to limit it.
    public void setBounded(boolean bounded) {
        this.bounded = bounded;
    }

    /// The width of the content region, in pixels: the component without the
    /// left header.
    public final int getBodyWidth() {
        return Math.max(0, getWidth() - headerWidth);
    }

    /// The height of the content region, in pixels: the component without the
    /// top header.
    public final int getBodyHeight() {
        return Math.max(0, getHeight() - headerHeight);
    }

    /// The largest horizontal scroll position of bounded content.
    public final int getMaxOffsetX() {
        return Math.max(0, getContentWidth() - getBodyWidth());
    }

    /// The largest vertical scroll position of bounded content.
    public final int getMaxOffsetY() {
        return Math.max(0, getContentHeight() - getBodyHeight());
    }

    /// How far the content has scrolled to the left, in pixels.
    public final int getOffsetX() {
        return offX;
    }

    /// How far the content has scrolled up, in pixels.
    public final int getOffsetY() {
        return offY;
    }

    /// Scrolls to a position, held inside bounded content.
    public void scrollTo(int x, int y) {
        int nx = bounded ? Math.max(0, Math.min(getMaxOffsetX(), x)) : x;
        int ny = bounded ? Math.max(0, Math.min(getMaxOffsetY(), y)) : y;
        if (nx != offX || ny != offY) {
            offX = nx;
            offY = ny;
            scrollChanged();
            repaint();
            if (hoverX >= 0 && !pressed) {
                // What is under a pointer that rests has changed.
                hoverAt(hoverX, hoverY);
            }
        }
    }

    /// Sets the scroll position without holding it inside the content and
    /// without calling `scrollChanged`, for a view whose content moves under
    /// the scroll position (a zoom about a point, a change of date).
    protected final void setOffsets(int x, int y) {
        offX = x;
        offY = y;
    }

    /// Lays out for the current size and holds the scroll position inside
    /// bounded content.
    public void refreshLayout() {
        layoutSurface(getWidth(), getHeight());
        if (!bounded) {
            return;
        }
        int nx = Math.max(0, Math.min(getMaxOffsetX(), offX));
        int ny = Math.max(0, Math.min(getMaxOffsetY(), offY));
        if (nx != offX || ny != offY) {
            offX = nx;
            offY = ny;
            scrollChanged();
        }
    }

    /// Stops a fling in progress.
    protected final void stopFling() {
        flingX = null;
        flingY = null;
    }

    // ---- what the pointer is over, what is held, what was dropped ----

    /// The time the surface animates by: the clock, or the one it was told
    /// to stand still at with `setAnimationTime`.
    protected final long currentTime() {
        return fixedClock >= 0 ? fixedClock : System.currentTimeMillis();
    }

    /// Stops the clock the surface animates hovers and drops by at a time, so
    /// a picture taken in the middle of one is the same every run; a negative
    /// time starts the clock again.
    public void setAnimationTime(long millis) {
        fixedClock = millis;
        repaint();
    }

    /// The pointer rests at a point of the surface without being pressed. A
    /// view answers with `setHoverTarget`, naming what is there and the
    /// cursor that says what a press would do.
    protected void hoverAt(int x, int y) {
        setHoverTarget(null, DEFAULT_CURSOR);
    }

    /// The pointer left, or nothing is known of it any more.
    protected void hoverGone() {
        hoverX = -1;
        hoverY = -1;
        setHoverTarget(null, DEFAULT_CURSOR);
    }

    /// Names what the pointer is over -- any object with a meaningful
    /// `equals`, or null for nothing -- and the cursor to show.
    protected final void setHoverTarget(Object key, int cursor) {
        if (key == null ? hoverKey != null : !key.equals(hoverKey)) {
            long now = currentTime();
            if (hoverKey != null) {
                leftKey = hoverKey;
                leftSince = now;
            }
            if (key != null && key.equals(leftKey)) {
                leftKey = null;
            }
            hoverKey = key;
            hoverSince = now;
            if (key == null) {
                setHoverTooltip(null);
            }
            requestAnimation();
            repaint();
        }
        if (getCursor() != cursor) {
            setCursor(cursor);
        }
    }

    /// The tooltip of the thing the pointer is on, for an application that
    /// shows tooltips; null for none.
    protected final void setHoverTooltip(String text) {
        String was = getTooltip();
        if (text == null ? was != null : !text.equals(was)) {
            setTooltip(text);
        }
    }

    /// What the pointer has just left, while its hover fades.
    public final Object getFadingHoverTarget() {
        return printing ? null : leftKey;
    }

    /// What the pointer is over, as the view named it.
    public final Object getHoverTarget() {
        return hoverKey;
    }

    /// Whether anything is lit by the pointer or still fading: a view that
    /// paints many items asks once before asking `getHoverLevel` for each.
    public final boolean hasHoverFeedback() {
        return !printing && (hoverKey != null || leftKey != null);
    }

    /// How far the hover of `key` has come up, from 0 to 1.
    public final float getHoverLevel(Object key) {
        if (key == null || printing) {
            return 0;
        }
        if (key.equals(hoverKey)) {
            return ease(currentTime() - hoverSince, HOVER_MS);
        }
        if (key.equals(leftKey)) {
            return 1 - ease(currentTime() - leftSince, HOVER_MS);
        }
        return 0;
    }

    /// How far the item being dragged has risen off the surface, from 0 to 1.
    public final float getLiftLevel() {
        return gesture == EDIT ? ease(currentTime() - liftSince, LIFT_MS) : 0;
    }

    /// An item was let go at a rectangle of the content that is not where it
    /// belongs: `applySettle` moves it from there to its place.
    protected final void startSettle(Object key, int x, int y, int w, int h) {
        if (key == null || printing) {
            return;
        }
        settleKey = key;
        settleFrom[0] = x;
        settleFrom[1] = y;
        settleFrom[2] = w;
        settleFrom[3] = h;
        settleSince = currentTime();
        requestAnimation();
    }

    /// Moves the rectangle of an item -- x, y, width, height, in the content
    /// -- to where it is on its way from where it was dropped.
    ///
    /// #### Returns
    ///
    /// true while it travels
    public final boolean applySettle(Object key, int[] rect) {
        if (settleKey == null || printing || !settleKey.equals(key)) {
            return false;
        }
        long t = currentTime() - settleSince;
        if (t >= SETTLE_MS) {
            return false;
        }
        // Fast at first and slow into place, as a thing put down is.
        float p = 1 - t / (float) SETTLE_MS;
        p = 1 - p * p * p;
        for (int i = 0; i < 4; i++) {
            rect[i] = settleFrom[i] + Math.round((rect[i] - settleFrom[i]) * p);
        }
        return true;
    }

    private static float ease(long elapsed, int duration) {
        if (elapsed >= duration) {
            return 1;
        }
        if (elapsed <= 0) {
            return 0;
        }
        float p = elapsed / (float) duration;
        return p * p * (3 - 2 * p);
    }

    // Whether any of the three is still on its way. Never with a clock that
    // stands still: nothing would ever arrive.
    private boolean feedbackBusy() {
        if (fixedClock >= 0) {
            return false;
        }
        long now = System.currentTimeMillis();
        boolean busy = false;
        if (leftKey != null) {
            if (now - leftSince >= HOVER_MS) {
                leftKey = null;
            } else {
                busy = true;
            }
        }
        if (hoverKey != null && now - hoverSince < HOVER_MS) {
            busy = true;
        }
        if (settleKey != null) {
            if (now - settleSince >= SETTLE_MS) {
                settleKey = null;
            } else {
                busy = true;
            }
        }
        if (gesture == EDIT && now - liftSince < LIFT_MS) {
            busy = true;
        }
        return busy;
    }

    @Override
    public void pointerHover(int[] x, int[] y) {
        if (pressed || x == null || x.length == 0) {
            return;
        }
        refreshLayout();
        hoverX = x[0] - getAbsoluteX();
        hoverY = y[0] - getAbsoluteY();
        hoverAt(hoverX, hoverY);
    }

    @Override
    public void setHovered(boolean hovered) {
        super.setHovered(hovered);
        if (!hovered) {
            hoverGone();
        }
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        if (Display.getInstance().isDesktop()) {
            // The cursor says what a press would do: move, stretch, link. A
            // form shows the cursors of its components only when asked.
            Form f = getComponentForm();
            if (f != null && !f.isEnableCursors()) {
                f.setEnableCursors(true);
            }
        }
    }

    // ---------------------------------------------------------- animation

    /// Asks for `animateSurface` to be called on the next frames, for a view
    /// with a motion of its own. Frames stop once nothing moves.
    protected final void requestAnimation() {
        if (!registered) {
            Form f = getComponentForm();
            if (f != null) {
                f.registerAnimated(this);
                registered = true;
            }
        }
    }

    /// Advances the view's own motion. Called every frame while the surface
    /// animates; call `requestAnimation` to start the frames.
    ///
    /// #### Returns
    ///
    /// true while the view still has something moving
    protected boolean animateSurface() {
        return false;
    }

    @Override
    public final boolean animate() {
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
                editMoved(lastX, lastY);
            }
        }
        if (animateSurface()) {
            busy = true;
            repaint();
        }
        if (feedbackBusy()) {
            busy = true;
            repaint();
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
        if (gesture == EDIT) {
            // Taken off the form in the middle of a drag: nothing is
            // committed, and the view does not keep drawing what was dragged.
            editEnded(false);
        }
        gesture = NONE;
        pressed = false;
        still = false;
        pinching = false;
        pinchBase = 0;
        autoX = 0;
        autoY = 0;
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
        refreshLayout();
        int x0 = getX();
        int y0 = getY();
        paintRegions(g, x0, y0, getBodyWidth(), getBodyHeight(), offX, offY);
        int cx = g.getClipX();
        int cy = g.getClipY();
        int cw = g.getClipWidth();
        int ch = g.getClipHeight();
        g.clipRect(x0, y0, getWidth(), getHeight());
        paintOverlay(g, x0, y0);
        g.setClip(cx, cy, cw, ch);
    }

    // The four regions, each in its own clip: the body is bw by bh at the
    // header offsets, scrolled to sx, sy.
    private void paintRegions(Graphics g, int x0, int y0, int bw, int bh, int sx, int sy) {
        int cx = g.getClipX();
        int cy = g.getClipY();
        int cw = g.getClipWidth();
        int ch = g.getClipHeight();
        int bx = x0 + headerWidth;
        int by = y0 + headerHeight;
        g.clipRect(bx, by, bw, bh);
        paintContent(g, bx - sx, by - sy);
        g.setClip(cx, cy, cw, ch);
        if (headerHeight > 0) {
            g.clipRect(bx, y0, bw, headerHeight);
            paintTopHeader(g, bx - sx, y0);
            g.setClip(cx, cy, cw, ch);
        }
        if (headerWidth > 0) {
            g.clipRect(x0, by, headerWidth, bh);
            paintLeftHeader(g, x0, by - sy);
            g.setClip(cx, cy, cw, ch);
        }
        if (headerWidth > 0 && headerHeight > 0) {
            g.clipRect(x0, y0, headerWidth, headerHeight);
            paintCorner(g, x0, y0);
            g.setClip(cx, cy, cw, ch);
        }
    }

    // -------------------------------------------------------------- pages

    /// How the content divides into pages of `width` by `height` pixels, with
    /// the headers repeated on each.
    ///
    /// The view is laid out at the page's size for the count, as it is for
    /// the painting, and put back as it was.
    public PageGrid getPageGrid(int width, int height) {
        int ow = getWidth();
        int oh = getHeight();
        int sx = offX;
        int sy = offY;
        Object zoom = saveZoomState();
        setWidth(width);
        setHeight(height);
        try {
            layoutSurface(width, height);
            int bw = Math.max(1, getBodyWidth());
            int bh = Math.max(1, getPageStep(getBodyHeight()));
            int across = Math.max(1, (getContentWidth() + bw - 1) / bw);
            int down = Math.max(1, (getContentHeight() + bh - 1) / bh);
            return new PageGrid(across, down, bw, bh);
        } finally {
            setWidth(ow);
            setHeight(oh);
            backToScreen(ow, oh, zoom);
            offX = sx;
            offY = sy;
        }
    }

    /// Paints one page: the content from `contentX`, `contentY` on, under
    /// the headers that belong to it, at `x`, `y` in a page of `width` by
    /// `height`.
    ///
    /// This is the view's own painting at another size and scroll position,
    /// so what prints is what the screen shows. The overlay is left out. The
    /// content is clipped to its end, so the last page of a row or column ends
    /// where the content does.
    public void paintPage(Graphics g, int x, int y, int width, int height, int contentX, int contentY) {
        int ox = getX();
        int oy = getY();
        int ow = getWidth();
        int oh = getHeight();
        int sx = offX;
        int sy = offY;
        int cx = g.getClipX();
        int cy = g.getClipY();
        int cw = g.getClipWidth();
        int ch = g.getClipHeight();
        Object zoom = saveZoomState();
        setX(x);
        setY(y);
        setWidth(width);
        setHeight(height);
        printing = true;
        try {
            layoutSurface(width, height);
            offX = contentX;
            offY = contentY;
            int bw = Math.min(getBodyWidth(), Math.max(0, getContentWidth() - contentX));
            int bh = Math.min(getPageStep(getBodyHeight()), Math.max(0, getContentHeight() - contentY));
            paintRegions(g, x, y, bw, bh, contentX, contentY);
        } finally {
            printing = false;
            g.setClip(cx, cy, cw, ch);
            setX(ox);
            setY(oy);
            setWidth(ow);
            setHeight(oh);
            backToScreen(ow, oh, zoom);
            offX = sx;
            offY = sy;
        }
    }

    /// Whether a page is being painted and not the screen.
    protected final boolean isPrinting() {
        return printing;
    }

    /// The zoom a layout at another size may change, as an object the view
    /// alone reads: a view that holds its scale inside limits depending on
    /// the room it has may move it when laid out for a page, and does not
    /// move it back when laid out for the screen again. Null for a view
    /// without a zoom.
    protected Object saveZoomState() {
        return null;
    }

    /// Puts back what `saveZoomState` returned. Called with the view laid out
    /// at its own size again.
    protected void restoreZoomState(Object state) {
    }

    /// How far down a page of content height `bodyHeight` goes before the
    /// next one starts. A view of rows answers a whole number of them, so no
    /// page ends in the middle of one.
    protected int getPageStep(int bodyHeight) {
        return bodyHeight;
    }

    private void backToScreen(int w, int h, Object zoom) {
        layoutSurface(w, h);
        if (zoom != null) {
            restoreZoomState(zoom);
            layoutSurface(w, h);
        }
    }

    @Override
    protected Dimension calcPreferredSize() {
        // A surface is given its size by the layout around it; this is what it
        // asks for when nothing constrains it.
        int side = mm(60);
        return new Dimension(side, side);
    }

    private static int mm(float mm) {
        return Math.max(1, Display.getInstance().convertToPixels(mm));
    }

    // ------------------------------------------------------------ pointer

    @Override
    public void pointerPressed(int x, int y) {
        super.pointerPressed(x, y);
        if (gesture == EDIT) {
            // The release of the last drag went somewhere else: a listener
            // showed a dialog over this while it was held.
            editEnded(false);
        }
        // A pinch whose end went elsewhere is over as well.
        pinching = false;
        pinchBase = 0;
        still = false;
        refreshLayout();
        stopFling();
        pressed = true;
        gesture = NONE;
        autoX = 0;
        autoY = 0;
        pressX = x - getAbsoluteX();
        pressY = y - getAbsoluteY();
        lastX = pressX;
        lastY = pressY;
        pressStarted(pressX, pressY);
    }

    @Override
    public void pointerDragged(int x, int y) {
        if (!pressed || pinching) {
            return;
        }
        int lx = x - getAbsoluteX();
        int ly = y - getAbsoluteY();
        if (gesture == NONE) {
            int slop = mm(1.5f);
            if (Math.abs(lx - pressX) < slop && Math.abs(ly - pressY) < slop) {
                return;
            }
            EditTrigger trigger = dragEdits ? EditTrigger.DRAG : EditTrigger.HANDLE_DRAG;
            gesture = editStarted(pressX, pressY, trigger) ? EDIT : PAN;
            if (gesture == EDIT) {
                liftSince = currentTime();
                requestAnimation();
            }
        }
        if (gesture == EDIT) {
            lastX = lx;
            lastY = ly;
            editMoved(lx, ly);
            autoScroll(lx, ly);
            repaint();
        } else {
            scrollTo(offX - (lx - lastX), offY - (ly - lastY));
            lastX = lx;
            lastY = ly;
        }
    }

    @Override
    public void longPointerPress(int x, int y) {
        if (!pressed || gesture != NONE) {
            return;
        }
        int lx = x - getAbsoluteX();
        int ly = y - getAbsoluteY();
        if (editStarted(lx, ly, EditTrigger.LONG_PRESS)) {
            gesture = EDIT;
            liftSince = currentTime();
            requestAnimation();
            lastX = lx;
            lastY = ly;
            editMoved(lx, ly);
            repaint();
        } else {
            // A press this long is not a tap, whatever follows, and the
            // finger's speed on release is not a fling.
            gesture = PAN;
            still = true;
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
        boolean rest = still;
        still = false;
        if (g == EDIT) {
            editEnded(true);
            repaint();
        } else if (g == PAN) {
            // A pinch or a cancelled edit ends where it is: the speed of the
            // finger was not a scroll.
            if (!rest) {
                fling();
            }
        } else {
            int lx = x - getAbsoluteX();
            int ly = y - getAbsoluteY();
            long now = System.currentTimeMillis();
            int near = mm(4);
            boolean twice = now - lastTapTime < DOUBLE_TAP_MS && Math.abs(lx - lastTapX) < near
                    && Math.abs(ly - lastTapY) < near;
            lastTapTime = twice ? 0 : now;
            lastTapX = lx;
            lastTapY = ly;
            tapped(lx, ly, twice);
        }
    }

    private void fling() {
        float vx = Display.getInstance().getDragSpeed(false);
        float vy = Display.getInstance().getDragSpeed(true);
        boolean any = false;
        if (Math.abs(vx) > 0.1f && (!bounded || getMaxOffsetX() > 0)) {
            flingX = Motion.createFrictionMotion(offX, flingEnd(vx, offX, getMaxOffsetX()), vx, 0.0007f);
            flingX.start();
            any = true;
        }
        if (Math.abs(vy) > 0.1f && (!bounded || getMaxOffsetY() > 0)) {
            flingY = Motion.createFrictionMotion(offY, flingEnd(vy, offY, getMaxOffsetY()), vy, 0.0007f);
            flingY.start();
            any = true;
        }
        if (any) {
            requestAnimation();
        }
    }

    private int flingEnd(float v, int from, int max) {
        if (bounded) {
            return v < 0 ? 0 : max;
        }
        // long math: an unbounded offset that has travelled far must not
        // wrap past the int range and fling the other way
        long end = v < 0 ? (long) from - FAR : (long) from + FAR;
        return (int) Math.max(Integer.MIN_VALUE + 1L, Math.min(Integer.MAX_VALUE - 1L, end));
    }

    // While an edit is dragged near an edge the surface scrolls under it, and
    // faster the nearer the edge.
    private void autoScroll(int lx, int ly) {
        if (!isEditAutoScroll(lx, ly)) {
            autoX = 0;
            autoY = 0;
            return;
        }
        int band = mm(6);
        int step = mm(2.5f);
        autoX = edgeSpeed(lx, headerWidth, getWidth(), band, step);
        autoY = edgeSpeed(ly, headerHeight, getHeight(), band, step);
        if (autoX != 0 || autoY != 0) {
            requestAnimation();
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
        refreshLayout();
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
            if (gesture == EDIT) {
                // A second finger on a drag makes it a zoom; the drag is
                // dropped, not left half made.
                autoX = 0;
                autoY = 0;
                editEnded(false);
            }
            gesture = PAN;
            still = true;
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
