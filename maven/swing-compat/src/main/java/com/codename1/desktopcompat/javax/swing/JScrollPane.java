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
package com.codename1.desktopcompat.javax.swing;

import com.codename1.desktopcompat.java.awt.LayoutManager;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.Point;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.event.MouseWheelEvent;
import com.codename1.desktopcompat.javax.accessibility.Accessible;
import com.codename1.desktopcompat.javax.swing.border.Border;
import com.codename1.desktopcompat.javax.swing.event.ChangeEvent;
import com.codename1.desktopcompat.javax.swing.event.ChangeListener;
import com.codename1.desktopcompat.rt.ScrollDelegate;
import java.util.HashMap;

/// Shows a component larger than itself and lets the user scroll it.
///
/// The view sits in a [JViewport] whose peer is a scrollable Codename One
/// container, so the user scrolls by dragging and flinging the content --
/// or with the mouse wheel on a desktop.
///
/// The two scroll bars are children of the pane and their models follow
/// the view: `getVerticalScrollBar().setValue(n)` scrolls, and a listener
/// on the bar hears the user scrolling. Where they are shown depends on
/// the device:
///
///  - On a desktop port they take room beside the viewport and are dragged
///    with the mouse. "As needed" shows a bar while the view is larger
///    than the viewport, "always" shows it whatever the size of the view.
///  - On a touch device they take no room and have no size. The user
///    drags the content, and Codename One draws its own thin indicator
///    over it while it moves.
///
/// The policy "never" stops the user scrolling that axis everywhere. Row
/// and column header views are shown and follow the view. A corner
/// component is shown in the corner it was set for whenever that corner
/// exists; the upper trailing corner is also shown with no scroll bar
/// below it, at its preferred width beside a column header, so that the
/// column control of a table has a place on a touch device too.
///
/// A wheel movement scrolls the viewport natively, once. It is delivered
/// as a `MouseWheelEvent` first to the nearest component under the
/// pointer that has a `MouseWheelListener`, and when there is one the
/// native scrolling does not happen -- as on the desktop, where a wheel
/// listener on the view replaces the pane's scrolling. A listener that
/// only wants some of the events passes the others on with
/// `getParent().dispatchEvent(e)`; a wheel event that reaches this pane
/// that way scrolls it.
public class JScrollPane extends JComponent implements Accessible, ScrollPaneConstants {

    protected int verticalScrollBarPolicy = VERTICAL_SCROLLBAR_AS_NEEDED;

    protected int horizontalScrollBarPolicy = HORIZONTAL_SCROLLBAR_AS_NEEDED;

    protected JViewport viewport;

    protected JScrollBar verticalScrollBar;

    protected JScrollBar horizontalScrollBar;

    protected JViewport rowHeader;

    protected JViewport columnHeader;

    private final Follow follow = new Follow();
    private HashMap<String, Component> corners;
    private Border viewportBorder;
    private boolean wheelScrollState = true;
    private boolean syncing;

    public JScrollPane(Component view, int vsbPolicy, int hsbPolicy) {
        setLayout(new ScrollPaneLayout.UIResource());
        setVerticalScrollBarPolicy(vsbPolicy);
        setHorizontalScrollBarPolicy(hsbPolicy);
        setViewport(createViewport());
        setVerticalScrollBar(createVerticalScrollBar());
        setHorizontalScrollBar(createHorizontalScrollBar());
        setBorder(UIManager.getBorder("ScrollPane.border"));
        if (view != null) {
            setViewportView(view);
        }
    }

