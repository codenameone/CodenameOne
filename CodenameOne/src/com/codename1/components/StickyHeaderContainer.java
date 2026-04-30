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
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.animations.AnimationTime;
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
/// or sectioned material lists. As the user scrolls past a section boundary
/// the previous header is replaced by the next one through a configurable
/// staged transition: a directional slide (slides up on forward scroll, down
/// on reverse scroll) or a cross-fade. Transitions are time-driven so a slow
/// scroll surfaces every animation frame at full duration while a fast
/// scroll lets the latest swap supersede earlier in-flight ones.
///
/// Sections are added with `addSection(header, content)`. The header is a
/// real component that participates in the scroll: when it is the active
/// section's header it is moved into a pinned overlay slot at the top of
/// the container, and a same-sized invisible placeholder is left behind in
/// the scroll content so nothing jumps. Because the pinned header is the
/// same instance, action listeners and child components remain interactive
/// while it is pinned.
///
/// ```java
/// StickyHeaderContainer sticky = new StickyHeaderContainer();
/// sticky.setTransitionStyle(StickyHeaderContainer.TRANSITION_SLIDE);
/// sticky.setTransitionDurationMillis(250);
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
    /// Replace the pinned header without animation.
    public static final int TRANSITION_NONE = 0;
    /// Slide the outgoing header out of the slot while the incoming header
    /// slides in. Forward scroll slides upward, reverse scroll slides
    /// downward.
    public static final int TRANSITION_SLIDE = 1;
    /// Fade the outgoing header to transparency on top of the incoming one.
    public static final int TRANSITION_FADE = 2;

    private static final int DEFAULT_TRANSITION_DURATION_MS = 250;

    private final ScrollContainer scroller;
    private final Container stickyHost;
    private final List<Section> sections = new ArrayList<Section>();

    private int activeIndex = -1;

    private int transitionStyle = TRANSITION_SLIDE;
    private int transitionDurationMillis = DEFAULT_TRANSITION_DURATION_MS;

    private Image transitionOutgoing;
    private long transitionStartMs;
    private boolean transitionForward;
    private int transitionSlotHeight;
    private int stickyHostBaseY;

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
    /// programmatic scrolling via [#setScrollPosition(int)].
    public Container getScrollContainer() {
        return scroller;
    }

    /// Returns the overlay container that hosts the currently-pinned header.
    /// While a section is active its header lives here; otherwise the host
    /// is empty and zero-height.
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
    /// header is currently pinned.
    public int getActiveSectionIndex() {
        return activeIndex;
    }

    /// Selects how the pinned header is replaced when the active section
    /// changes. One of [#TRANSITION_NONE], [#TRANSITION_SLIDE] (default) or
    /// [#TRANSITION_FADE].
    public void setTransitionStyle(int style) {
        if (style != TRANSITION_NONE && style != TRANSITION_SLIDE && style != TRANSITION_FADE) {
            throw new IllegalArgumentException("Unknown transition style: " + style);
        }
        this.transitionStyle = style;
    }

    public int getTransitionStyle() {
        return transitionStyle;
    }

    /// Sets the duration of the header replacement animation in
    /// milliseconds. A value of `0` makes transitions instantaneous
    /// regardless of [#getTransitionStyle()].
    public void setTransitionDurationMillis(int millis) {
        if (millis < 0) {
            throw new IllegalArgumentException("duration cannot be negative");
        }
        this.transitionDurationMillis = millis;
    }

    public int getTransitionDurationMillis() {
        return transitionDurationMillis;
    }

    /// Returns true while a header replacement animation is in progress.
    public boolean isTransitionInProgress() {
        if (transitionOutgoing == null) {
            return false;
        }
        return AnimationTime.now() - transitionStartMs < transitionDurationMillis;
    }

    /// Returns the progress of the in-flight transition as a fraction in
    /// `[0, 1]`. Returns `1` when no transition is running.
    public float getTransitionProgress() {
        if (transitionOutgoing == null || transitionDurationMillis <= 0) {
            return 1f;
        }
        long elapsed = AnimationTime.now() - transitionStartMs;
        if (elapsed <= 0) {
            return 0f;
        }
        if (elapsed >= transitionDurationMillis) {
            return 1f;
        }
        return (float) elapsed / (float) transitionDurationMillis;
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
            }
        }

        if (newActive == activeIndex) {
            return;
        }

        applyActivation(newActive);
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

    private void applyActivation(int newActive) {
        Image outgoingSnapshot = null;
        int outgoingHeight = 0;
        boolean wasActive = activeIndex >= 0;
        boolean willBeActive = newActive >= 0;

        if (wasActive && willBeActive
                && transitionStyle != TRANSITION_NONE
                && transitionDurationMillis > 0) {
            Component oldHeader = sections.get(activeIndex).header;
            outgoingHeight = oldHeader.getHeight();
            if (oldHeader.getWidth() > 0 && outgoingHeight > 0) {
                outgoingSnapshot = oldHeader.toImage();
            }
        }

        boolean forward;
        if (!wasActive) {
            forward = true;
        } else if (!willBeActive) {
            forward = false;
        } else {
            forward = newActive > activeIndex;
        }

        if (wasActive) {
            Section prev = sections.get(activeIndex);
            stickyHost.removeAll();
            int idx = scroller.getComponentIndex(prev.placeholder);
            if (idx >= 0) {
                scroller.removeComponent(prev.placeholder);
                scroller.addComponent(idx, prev.header);
            }
        }

        if (willBeActive) {
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

        if (outgoingSnapshot != null && willBeActive) {
            transitionOutgoing = outgoingSnapshot;
            transitionStartMs = AnimationTime.now();
            transitionForward = forward;
            transitionSlotHeight = outgoingHeight > 0 ? outgoingHeight
                    : activeHeightFor(activeIndex);
            registerForAnimation();
        } else {
            stopTransition();
        }

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
        applyActivation(-1);
    }

    private void registerForAnimation() {
        Form f = getComponentForm();
        if (f != null) {
            f.registerAnimated(this);
        }
    }

    private void stopTransition() {
        transitionOutgoing = null;
        stickyHost.setY(stickyHostBaseY);
        stickyHost.getAllStyles().setOpacity(255);
        Form f = getComponentForm();
        if (f != null) {
            f.deregisterAnimated(this);
        }
    }

    private void applyTransitionStateToIncoming() {
        if (transitionOutgoing == null) {
            stickyHost.setY(stickyHostBaseY);
            stickyHost.getAllStyles().setOpacity(255);
            return;
        }
        long elapsed = AnimationTime.now() - transitionStartMs;
        if (elapsed >= transitionDurationMillis) {
            stickyHost.setY(stickyHostBaseY);
            stickyHost.getAllStyles().setOpacity(255);
            return;
        }
        if (elapsed < 0) {
            elapsed = 0;
        }
        float progress = (float) elapsed / (float) transitionDurationMillis;

        if (transitionStyle == TRANSITION_SLIDE && transitionSlotHeight > 0) {
            int direction = transitionForward ? 1 : -1;
            int yOffset = (int) (direction * (1f - progress) * transitionSlotHeight);
            stickyHost.setY(stickyHostBaseY + yOffset);
            stickyHost.getAllStyles().setOpacity(255);
        } else if (transitionStyle == TRANSITION_FADE) {
            int alpha = (int) (255 * progress);
            stickyHost.getAllStyles().setOpacity(alpha);
            stickyHost.setY(stickyHostBaseY);
        }
    }

    @Override
    public boolean animate() {
        if (transitionOutgoing == null) {
            return false;
        }
        long elapsed = AnimationTime.now() - transitionStartMs;
        if (elapsed >= transitionDurationMillis) {
            stopTransition();
            return true;
        }
        applyTransitionStateToIncoming();
        return true;
    }

    @Override
    public void paint(Graphics g) {
        applyTransitionStateToIncoming();
        super.paint(g);
    }

    @Override
    protected void paintGlass(Graphics g) {
        super.paintGlass(g);
        if (transitionOutgoing == null) {
            return;
        }
        long elapsed = AnimationTime.now() - transitionStartMs;
        if (elapsed < 0) {
            elapsed = 0;
        }
        if (elapsed >= transitionDurationMillis) {
            return;
        }
        float progress = (float) elapsed / (float) transitionDurationMillis;

        Style ps = getStyle();
        int slotAbsX = getAbsoluteX() + ps.getPaddingLeft(isRTL());
        int slotAbsY = getAbsoluteY() + ps.getPaddingTop();

        if (transitionStyle == TRANSITION_FADE) {
            int alpha = (int) (255 * (1f - progress));
            int saved = g.getAlpha();
            g.setAlpha(alpha);
            g.drawImage(transitionOutgoing, slotAbsX, slotAbsY);
            g.setAlpha(saved);
        } else if (transitionStyle == TRANSITION_SLIDE && transitionSlotHeight > 0) {
            int direction = transitionForward ? -1 : 1;
            int yOffset = (int) (direction * progress * transitionSlotHeight);
            g.drawImage(transitionOutgoing, slotAbsX, slotAbsY + yOffset);
        }
    }

    @Override
    protected void deinitialize() {
        super.deinitialize();
        Form f = getComponentForm();
        if (f != null) {
            f.deregisterAnimated(this);
        }
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
            stickyHostBaseY = y;
            stickyHost.setX(x);
            stickyHost.setY(y);
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
