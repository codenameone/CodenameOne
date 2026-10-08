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

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.Point;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.javax.swing.event.ChangeEvent;
import com.codename1.desktopcompat.javax.swing.event.ChangeListener;
import com.codename1.desktopcompat.rt.Units;
import com.codename1.desktopcompat.rt.ViewportPeer;

/// The window a scroll pane looks at its view through.
///
/// The view position, the extent and the view size are logical pixels, as
/// everywhere in this layer, and as on the desktop the view's location is
/// the negated view position, so hit testing and `getLocationOnScreen`
/// see the view where it is drawn. The scrolling itself is Codename
/// One's: the viewport's peer is a scrollable container, the user drags
/// and flings it natively, and each native movement arrives here as a
/// change of the view position with a change event.
///
/// A view that is not [Scrollable] is stretched to fill a larger
/// viewport; one that is follows what its `getScrollableTracks...`
/// methods answer. Scroll modes and the backing store are recorded only.
public class JViewport extends JComponent {

    public static final int BLIT_SCROLL_MODE = 1;

    public static final int BACKINGSTORE_SCROLL_MODE = 2;

    public static final int SIMPLE_SCROLL_MODE = 0;

    protected boolean isViewSizeSet;

    protected boolean scrollUnderway;

    private transient ChangeEvent changeEvent;
    private int scrollMode = BLIT_SCROLL_MODE;
    private int viewX;
    private int viewY;
    private boolean nativeScrolling = true;
    private boolean scrollableX = true;
    private boolean scrollableY = true;
    private boolean layingOut;

    public JViewport() {
        setOpaque(true);
    }

    // ------------------------------------------------------------ peer

    @Override
    protected com.codename1.ui.Component cn1CreatePeer() {
        return new ViewportPeer(this, new ViewportPeer.Listener() {
            @Override
            public void scrolled(int x, int y) {
                cn1NativeScrolled(x, y);
            }
        });
    }

    @Override
    protected void cn1PeerCreated() {
        super.cn1PeerCreated();
        applyScrollable();
    }

    private void applyScrollable() {
        com.codename1.ui.Component p = cn1PeerOrNull();
        if (p instanceof ViewportPeer) {
            ViewportPeer v = (ViewportPeer) p;
            v.setScrollableX(nativeScrolling && scrollableX);
            v.setScrollableY(nativeScrolling && scrollableY);
        }
    }

    /// Which axes the user may scroll; a scroll pane turns one off for
    /// the policy "never".
    public void cn1SetScrollable(boolean x, boolean y) {
        scrollableX = x;
        scrollableY = y;
        applyScrollable();
    }

    /// With `false` the viewport is only moved by `setViewPosition`, as
    /// the header viewports of a scroll pane are, and its view's peer is
    /// placed at the negated view position instead of being scrolled.
    public void cn1SetNativeScrolling(boolean b) {
        nativeScrolling = b;
        applyScrollable();
        placeView();
    }

    /// The peer was scrolled by the user, to a position in device pixels.
    protected void cn1NativeScrolled(int deviceX, int deviceY) {
        if (!nativeScrolling) {
            return;
        }
        Dimension size = viewSize();
        Dimension extent = getExtentSize();
        int x = Math.max(0, Math.min(Units.toLogical(deviceX), size.width - extent.width));
        int y = Math.max(0, Math.min(Units.toLogical(deviceY), size.height - extent.height));
        if (x != viewX || y != viewY) {
            viewX = x;
            viewY = y;
            scrollUnderway = true;
            try {
                placeView();
                fireStateChanged();
            } finally {
                scrollUnderway = false;
            }
        }
    }

    /// Gives the view its location and, when the peer scrolls natively,
    /// keeps the view's peer at the origin for Codename One to scroll.
    private void placeView() {
        Component view = getView();
        if (view == null) {
            return;
        }
        view.setLocation(-viewX, -viewY);
        com.codename1.ui.Component vp = view.cn1PeerOrNull();
        if (nativeScrolling && vp != null) {
            vp.setX(0);
            vp.setY(0);
        }
    }

    private void pushPosition() {
        com.codename1.ui.Component p = cn1PeerOrNull();
        if (nativeScrolling && p instanceof ViewportPeer) {
            ((ViewportPeer) p).scrollTo(Units.toDevice(viewX), Units.toDevice(viewY));
        }
    }

    // ------------------------------------------------------------ view

    public Component getView() {
        return getComponentCount() > 0 ? getComponent(0) : null;
    }

