/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.components;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.events.ScrollListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.Style;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// A scrollable container that pins the most recently scrolled-past section
/// header to the top of its viewport, in the style of the iOS contacts list
/// or sectioned material lists. As the next section's header reaches the
/// pinned slot the previous header is pushed up out of the way and the next
/// header takes its place.
///
/// Sections are added with `addSection(header, content)`. The header is a
/// real component that participates in the scroll: when it would scroll above
/// the viewport top it is moved into a pinned overlay slot at the top of the
/// container, and a same-sized invisible placeholder is left in its place so
/// the scroll content does not jump. Because the pinned header is the same
/// instance that participates in the scroll, action listeners and child
/// components remain interactive while it is pinned.
///
/// ```java
/// StickyHeaderContainer sticky = new StickyHeaderContainer();
/// for (char c = 'A'; c <= 'Z'; c++) {
///     Label header = new Label("" + c, "StickyHeader");
///     Container items = new Container(BoxLayout.y());
///     for (int i = 0; i < 5; i++) {
///         items.add(new Label(c + " entry " + i));
///     }
///     sticky.addSection(header, items);
/// }
/// form.add(BorderLayout.CENTER, sticky);
/// ```
///
/// @author Shai Almog
public class StickyHeaderContainer extends Container {
    private final ScrollContainer scroller;
    private final Container stickyHost;
    private final List<Section> sections = new ArrayList<Section>();

    private int activeIndex = -1;
    private int activeOffset;

    private static class Section {
        final Component header;
        final Container placeholder;
        int height;

        Section(Component header) {
            this.header = header;
            this.placeholder = new Container();
            this.placeholder.setVisible(false);
        }
    }

    /// Creates an empty sticky header container. Add sections via
    /// `addSection(header, content)`.
    public StickyHeaderContainer() {
        super();
        scroller = new ScrollContainer();
        stickyHost = new Container(new BorderLayout());

        setLayout(new StickyOverlayLayout());
        super.addComponent(scroller);
        super.addComponent(stickyHost);

        scroller.addScrollListener(new ScrollListener() {
            @Override
            public void scrollChanged(int scrollX, int scrollY, int oldscrollX, int oldscrollY) {
                updateSticky();
            }
        });
    }

    private static final class ScrollContainer extends Container {
        ScrollContainer() {
            super(BoxLayout.y());
            setScrollableY(true);
        }

        void setScrollYExposed(int y) {
            setScrollY(y);
        }
    }

    /// Adds a section consisting of a sticky header and its content. The
    /// content may be `null` for a header-only section. Returns this for
    /// chaining.
    public StickyHeaderContainer addSection(Component header, Component content) {
        if (header == null) {
            throw new IllegalArgumentException("header cannot be null");
        }
        Section s = new Section(header);
        sections.add(s);
        scroller.addComponent(header);
        if (content != null) {
            scroller.addComponent(content);
        }
        return this;
    }

    /// Adds a header-only section.
    public StickyHeaderContainer addSection(Component header) {
        return addSection(header, null);
    }

    /// Returns the inner scrolling container that hosts the section content.
    /// Use this to add non-section components such as a footer, or for
    /// programmatic scrolling via [Container#setScrollY(int)].
    public Container getScrollContainer() {
        return scroller;
    }

    /// Returns the overlay container that hosts the currently-pinned header.
    /// While a section is active its header lives here; otherwise the host is
    /// empty and zero-height.
    public Container getStickyHost() {
        return stickyHost;
    }

    /// Returns an unmodifiable view of the registered sticky headers in the
    /// order they were added.
    public List<Component> getStickyHeaders() {
        List<Component> out = new ArrayList<Component>(sections.size());
        for (Section s : sections) {
            out.add(s.header);
        }
        return Collections.unmodifiableList(out);
    }

    /// Returns the index of the currently pinned section, or `-1` if no
    /// header is currently pinned (i.e. the user has not scrolled past the
    /// first section's header).
    public int getActiveSectionIndex() {
        return activeIndex;
    }

    /// Returns the vertical offset applied to the pinned header. The value
    /// is `0` while the header is fully pinned and becomes negative as the
    /// next section's header pushes it out of view. Once the next header
    /// reaches the top, the active section advances and the offset returns
    /// to `0`.
    public int getActiveSectionOffset() {
        return activeOffset;
    }

    /// Sets the scroll position of the inner scroll container. The value is
    /// clamped to the valid range and triggers a sticky-header recompute.
    public void setScrollPosition(int y) {
        scroller.setScrollYExposed(y);
    }

    /// Returns the current scroll position of the inner scroll container.
    public int getScrollPosition() {
        return scroller.getScrollY();
    }