    public JScrollPane(Component view) {
        this(view, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    public JScrollPane(int vsbPolicy, int hsbPolicy) {
        this(null, vsbPolicy, hsbPolicy);
    }

    public JScrollPane() {
        this(null, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    @Override
    public boolean isValidateRoot() {
        return true;
    }

    // ------------------------------------------------------------ policies

    public int getVerticalScrollBarPolicy() {
        return verticalScrollBarPolicy;
    }

    public void setVerticalScrollBarPolicy(int policy) {
        switch (policy) {
            case VERTICAL_SCROLLBAR_AS_NEEDED:
            case VERTICAL_SCROLLBAR_NEVER:
            case VERTICAL_SCROLLBAR_ALWAYS:
                break;
            default:
                throw new IllegalArgumentException("invalid verticalScrollBarPolicy");
        }
        int old = verticalScrollBarPolicy;
        verticalScrollBarPolicy = policy;
        firePropertyChange("verticalScrollBarPolicy", old, policy);
        applyPolicies();
        if (old != policy) {
            revalidate();
        }
    }

    public int getHorizontalScrollBarPolicy() {
        return horizontalScrollBarPolicy;
    }

    public void setHorizontalScrollBarPolicy(int policy) {
        switch (policy) {
            case HORIZONTAL_SCROLLBAR_AS_NEEDED:
            case HORIZONTAL_SCROLLBAR_NEVER:
            case HORIZONTAL_SCROLLBAR_ALWAYS:
                break;
            default:
                throw new IllegalArgumentException("invalid horizontalScrollBarPolicy");
        }
        int old = horizontalScrollBarPolicy;
        horizontalScrollBarPolicy = policy;
        firePropertyChange("horizontalScrollBarPolicy", old, policy);
        applyPolicies();
        if (old != policy) {
            revalidate();
        }
    }

    private void applyPolicies() {
        if (viewport != null) {
            viewport.cn1SetScrollable(horizontalScrollBarPolicy != HORIZONTAL_SCROLLBAR_NEVER,
                    verticalScrollBarPolicy != VERTICAL_SCROLLBAR_NEVER);
        }
    }

    // ------------------------------------------------------------ viewport

    protected JViewport createViewport() {
        return new JViewport();
    }

    public JViewport getViewport() {
        return viewport;
    }

    public void setViewport(JViewport viewport) {
        JViewport old = this.viewport;
        if (old != null) {
            old.removeChangeListener(follow);
            remove(old);
        }
        this.viewport = viewport;
        if (viewport != null) {
            add(viewport);
            viewport.addChangeListener(follow);
            applyPolicies();
        }
        firePropertyChange("viewport", old, viewport);
        revalidate();
        repaint();
    }

    public void setViewportView(Component view) {
        if (getViewport() == null) {
            setViewport(createViewport());
        }
        getViewport().setView(view);
    }

    public Border getViewportBorder() {
        return viewportBorder;
    }

    /// Recorded, and its insets are left free around the viewport; the
    /// border itself is not drawn.
    public void setViewportBorder(Border viewportBorder) {
        Border old = this.viewportBorder;
        this.viewportBorder = viewportBorder;
        firePropertyChange("viewportBorder", old, viewportBorder);
        revalidate();
    }

    public Rectangle getViewportBorderBounds() {
        Insets insets = getInsets();
        Rectangle r = new Rectangle(insets.left, insets.top, getWidth() - insets.left - insets.right,
                getHeight() - insets.top - insets.bottom);
        if (columnHeader != null && columnHeader.getView() != null) {
            int h = columnHeader.getHeight();
            r.y += h;
            r.height -= h;
        }
        if (rowHeader != null && rowHeader.getView() != null) {
            int w = rowHeader.getWidth();
            r.x += w;
            r.width -= w;
        }
        return r;
    }

    // ------------------------------------------------------------ bars

    public JScrollBar createVerticalScrollBar() {
        return new JScrollBar(JScrollBar.VERTICAL);
    }

    public JScrollBar createHorizontalScrollBar() {
        return new JScrollBar(JScrollBar.HORIZONTAL);
    }

    public JScrollBar getVerticalScrollBar() {
        return verticalScrollBar;
    }

    public void setVerticalScrollBar(JScrollBar verticalScrollBar) {
        JScrollBar old = this.verticalScrollBar;
        if (old != null) {
            old.getModel().removeChangeListener(follow);
            remove(old);
        }
        this.verticalScrollBar = verticalScrollBar;
        if (verticalScrollBar != null) {
            verticalScrollBar.getModel().addChangeListener(follow);
            add(verticalScrollBar);
        }
        revalidate();
        firePropertyChange("verticalScrollBar", old, verticalScrollBar);
        syncBars();
    }

    public JScrollBar getHorizontalScrollBar() {
        return horizontalScrollBar;
    }

    public void setHorizontalScrollBar(JScrollBar horizontalScrollBar) {
        JScrollBar old = this.horizontalScrollBar;
        if (old != null) {
            old.getModel().removeChangeListener(follow);
            remove(old);
        }
        this.horizontalScrollBar = horizontalScrollBar;
        if (horizontalScrollBar != null) {
            horizontalScrollBar.getModel().addChangeListener(follow);
            add(horizontalScrollBar);
        }
        revalidate();
        firePropertyChange("horizontalScrollBar", old, horizontalScrollBar);
        syncBars();
    }

    /// Writes the viewport's position, extent and view size into the
    /// models of the bars, and moves the headers along.
    private void syncBars() {
        if (viewport == null || syncing) {
            return;
        }
        syncing = true;
        try {
            Point p = viewport.getViewPosition();
            Dimension extent = viewport.getExtentSize();
            Component view = viewport.getView();
            Dimension size = view == null ? new Dimension(0, 0) : view.getSize();
            if (verticalScrollBar != null) {
                int e = Math.min(extent.height, size.height);
                verticalScrollBar.getModel().setRangeProperties(Math.min(p.y, Math.max(0, size.height - e)), e, 0,
                        size.height, verticalScrollBar.getModel().getValueIsAdjusting());
                int unit = unit(view, extent, SwingConstants.VERTICAL);
                verticalScrollBar.unitIncrement = unit;
                verticalScrollBar.blockIncrement = Math.max(1, extent.height);
            }
            if (horizontalScrollBar != null) {
                int e = Math.min(extent.width, size.width);
                horizontalScrollBar.getModel().setRangeProperties(Math.min(p.x, Math.max(0, size.width - e)), e, 0,
                        size.width, horizontalScrollBar.getModel().getValueIsAdjusting());
                horizontalScrollBar.unitIncrement = unit(view, extent, SwingConstants.HORIZONTAL);
                horizontalScrollBar.blockIncrement = Math.max(1, extent.width);
            }
            if (columnHeader != null) {
                columnHeader.setViewPosition(new Point(p.x, 0));
            }
            if (rowHeader != null) {
                rowHeader.setViewPosition(new Point(0, p.y));
            }
        } finally {
            syncing = false;
        }
    }

    private int unit(Component view, Dimension extent, int orientation) {
        if (view instanceof Scrollable && viewport != null) {
            return Math.max(1, ((Scrollable) view).getScrollableUnitIncrement(viewport.getViewRect(), orientation, 1));
        }
        return Math.max(1, (orientation == SwingConstants.VERTICAL ? extent.height : extent.width) / 10);
    }

    private final class Follow implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            if (syncing || viewport == null) {
                return;
            }
            if (e.getSource() == viewport) {
                syncBars();
                return;
            }
            Point p = viewport.getViewPosition();
            int x = horizontalScrollBar == null ? p.x : horizontalScrollBar.getValue();
            int y = verticalScrollBar == null ? p.y : verticalScrollBar.getValue();
            if (x != p.x || y != p.y) {
                viewport.setViewPosition(new Point(x, y));
            }
        }
    }

    // ------------------------------------------------------------ headers

    public JViewport getRowHeader() {
        return rowHeader;
    }

    public void setRowHeader(JViewport rowHeader) {
        JViewport old = this.rowHeader;
        if (old != null) {
            remove(old);
        }
        this.rowHeader = rowHeader;
        if (rowHeader != null) {
            rowHeader.cn1SetNativeScrolling(false);
            add(rowHeader);
        }
        firePropertyChange("rowHeader", old, rowHeader);
        revalidate();
        repaint();
    }

    public void setRowHeaderView(Component view) {
        if (getRowHeader() == null) {
            setRowHeader(createViewport());
        }
        getRowHeader().setView(view);
    }

    public JViewport getColumnHeader() {
        return columnHeader;
    }

    public void setColumnHeader(JViewport columnHeader) {
        JViewport old = this.columnHeader;
        if (old != null) {
            remove(old);
        }
        this.columnHeader = columnHeader;
        if (columnHeader != null) {
            columnHeader.cn1SetNativeScrolling(false);
            add(columnHeader);
        }
        firePropertyChange("columnHeader", old, columnHeader);
        revalidate();
        repaint();
    }

    public void setColumnHeaderView(Component view) {
        if (getColumnHeader() == null) {
            setColumnHeader(createViewport());
        }
        getColumnHeader().setView(view);
    }

    public Component getCorner(String key) {
        return corners == null ? null : corners.get(key);
    }

    /// Puts a component in one of the four corners; `null` empties it.
    /// See the class description for when a corner has room.
    public void setCorner(String key, Component corner) {
        if (!LOWER_LEFT_CORNER.equals(key) && !LOWER_RIGHT_CORNER.equals(key) && !UPPER_LEFT_CORNER.equals(key)
                && !UPPER_RIGHT_CORNER.equals(key) && !LOWER_LEADING_CORNER.equals(key)
                && !LOWER_TRAILING_CORNER.equals(key) && !UPPER_LEADING_CORNER.equals(key)
                && !UPPER_TRAILING_CORNER.equals(key)) {
            throw new IllegalArgumentException("invalid corner key");
        }
        if (corners == null) {
            corners = new HashMap<String, Component>();
        }
        Component old = corner == null ? corners.remove(key) : corners.put(key, corner);
        if (old != null && old != corner) {
            remove(old);
        }
        if (corner != null && old != corner) {
            add(corner);
        }
        firePropertyChange(key, old, corner);
        revalidate();
        repaint();
    }

    /// The component of a corner, by either of its two names: the layer
    /// lays out left to right, so leading is left and trailing is right.
    private Component corner(String side, String edge) {
        if (corners == null) {
            return null;
        }
        Component c = corners.get(side);
        return c != null ? c : corners.get(edge);
    }

    public boolean isWheelScrollingEnabled() {
        return wheelScrollState;
    }

    /// With `false` a wheel movement over the pane scrolls nothing.
    public void setWheelScrollingEnabled(boolean handleWheel) {
        boolean old = wheelScrollState;
        wheelScrollState = handleWheel;
        firePropertyChange("wheelScrollingEnabled", old, handleWheel);
    }

    /// Scrolls for a wheel event that was sent to the pane itself: one
    /// that a listener on the view passed on, or one that arrived because
    /// the pane has a wheel listener of its own. The wheel over a pane
    /// nobody listens on never comes here; Codename One scrolls for it.
    @Override
    protected void processMouseWheelEvent(MouseWheelEvent e) {
        super.processMouseWheelEvent(e);
        if (e.isConsumed() || !wheelScrollState || !isEnabled() || e.getWheelRotation() == 0) {
            return;
        }
        JScrollBar bar = verticalScrollBar;
        boolean vertical = bar != null && verticalScrollBarPolicy != VERTICAL_SCROLLBAR_NEVER && !e.isShiftDown()
                && bar.getMaximum() - bar.getVisibleAmount() > bar.getMinimum();
        if (!vertical) {
            bar = horizontalScrollBarPolicy == HORIZONTAL_SCROLLBAR_NEVER ? null : horizontalScrollBar;
        }
        if (bar == null) {
            return;
        }
        int direction = e.getWheelRotation() < 0 ? -1 : 1;
        int step;
        if (e.getScrollType() == MouseWheelEvent.WHEEL_BLOCK_SCROLL) {
            step = bar.getBlockIncrement(direction) * direction;
        } else {
            step = bar.getUnitIncrement(direction) * e.getUnitsToScroll();
        }
        bar.setValue(bar.getValue() + step);
        e.consume();
    }

    // ------------------------------------------------------------ layout

    private Insets space() {
        Insets i = getInsets();
        Insets out = new Insets(i.top, i.left, i.bottom, i.right);
        if (viewportBorder != null) {
            Insets b = viewportBorder.getBorderInsets(this);
            out.top += b.top;
            out.left += b.left;
            out.bottom += b.bottom;
            out.right += b.right;
        }
        return out;
    }

    private static boolean has(JViewport v) {
        return v != null && v.isVisible() && v.getView() != null;
    }

    /// Which bars to show, as `{vertical, horizontal}`, for bars `t`
    /// thick. `laidOut` reads the size the view has now instead of the
    /// one it would like, which is the only way to know how high a view
    /// is whose height follows its width.
    private boolean[] barsFor(int t, boolean laidOut) {
        if (t <= 0) {
            return new boolean[]{false, false};
        }
        boolean v = verticalScrollBarPolicy == VERTICAL_SCROLLBAR_ALWAYS;
        boolean h = horizontalScrollBarPolicy == HORIZONTAL_SCROLLBAR_ALWAYS;
        Component view = viewport == null ? null : viewport.getView();
        if (view == null) {
            return new boolean[]{v, h};
        }
        Insets in = space();
        int availW = Math.max(0, getWidth() - in.left - in.right)
                - (has(rowHeader) ? rowHeader.getPreferredSize().width : 0);
        int availH = Math.max(0, getHeight() - in.top - in.bottom)
                - (has(columnHeader) ? columnHeader.getPreferredSize().height : 0);
        boolean tracksW = false;
        boolean tracksH = false;
        if (view instanceof Scrollable) {
            tracksW = ((Scrollable) view).getScrollableTracksViewportWidth();
            tracksH = ((Scrollable) view).getScrollableTracksViewportHeight();
        }
        Dimension size = laidOut ? view.getSize() : view.getPreferredSize();
        boolean vAsNeeded = verticalScrollBarPolicy == VERTICAL_SCROLLBAR_AS_NEEDED && !tracksH;
        boolean hAsNeeded = horizontalScrollBarPolicy == HORIZONTAL_SCROLLBAR_AS_NEEDED && !tracksW;
        if (vAsNeeded) {
            v = size.height > availH;
        }
        if (hAsNeeded) {
            h = size.width > availW - (v ? t : 0);
        }
        if (vAsNeeded && !v && h) {
            v = size.height > availH - t;
        }
        return new boolean[]{v, h};
    }

    private static void bounds(Component c, int x, int y, int w, int h) {
        if (c != null) {
            c.setBounds(x, y, Math.max(0, w), Math.max(0, h));
        }
    }

    /// A bar that has no room is not visible, which is how a layout of
    /// the application's knows whether there is one.
    private static void shown(JScrollBar bar, boolean visible) {
        if (bar != null && bar.isVisible() != visible) {
            bar.setVisible(visible);
        }
    }

    private void place(boolean vsb, boolean hsb, int t) {
        Insets in = space();
        int x = in.left;
        int y = in.top;
        int w = Math.max(0, getWidth() - in.left - in.right);
        int h = Math.max(0, getHeight() - in.top - in.bottom);
        int headH = has(columnHeader) ? Math.min(h, columnHeader.getPreferredSize().height) : 0;
        int headW = has(rowHeader) ? Math.min(w, rowHeader.getPreferredSize().width) : 0;
        int barW = vsb ? Math.min(t, w - headW) : 0;
        int barH = hsb ? Math.min(t, h - headH) : 0;
        Component upperRight = corner(UPPER_RIGHT_CORNER, UPPER_TRAILING_CORNER);
        int cornerW = barW;
        if (barW == 0 && upperRight != null && headH > 0) {
            // No bar to sit above: the corner takes its room from the end
            // of the column header instead.
            cornerW = Math.max(0, Math.min(upperRight.getPreferredSize().width, (w - headW) / 2));
        }
        bounds(columnHeader, x + headW, y, w - headW - cornerW, headH);
        bounds(rowHeader, x, y + headH, headW, h - headH - barH);
        bounds(viewport, x + headW, y + headH, w - headW - barW, h - headH - barH);
        bounds(verticalScrollBar, x + w - barW, y + headH, barW, barW == 0 ? 0 : h - headH - barH);
        bounds(horizontalScrollBar, x + headW, y + h - barH, barH == 0 ? 0 : w - headW - barW, barH);
        shown(verticalScrollBar, barW > 0);
        shown(horizontalScrollBar, barH > 0);
        bounds(upperRight, x + w - cornerW, y, cornerW, cornerW == 0 ? 0 : headH);
        bounds(corner(UPPER_LEFT_CORNER, UPPER_LEADING_CORNER), x, y, headW, headW == 0 ? 0 : headH);
        bounds(corner(LOWER_LEFT_CORNER, LOWER_LEADING_CORNER), x, y + h - barH, headW, headW == 0 ? 0 : barH);
        bounds(corner(LOWER_RIGHT_CORNER, LOWER_TRAILING_CORNER), x + w - barW, y + h - barH, barW,
                barW == 0 ? 0 : barH);
    }

    /// Sets the layout, which has to be a [ScrollPaneLayout].
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: if `layout` is neither `null` nor a
    ///   `ScrollPaneLayout`
    @Override
    public void setLayout(LayoutManager layout) {
        if (layout != null && !(layout instanceof ScrollPaneLayout)) {
            throw new ClassCastException("layout of JScrollPane must be a ScrollPaneLayout");
        }
        super.setLayout(layout);
        cn1SyncLayout();
    }

    /// Tells the layout of the parts the pane has now.
    private void cn1SyncLayout() {
        LayoutManager l = getLayout();
        if (l instanceof ScrollPaneLayout) {
            ((ScrollPaneLayout) l).syncWithScrollPane(this);
        }
    }

    @Override
    public void revalidate() {
        // Every change of a part comes through here.
        cn1SyncLayout();
        super.revalidate();
    }

    @Override
    public void doLayout() {
        LayoutManager l = getLayout();
        if (l instanceof ScrollPaneLayout) {
            l.layoutContainer(this);
        } else {
            cn1Layout();
        }
    }

    /// The standard placement, which is what `ScrollPaneLayout` does.
    void cn1Layout() {
        int t = ScrollDelegate.barThickness();
        boolean[] bars = barsFor(t, false);
        place(bars[0], bars[1], t);
        if (t > 0 && viewport != null && viewport.getView() != null) {
            // The view is laid out now, so that a view whose height follows
            // its width is measured at the width it really got.
            viewport.validate();
            boolean[] measured = barsFor(t, true);
            if (measured[0] != bars[0] || measured[1] != bars[1]) {
                place(measured[0], measured[1], t);
                viewport.validate();
            }
        }
    }

    @Override
    protected void validateTree() {
        super.validateTree();
        syncBars();
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        LayoutManager l = getLayout();
        return l instanceof ScrollPaneLayout ? l.preferredLayoutSize(this) : cn1PreferredSize();
    }

    Dimension cn1PreferredSize() {
        Insets in = space();
        Dimension d = viewport == null ? new Dimension(0, 0) : viewport.getPreferredSize();
        int w = d.width;
        int h = d.height;
        if (has(columnHeader)) {
            h += columnHeader.getPreferredSize().height;
        }
        if (has(rowHeader)) {
            w += rowHeader.getPreferredSize().width;
        }
        return new Dimension(w + in.left + in.right, h + in.top + in.bottom);
    }

    @Override
    public Dimension getMinimumSize() {
        if (isMinimumSizeSet()) {
            return super.getMinimumSize();
        }
        LayoutManager l = getLayout();
        return l instanceof ScrollPaneLayout ? l.minimumLayoutSize(this) : cn1MinimumSize();
    }

    Dimension cn1MinimumSize() {
        Insets in = space();
        int w = 4;
        int h = 4;
        if (has(columnHeader)) {
            h += columnHeader.getPreferredSize().height;
        }
        if (has(rowHeader)) {
            w += rowHeader.getPreferredSize().width;
        }
        return new Dimension(w + in.left + in.right, h + in.top + in.bottom);
    }

    @Override
    public Dimension getMaximumSize() {
        return isMaximumSizeSet() ? super.getMaximumSize() : new Dimension(Short.MAX_VALUE, Short.MAX_VALUE);
    }

    @Override
    protected String paramString() {
        return super.paramString() + ",verticalScrollBarPolicy=" + verticalScrollBarPolicy
                + ",horizontalScrollBarPolicy=" + horizontalScrollBarPolicy;
    }
}