    public void setView(Component view) {
        for (int i = getComponentCount() - 1; i >= 0; i--) {
            remove(i);
        }
        isViewSizeSet = false;
        if (view != null) {
            super.addImpl(view, null, -1);
        }
        viewX = 0;
        viewY = 0;
        pushPosition();
        revalidate();
        repaint();
        fireStateChanged();
    }

    /// A viewport has one child: adding a component makes it the view.
    @Override
    protected void addImpl(Component child, Object constraints, int index) {
        setView(child);
    }

    private Dimension viewSize() {
        Component view = getView();
        return view == null ? new Dimension(0, 0) : view.getSize();
    }

    public Dimension getViewSize() {
        Component view = getView();
        if (view == null) {
            return new Dimension(0, 0);
        } else if (isViewSizeSet) {
            return view.getSize();
        }
        return view.getPreferredSize();
    }

    public void setViewSize(Dimension newSize) {
        Component view = getView();
        if (view != null && !newSize.equals(view.getSize())) {
            scrollUnderway = false;
            view.setSize(newSize);
            isViewSizeSet = true;
            fireStateChanged();
        }
    }

    public Point getViewPosition() {
        return new Point(viewX, viewY);
    }

    public void setViewPosition(Point p) {
        if (p.x == viewX && p.y == viewY) {
            return;
        }
        viewX = p.x;
        viewY = p.y;
        scrollUnderway = true;
        try {
            pushPosition();
            placeView();
            repaint();
            fireStateChanged();
        } finally {
            scrollUnderway = false;
        }
    }

    public Dimension getExtentSize() {
        Insets i = getInsets();
        return new Dimension(Math.max(0, getWidth() - i.left - i.right), Math.max(0, getHeight() - i.top - i.bottom));
    }

    public void setExtentSize(Dimension newExtent) {
        Dimension old = getExtentSize();
        if (!old.equals(newExtent)) {
            setSize(newExtent);
            fireStateChanged();
        }
    }

    public Rectangle getViewRect() {
        Dimension e = getExtentSize();
        return new Rectangle(viewX, viewY, e.width, e.height);
    }

    public Dimension toViewCoordinates(Dimension size) {
        return new Dimension(size);
    }

    public Point toViewCoordinates(Point p) {
        return new Point(p);
    }

    public int getScrollMode() {
        return scrollMode;
    }

    public void setScrollMode(int mode) {
        scrollMode = mode;
    }

    @Deprecated
    public boolean isBackingStoreEnabled() {
        return scrollMode == BACKINGSTORE_SCROLL_MODE;
    }

    @Deprecated
    public void setBackingStoreEnabled(boolean enabled) {
        scrollMode = enabled ? BACKINGSTORE_SCROLL_MODE : BLIT_SCROLL_MODE;
    }

    // ------------------------------------------------------------ scrolling

    private static int adjust(int parentWidth, int childWidth, int childAt) {
        if (childAt >= 0 && childWidth + childAt <= parentWidth) {
            return 0;
        }
        if (childAt <= 0 && childWidth + childAt >= parentWidth) {
            return 0;
        }
        if (childAt > 0 && childWidth <= parentWidth) {
            return -childAt + parentWidth - childWidth;
        }
        if (childAt >= 0 && childWidth >= parentWidth) {
            return -childAt;
        }
        if (childAt <= 0 && childWidth <= parentWidth) {
            return -childAt;
        }
        if (childAt < 0 && childWidth >= parentWidth) {
            return -childAt + parentWidth - childWidth;
        }
        return 0;
    }

    /// Scrolls so that the rectangle, given in this viewport's own
    /// coordinates, shows.
    public void scrollRectToVisible(Rectangle contentRect) {
        Component view = getView();
        if (view == null) {
            return;
        }
        if (!layingOut && !view.isValid()) {
            // The view may just have grown: scroll within its new size.
            // Not from this viewport's own layout, which is what gives the
            // view that size and would otherwise be started again.
            validate();
        }
        Dimension extent = getExtentSize();
        int dx = adjust(extent.width, contentRect.width, contentRect.x);
        int dy = adjust(extent.height, contentRect.height, contentRect.y);
        if (dx == 0 && dy == 0) {
            return;
        }
        Dimension size = view.getSize();
        int x = Math.max(0, Math.min(viewX - dx, Math.max(0, size.width - extent.width)));
        int y = Math.max(0, Math.min(viewY - dy, Math.max(0, size.height - extent.height)));
        setViewPosition(new Point(x, y));
    }

