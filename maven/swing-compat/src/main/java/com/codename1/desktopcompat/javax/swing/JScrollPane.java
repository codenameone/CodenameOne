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

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.Point;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.javax.swing.border.Border;
import com.codename1.desktopcompat.javax.swing.event.ChangeEvent;
import com.codename1.desktopcompat.javax.swing.event.ChangeListener;
import java.util.HashMap;

/// Shows a component larger than itself and lets the user scroll it.
///
/// The view sits in a [JViewport] whose peer is a scrollable Codename One
/// container, so the user scrolls by dragging and flinging the content --
/// or with the mouse wheel on a desktop -- and Codename One draws its own
/// scroll indicator. The two scroll bars exist and their models follow
/// the view (`getVerticalScrollBar().setValue(n)` scrolls, and a listener
/// on the bar hears the user scrolling), but they take no room and are not
/// drawn. The policy "never" stops the user scrolling that axis;
/// "always" and "as needed" are the same. Row and column header views are
/// shown and follow the view; corners are recorded only.
public class JScrollPane extends JComponent implements ScrollPaneConstants {

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
        setVerticalScrollBarPolicy(vsbPolicy);
        setHorizontalScrollBarPolicy(hsbPolicy);
        setViewport(createViewport());
        setVerticalScrollBar(createVerticalScrollBar());
        setHorizontalScrollBar(createHorizontalScrollBar());
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
        }
        this.verticalScrollBar = verticalScrollBar;
        if (verticalScrollBar != null) {
            verticalScrollBar.getModel().addChangeListener(follow);
        }
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
        }
        this.horizontalScrollBar = horizontalScrollBar;
        if (horizontalScrollBar != null) {
            horizontalScrollBar.getModel().addChangeListener(follow);
        }
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

    /// Recorded only: with no scroll bars there are no corners to fill.
    public void setCorner(String key, Component corner) {
        if (corners == null) {
            corners = new HashMap<String, Component>();
        }
        Component old = corners.put(key, corner);
        firePropertyChange(key, old, corner);
    }

    public boolean isWheelScrollingEnabled() {
        return wheelScrollState;
    }

    /// Recorded only; the wheel scrolls the content natively.
    public void setWheelScrollingEnabled(boolean handleWheel) {
        boolean old = wheelScrollState;
        wheelScrollState = handleWheel;
        firePropertyChange("wheelScrollingEnabled", old, handleWheel);
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

    @Override
    public void doLayout() {
        Insets in = space();
        int x = in.left;
        int y = in.top;
        int w = Math.max(0, getWidth() - in.left - in.right);
        int h = Math.max(0, getHeight() - in.top - in.bottom);
        int headH = has(columnHeader) ? Math.min(h, columnHeader.getPreferredSize().height) : 0;
        int headW = has(rowHeader) ? Math.min(w, rowHeader.getPreferredSize().width) : 0;
        if (columnHeader != null) {
            columnHeader.setBounds(x + headW, y, w - headW, headH);
        }
        if (rowHeader != null) {
            rowHeader.setBounds(x, y + headH, headW, h - headH);
        }
        if (viewport != null) {
            viewport.setBounds(x + headW, y + headH, w - headW, h - headH);
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
