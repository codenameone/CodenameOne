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
import com.codename1.ui.Graphics;
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
/// or sectioned material lists. As the next section's header rises into the
/// pinned slot the previous header is replaced through a configurable
/// scroll-driven transition: a directional slide where the rising header
/// pushes the pinned one up and out, or a cover where the rising header
/// progressively slides on top of the pinned one. The outgoing header can
/// optionally fade out alongside either transition via
/// [#setHeaderFadeOut(boolean)]. Transitions are driven by scroll position
/// so the visual stays in sync with the user's gesture and there is no
/// time-based animation that lags behind a slow drag or skips ahead on a
/// fling.
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
    /// Replace the pinned header without any visible movement of the
    /// pinned header. The rising section's header stays hidden behind
    /// the pinned slot during the overlap and the swap is instant once
    /// it reaches the slot top. Generally [#TRANSITION_COVER] is a more
    /// useful choice, since with `TRANSITION_NONE` the rising header
    /// disappears under the pinned one with no visual feedback that the
    /// swap is approaching. Kept for backward compatibility.
    public static final int TRANSITION_NONE = 0;
    /// As the next section's header rises into the pinned slot from below
    /// it pushes the pinned header up and out of the slot in sync with
    /// the scroll, replacing it once the rising header reaches the top.
    public static final int TRANSITION_SLIDE = 1;
    /// Convenience equivalent to [#TRANSITION_SLIDE] combined with
    /// [#setHeaderFadeOut(boolean)] set to `true`: the pinned header
    /// both slides up and fades to transparency as the rising header
    /// closes the gap. Prefer composing
    /// `setTransitionStyle(TRANSITION_SLIDE)` with
    /// `setHeaderFadeOut(true)` -- or
    /// `setTransitionStyle(TRANSITION_COVER)` with
    /// `setHeaderFadeOut(true)` -- when you want a fade-out on top of
    /// another transition. Kept for backward compatibility.
    public static final int TRANSITION_FADE = 2;
    /// As the next section's header rises into the pinned slot from
    /// below it progressively slides on top of the pinned header,
    /// covering it from the bottom up while the pinned header stays
    /// fixed in the slot. The swap happens once the rising header
    /// reaches the slot top, at which point it has fully replaced the
    /// pinned one with no jump in position. Combine with
    /// [#setHeaderFadeOut(boolean)] to also fade the covered header out
    /// during the overlap.
    public static final int TRANSITION_COVER = 3;

    private final ScrollContainer scroller;
    private final Container stickyHost;
    private final List<Section> sections = new ArrayList<Section>();

    private int activeIndex = -1;

    private int transitionStyle = TRANSITION_SLIDE;
    private int transitionDurationMillis;
    private boolean headerFadeOut;

    /// Pixels of overlap between the pinned header's slot and the next
    /// section's header. `0` means the next section is still fully below
    /// the slot; equal to the pinned header's height means the swap is
    /// imminent.
    private int pushOffset;
    private int stickyHostBaseY;

    /// Reverse-activation hysteresis. Once a section is pinned, scroll
    /// inertia tends to bounce a few pixels past the swap boundary; if
    /// each bounce flipped the active section the pinned header would
    /// visibly jitter as it teleports between scroller-tracked and
    /// slot-fixed positions. Suppressing tiny reverse swaps inside this
    /// window absorbs the bounce.
    private static final int SWAP_HYSTERESIS_PIXELS = 4;

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
        stickyHost = new StickyHostContainer();

        setLayout(new StickyOverlayLayout());
        super.addComponent(scroller);
        super.addComponent(stickyHost);

        scroller.addScrollListener(new ScrollListener() {
            @Override
            public void scrollChanged(int scrollX, int scrollY, int oldscrollX, int oldscrollY) {
                // The ScrollContainer fires this event before its
                // internal scrollY field is updated, so reading it back
                // via getScrollY() returns the previous value. Drive the
                // recompute off the new scroll position the listener was
                // handed: a single multi-pixel drag/momentum step that
                // crosses the push window otherwise sees pushOffset stuck
                // at 0 and the rising header appears to slide *under* the
                // pinned header instead of pushing it out.
                updateSticky(scrollY);
            }
        });
    }

    /// Paints the COVER overlay on top of the pinned slot: when the
    /// transition style is [#TRANSITION_COVER] and a push is in flight,
    /// the rising section's header is rendered again above the pinned
    /// host so it visibly slides over the pinned header instead of
    /// being hidden underneath it. The natural paint in the scroller is
    /// at the same viewport position, so re-painting the header after
    /// `super.paint(g)` simply moves it to the top of the z-order in
    /// the slot region; outside that region the second paint draws over
    /// identical scroller content and is invisible. No-op for any other
    /// transition style or when no swap is mid-flight.
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (transitionStyle != TRANSITION_COVER || pushOffset <= 0 || activeIndex < 0) {
            return;
        }
        int nextIdx = activeIndex + 1;
        if (nextIdx >= sections.size()) {
            return;
        }
        Component rising = sections.get(nextIdx).header;
        if (rising.getParent() != scroller || !rising.isVisible()) { //NOPMD CompareObjectsWithEquals
            return;
        }
        rising.paintComponent(g);
    }

    /// Overlay host that always paints its parent's background under the
    /// pinned header. This keeps a transparent header UIID from showing
    /// scroller content through the slot during a transition.
    private final class StickyHostContainer extends Container {
        StickyHostContainer() {
            super(new BorderLayout());
        }

        @Override
        public void paintBackground(Graphics g) {
            Container parent = StickyHeaderContainer.this;
            Style ps = parent.getStyle();
            byte transparency = ps.getBgTransparency();
            if (transparency != 0 && g.isAlphaSupported()) {
                int oldColor = g.getColor();
                int oldAlpha = g.getAlpha();
                g.setColor(ps.getBgColor());
                g.setAlpha(transparency & 0xff);
                g.fillRect(getX(), getY(), getWidth(), getHeight());
                g.setColor(oldColor);
                g.setAlpha(oldAlpha);
            } else if (transparency != 0) {
                int oldColor = g.getColor();
                g.setColor(ps.getBgColor());
                g.fillRect(getX(), getY(), getWidth(), getHeight());
                g.setColor(oldColor);
            }
            super.paintBackground(g);
        }
    }

    private static final class ScrollContainer extends Container {
        boolean suppressLaidOutClamp;

        ScrollContainer() {
            super(BoxLayout.y());
            setScrollableY(true);
        }

        void setScrollYExposed(int y) {
            setScrollY(y);
        }

        @Override
        protected void laidOut() {
            if (suppressLaidOutClamp) {
                // applyActivation() relayouts the scroller from inside
                // the ScrollListener, before the outer setScrollY has
                // committed the new scrollY. Component#laidOut() would
                // then read the stale scrollY, decide it overflows, and
                // recursively call setScrollY(maxScroll) -- which fires
                // our listener again, flips the activation back, and
                // leaves activeIndex inconsistent with the scroll the
                // user requested. The outer setScrollY will write the
                // correct scrollY moments later, so skip the clamp.
                return;
            }
            super.laidOut();
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

    /// Selects how the pinned header is replaced when the next section
    /// rises into the slot. One of [#TRANSITION_NONE], [#TRANSITION_SLIDE]
    /// (default), [#TRANSITION_FADE] or [#TRANSITION_COVER]. The fade-out
    /// of the pinned header is a separate, composable concern controlled
    /// by [#setHeaderFadeOut(boolean)].
    public void setTransitionStyle(int style) {
        if (style != TRANSITION_NONE && style != TRANSITION_SLIDE
                && style != TRANSITION_FADE && style != TRANSITION_COVER) {
            throw new IllegalArgumentException("Unknown transition style: " + style);
        }
        if (this.transitionStyle == style) {
            return;
        }
        this.transitionStyle = style;
        applyPushVisuals();
        if (isInitialized()) {
            repaint();
        }
    }

    public int getTransitionStyle() {
        return transitionStyle;
    }

    /// When `true` the pinned header fades to transparency in proportion
    /// to the push progress as the next section rises into the slot.
    /// This is independent of the transition style and composes with
    /// [#TRANSITION_SLIDE] (slide + fade), [#TRANSITION_COVER] (cover +
    /// fade) and [#TRANSITION_NONE] (fade only). [#TRANSITION_FADE]
    /// implies the fade-out behaviour regardless of this flag.
    /// Defaults to `false`.
    public void setHeaderFadeOut(boolean fade) {
        if (this.headerFadeOut == fade) {
            return;
        }
        this.headerFadeOut = fade;
        applyPushVisuals();
        if (isInitialized()) {
            repaint();
        }
    }

    /// Returns whether the pinned header is configured to fade out
    /// during a swap. See [#setHeaderFadeOut(boolean)].
    public boolean isHeaderFadeOut() {
        return headerFadeOut;
    }

    /// Retained for API compatibility. Transitions are now scroll-driven so
    /// the per-frame duration no longer affects visuals; the value is
    /// validated and stored but otherwise unused.
    public void setTransitionDurationMillis(int millis) {
        if (millis < 0) {
            throw new IllegalArgumentException("duration cannot be negative");
        }
        this.transitionDurationMillis = millis;
    }

    public int getTransitionDurationMillis() {
        return transitionDurationMillis;
    }

    /// Returns true while the next section's header is overlapping the
    /// pinned slot, i.e. the scroll-driven transition is mid-flight.
    public boolean isTransitionInProgress() {
        return pushOffset > 0;
    }

    /// Returns the progress of the in-flight transition as a fraction in
    /// `[0, 1]`: `0` when the next section is just touching the slot from
    /// below and `1` when it has fully displaced the pinned header.
    /// Returns `0` when no transition is in progress.
    public float getTransitionProgress() {
        if (activeIndex < 0 || pushOffset <= 0) {
            return 0f;
        }
        int activeH = activeHeightFor(activeIndex);
        if (activeH <= 0) {
            return 0f;
        }
        if (pushOffset >= activeH) {
            return 1f;
        }
        return (float) pushOffset / (float) activeH;
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

    /// Recomputes which section header should be pinned and how far the
    /// next section has displaced it. Called internally on every scroll
    /// event; call it explicitly when section content has been mutated
    /// outside of a normal layout cycle.
    public void updateSticky() {
        updateSticky(scroller.getScrollY());
    }

    private void updateSticky(int sy) {
        if (sections.isEmpty()) {
            deactivate();
            return;
        }
        if (scroller.getHeight() <= 0) {
            return;
        }
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

        if (newActive < activeIndex && activeIndex >= 0) {
            // Inertial bounce on iOS routinely overshoots the swap
            // boundary by a few pixels. If we deactivate immediately the
            // pinned header teleports back into the scroller and is
            // re-activated on the next forward bounce, producing a
            // visible jitter. Hold the current active section across
            // tiny reverse excursions; a real backwards scroll past the
            // hysteresis window still deactivates normally.
            Section curr = sections.get(activeIndex);
            if (curr.placeholder.getParent() == scroller) { //NOPMD CompareObjectsWithEquals
                int distancePastBoundary = curr.placeholder.getY() - sy;
                if (distancePastBoundary > 0 && distancePastBoundary <= SWAP_HYSTERESIS_PIXELS) {
                    newActive = activeIndex;
                }
            }
        }

        boolean activationChanged = (newActive != activeIndex);
        if (activationChanged) {
            applyActivation(newActive);
            // Don't re-read scroller.getScrollY() here: when invoked
            // from the ScrollListener, the scroller hasn't yet updated
            // its internal scrollY field, so getScrollY() would return
            // the stale value and undo the parameter we were handed.
        }

        int newPush = computePushOffset(sy);
        boolean pushChanged = (newPush != pushOffset);
        pushOffset = newPush;

        if (activationChanged || pushChanged) {
            applyPushVisuals();
            if (isInitialized()) {
                repaint();
            }
        }
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

    private int computePushOffset(int sy) {
        if (activeIndex < 0) {
            return 0;
        }
        int activeH = activeHeightFor(activeIndex);
        if (activeH <= 0) {
            return 0;
        }
        int nextIdx = activeIndex + 1;
        if (nextIdx >= sections.size()) {
            return 0;
        }
        Section next = sections.get(nextIdx);
        Component anchor = next.placeholder.getParent() == scroller ? next.placeholder : next.header; //NOPMD CompareObjectsWithEquals
        if (anchor.getParent() != scroller) { //NOPMD CompareObjectsWithEquals
            return 0;
        }
        int aH = anchor.getHeight();
        if (aH <= 0) {
            aH = anchor.getPreferredH();
        }
        if (aH <= 0) {
            return 0;
        }
        int relY = anchor.getY() - sy;
        if (relY >= activeH) {
            return 0;
        }
        if (relY <= 0) {
            return activeH;
        }
        return activeH - relY;
    }

    private void applyActivation(int newActive) {
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
        // The newly active section starts fresh: no overlap with the
        // section after it until further scrolling brings it into the
        // push window.
        pushOffset = 0;

        scroller.suppressLaidOutClamp = true;
        try {
            scroller.layoutContainer();
        } finally {
            scroller.suppressLaidOutClamp = false;
        }
        stickyHost.layoutContainer();
        layoutContainer();
    }

    private void deactivate() {
        if (activeIndex < 0) {
            return;
        }
        applyActivation(-1);
        applyPushVisuals();
        if (isInitialized()) {
            repaint();
        }
    }

    private void applyPushVisuals() {
        if (activeIndex < 0 || pushOffset <= 0) {
            stickyHost.setY(stickyHostBaseY);
            stickyHost.getAllStyles().setOpacity(255);
            stickyHost.setVisible(true);
            return;
        }
        int activeH = activeHeightFor(activeIndex);
        // TRANSITION_FADE is a legacy convenience for "slide + fade out";
        // outside of that, the fade is governed solely by the separate
        // headerFadeOut flag.
        boolean fadeOutgoing = headerFadeOut || transitionStyle == TRANSITION_FADE;
        int alpha = 255;
        if (fadeOutgoing && activeH > 0) {
            alpha = 255 - (pushOffset * 255) / activeH;
            if (alpha < 0) {
                alpha = 0;
            } else if (alpha > 255) {
                alpha = 255;
            }
        }
        switch (transitionStyle) {
            case TRANSITION_SLIDE:
            case TRANSITION_FADE: {
                // FADE shares SLIDE's movement so the rising header is
                // visibly filling the slot from below while the pinned
                // header dissolves on its way out. With a fade-only
                // implementation the user sees the slot become empty as
                // the pinned header alpha drops, since the rising
                // header is still well below the slot top.
                stickyHost.setY(stickyHostBaseY - pushOffset);
                break;
            }
            case TRANSITION_COVER:
            case TRANSITION_NONE:
            default: {
                // Keep the pinned header in place. For COVER the rising
                // header is painted on top of the slot in paint(g) so
                // the user sees the cover progress; for NONE the rising
                // section stays hidden behind the pinned host until the
                // swap, which is instant. Hiding the host in NONE would
                // expose scroller content where the slot used to be.
                stickyHost.setY(stickyHostBaseY);
                break;
            }
        }
        stickyHost.getAllStyles().setOpacity(alpha);
        stickyHost.setVisible(true);
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
            stickyHost.setWidth(innerW);
            stickyHost.setHeight(headerH);
            // Re-apply any in-flight push so the host's Y matches the
            // current push offset relative to the freshly computed base.
            applyPushVisuals();
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