    /// Removes all sections and content from the container.
    public void clearSections() {
        deactivate();
        scroller.removeAll();
        sections.clear();
    }

    /// Recomputes which section header should be pinned and updates the
    /// overlay accordingly. The container calls this internally on scroll;
    /// call it explicitly when section content has been mutated outside of
    /// a normal layout cycle.
    public void updateSticky() {
        if (sections.isEmpty()) {
            deactivate();
            return;
        }
        if (scroller.getHeight() <= 0) {
            return;
        }
        int sy = scroller.getScrollY();
        int newActive = -1;
        int nextRelTop = Integer.MAX_VALUE;
        for (int i = 0; i < sections.size(); i++) {
            Section s = sections.get(i);
            Component anchor = s.placeholder.getParent() == scroller ? s.placeholder : s.header; //NOPMD CompareObjectsWithEquals
            if (anchor.getParent() != scroller) { //NOPMD CompareObjectsWithEquals
                continue;
            }
            int aH = anchor.getHeight();
            if (aH <= 0) {
                aH = anchor.getPreferredH();
            }
            if (aH <= 0) {
                continue;
            }
            int relY = anchor.getY() - sy;
            if (relY < 0) {
                newActive = i;
            } else if (relY < nextRelTop) {
                nextRelTop = relY;
            }
        }

        int newOffset = 0;
        if (newActive >= 0) {
            int activeH = activeHeightFor(newActive);
            if (activeH > 0 && nextRelTop < activeH) {
                newOffset = nextRelTop - activeH;
            }
        }

        if (newActive == activeIndex) {
            if (newOffset != activeOffset) {
                activeOffset = newOffset;
                layoutContainer();
                if (isInitialized()) {
                    repaint();
                }
            }
            return;
        }

        applyActivation(newActive, newOffset);
    }

    private int activeHeightFor(int index) {
        Section s = sections.get(index);
        if (s.height > 0) {
            return s.height;
        }
        int h = s.header.getHeight();
        if (h <= 0) {
            h = s.header.getPreferredH();
        }
        return h;
    }

    private void applyActivation(int newActive, int newOffset) {
        if (activeIndex >= 0) {
            Section prev = sections.get(activeIndex);
            stickyHost.removeAll();
            int idx = scroller.getComponentIndex(prev.placeholder);
            if (idx >= 0) {
                scroller.removeComponent(prev.placeholder);
                scroller.addComponent(idx, prev.header);
            }
        }

        if (newActive >= 0) {
            Section next = sections.get(newActive);
            int idx = scroller.getComponentIndex(next.header);
            int h = next.header.getHeight();
            if (h <= 0) {
                h = next.header.getPreferredH();
            }
            int w = next.header.getWidth();
            if (w <= 0) {
                w = scroller.getWidth();
            }
            next.height = h;
            next.placeholder.setPreferredSize(new Dimension(w, h));
            if (idx >= 0) {
                scroller.removeComponent(next.header);
                scroller.addComponent(idx, next.placeholder);
            }
            stickyHost.addComponent(BorderLayout.CENTER, next.header);
        }

        activeIndex = newActive;
        activeOffset = newOffset;

        scroller.layoutContainer();
        stickyHost.layoutContainer();
        layoutContainer();
        if (isInitialized()) {
            repaint();
        }
    }

    private void deactivate() {
        if (activeIndex < 0) {
            return;
        }
        applyActivation(-1, 0);
    }

    private final class StickyOverlayLayout extends Layout {
        @Override
        public void layoutContainer(Container parent) {
            Style ps = parent.getStyle();
            int x = ps.getPaddingLeft(parent.isRTL());
            int y = ps.getPaddingTop();
            int innerW = parent.getLayoutWidth() - parent.getSideGap() - ps.getHorizontalPadding();
            int innerH = parent.getLayoutHeight() - parent.getBottomGap() - ps.getVerticalPadding();
            if (innerW < 0) {
                innerW = 0;
            }
            if (innerH < 0) {
                innerH = 0;
            }

            scroller.setX(x);
            scroller.setY(y);
            scroller.setWidth(innerW);
            scroller.setHeight(innerH);

            int headerH = activeIndex >= 0 ? activeHeightFor(activeIndex) : 0;
            stickyHost.setX(x);
            stickyHost.setY(y + activeOffset);
            stickyHost.setWidth(innerW);
            stickyHost.setHeight(headerH);
        }

        @Override
        public Dimension getPreferredSize(Container parent) {
            Dimension d = scroller.getPreferredSize();
            Style ps = parent.getStyle();
            return new Dimension(d.getWidth() + ps.getHorizontalPadding(),
                    d.getHeight() + ps.getVerticalPadding());
        }

        @Override
        public boolean isOverlapSupported() {
            return true;
        }
    }
}