    /// Scrolls every viewport above `c` so that the rectangle, given in
    /// the coordinates of `c`, shows. This is what
    /// `JComponent.scrollRectToVisible` does on the desktop.
    public static void cn1ScrollRectToVisible(Component c, Rectangle aRect) {
        int dx = c.getX();
        int dy = c.getY();
        Container parent = c.getParent();
        while (parent != null && !(parent instanceof JViewport)) {
            dx += parent.getX();
            dy += parent.getY();
            parent = parent.getParent();
        }
        if (parent != null) {
            Rectangle r = new Rectangle(aRect.x + dx, aRect.y + dy, aRect.width, aRect.height);
            ((JViewport) parent).scrollRectToVisible(r);
            cn1ScrollRectToVisible(parent, new Rectangle(r.x, r.y, r.width, r.height));
        }
    }

    // ------------------------------------------------------------ layout

    @Override
    public void doLayout() {
        if (layingOut) {
            return;
        }
        layingOut = true;
        try {
            layOutView();
        } finally {
            layingOut = false;
        }
    }

    private void layOutView() {
        Component view = getView();
        if (view == null) {
            return;
        }
        Insets insets = getInsets();
        Dimension pref = view.getPreferredSize();
        Dimension extent = getExtentSize();
        int w = pref.width;
        int h = pref.height;
        if (view instanceof Scrollable) {
            Scrollable s = (Scrollable) view;
            if (s.getScrollableTracksViewportWidth()) {
                w = extent.width;
            }
            if (s.getScrollableTracksViewportHeight()) {
                h = extent.height;
            }
            if (w != pref.width && view instanceof JComponent) {
                // A view whose height follows its width (wrapped text) is
                // asked again once it has the width it will be shown at.
                view.setSize(w, view.getHeight());
                int again = view.getPreferredSize().height;
                if (!s.getScrollableTracksViewportHeight()) {
                    h = again;
                }
            }
        } else {
            if (w < extent.width) {
                w = extent.width;
            }
            if (h < extent.height) {
                h = extent.height;
            }
        }
        viewX = Math.max(0, Math.min(viewX, w - extent.width));
        viewY = Math.max(0, Math.min(viewY, h - extent.height));
        view.setBounds(insets.left - viewX, insets.top - viewY, w, h);
        // From here on the size of the view is what the layout gave it, the
        // way the desktop's ViewportLayout leaves it through setViewSize.
        isViewSizeSet = true;
        com.codename1.ui.Component vp = view.cn1PeerOrNull();
        if (nativeScrolling && vp != null) {
            vp.setX(0);
            vp.setY(0);
        }
        pushPosition();
        if (view instanceof JTextArea) {
            JTextArea area = (JTextArea) view;
            area.cn1RevealCaret();
            area.cn1RevealDone();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        Component view = getView();
        if (view == null) {
            return new Dimension(0, 0);
        }
        if (view instanceof Scrollable) {
            return ((Scrollable) view).getPreferredScrollableViewportSize();
        }
        return view.getPreferredSize();
    }

    @Override
    public Dimension getMinimumSize() {
        return isMinimumSizeSet() ? super.getMinimumSize() : new Dimension(4, 4);
    }

    @Override
    public Dimension getMaximumSize() {
        return isMaximumSizeSet() ? super.getMaximumSize() : new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /// Fills the viewport with the view's background, or its own when it
    /// has no view, so the area a short view leaves free is not blank.
    @Override
    protected void paintComponent(Graphics g) {
        if (isOpaque()) {
            Component view = getView();
            Color bg = view != null && view.isBackgroundSet() ? view.getBackground() : getBackground();
            if (bg != null) {
                g.setColor(bg);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        }
    }

    // ------------------------------------------------------------ listeners

    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    public ChangeListener[] getChangeListeners() {
        return listenerList.getListeners(ChangeListener.class);
    }

    protected void fireStateChanged() {
        ChangeListener[] ls = getChangeListeners();
        for (int i = ls.length - 1; i >= 0; i--) {
            if (changeEvent == null) {
                changeEvent = new ChangeEvent(this);
            }
            ls[i].stateChanged(changeEvent);
        }
    }

    @Override
    protected String paramString() {
        return super.paramString() + ",isViewSizeSet=" + isViewSizeSet + ",scrollUnderway=" + scrollUnderway;
    }
}
