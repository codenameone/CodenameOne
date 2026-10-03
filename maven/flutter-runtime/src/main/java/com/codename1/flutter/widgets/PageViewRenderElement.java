/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.widgets;

import com.codename1.flutter.Axis;
import com.codename1.flutter.CrossAxisAlignment;
import com.codename1.flutter.Element;
import com.codename1.flutter.MainAxisSize;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.SingleChildRenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Size;

import dart.core.DartList;

/*
 * NOTE on the gallery: its home carousel passes pageSnapping: false, so it scrolls freely
 * and NONE of the settle code below runs for it. Worth knowing before reaching for this
 * class to explain something the carousel does - the answer is in the scroll physics.
 */
/**
 * Scroll boundary for {@link PageView}: pages sit side by side along the scroll
 * axis inside a real CN1 scroll pane, each sized to the controller's
 * {@code viewportFraction} of the viewport.
 *
 * <p>That fraction is the whole point of the widget's look — a value below 1 is
 * what makes the neighbouring pages peek in at the edges, which is how the
 * gallery's home carousel is built — so it has to reach the pages as a real
 * constraint rather than being ignored.</p>
 *
 * <p>Pages are materialized eagerly: page lists are short (the carousel holds
 * six study cards) and every one of them animates against the controller.</p>
 *
 * <p>The element also DRIVES the {@link PageController}: the pane's scroll offset
 * is published as a fractional page on every scroll event, which is what lets an
 * {@code AnimatedBuilder(animation: controller)} rebuild as the finger moves.
 * Momentum comes from CN1's pane; a released drag settles onto a page only when
 * {@code pageSnapping} is on.</p>
 */
public class PageViewRenderElement extends ScrollRenderElement {

    public PageViewRenderElement(PageView widget) {
        super(widget);
    }

    private PageView pageView() {
        return (PageView) widget();
    }

    @Override
    protected boolean horizontal() {
        return pageView().getScrollDirection() != Axis.vertical;
    }

    @Override
    protected boolean hideScrollbar() {
        return true;
    }

    private double viewportW;
    private double viewportH;
    private PageController attached;
    private long reportedPage;
    private boolean initialScrollApplied;

    @Override
    protected void viewport(double width, double height) {
        viewportW = width;
        viewportH = height;
    }

    // ------------------------------------------------------------------
    // Controller coupling
    // ------------------------------------------------------------------

    @Override
    public void mount(Element parent, int slot) {
        super.mount(parent, slot);
        attachController();
    }

    @Override
    protected void syncChildren() {
        super.syncChildren();
        // the configuration may have swapped the controller out from under us
        attachController();
    }

    @Override
    public void unmount() {
        if (attached != null) {
            attached.detach(this);
            attached = null;
        }
        super.unmount();
    }

    private void attachController() {
        PageController c = pageView().getController();
        if (c == attached) {
            return;
        }
        if (attached != null) {
            attached.detach(this);
        }
        attached = c;
        if (attached != null) {
            attached.attach(this);
            // Seed the reported page so settling on the page we STARTED on is not
            // announced as a change - Flutter fires onPageChanged on transitions,
            // never for the initial page.
            reportedPage = attached.initialPage();
            publishMetrics();
        }
    }

    /**
     * Feeds the controller the pane's geometry and offset. This is what makes
     * {@code controller.position.haveDimensions} true and {@code controller.page}
     * fractional, which is the whole basis of the carousel's per-card scaling.
     */
    private void publishMetrics() {
        double extent = pageExtent();
        if (attached == null || extent <= 0) {
            // Before the first layout there is no viewport, and publishing zeroes
            // would claim haveDimensions with a bogus page-0 offset. Flutter's
            // contract until then is exactly the initial page, which is what an
            // unattached controller already reports.
            return;
        }
        com.codename1.ui.Container pane = pane();
        if (!initialScrollApplied) {
            initialScrollApplied = true;
            // A PageView opens ON its initialPage; the pane starts at zero, so the
            // first layout that knows the page extent is where that is realized.
            if (attached.initialPage() != 0) {
                scrollToPage(attached.initialPage(), false);
            }
        }
        // Headless there is no pane and therefore no scrolling: the view simply
        // sits on its initial page.
        double pixels = pane == null
                ? attached.initialPage() * extent
                : (horizontal() ? pane.getScrollX() : pane.getScrollY());
        double viewportDim = horizontal() ? viewportW : viewportH;
        attached.applyMetrics(pixels, extent, viewportDim, 0, Math.max(0, maxScroll()));
        firePageChanged(Math.round(pixels / extent));
    }

    /** The largest legal scroll offset: the last page's resting position. */
    private double maxScroll() {
        return (pageCount() - 1) * pageExtent();
    }

    private int pageCount() {
        PageView w = pageView();
        if (w.isBuilderMode()) {
            return w.getItemCount() == null ? 0 : (int) w.getItemCount().longValue();
        }
        return w.getChildren() == null ? 0 : w.getChildren().size();
    }

    /** Flutter reports onPageChanged on the SETTLED page, so only on a whole-page change. */
    private void firePageChanged(long page) {
        if (page == reportedPage) {
            return;
        }
        reportedPage = page;
        dart.runtime.Funcs.VoidFunc1<dart.runtime.RefLong> cb = pageView().getOnPageChanged();
        if (cb != null) {
            cb.call(new dart.runtime.RefLong(page));
        }
    }

    private com.codename1.ui.Container pane() {
        com.codename1.ui.Component c = component();
        return c instanceof com.codename1.ui.Container ? (com.codename1.ui.Container) c : null;
    }

    /** Moves the pane onto {@code page}, animated or immediately. */
    void scrollToPage(long page, boolean animate) {
        com.codename1.ui.Container pane = pane();
        if (!(pane instanceof SnappingPane)) {
            return;
        }
        int target = (int) Math.round(Math.max(0, Math.min(maxScroll(), page * pageExtent())));
        ((SnappingPane) pane).moveTo(target, animate);
    }

    /**
     * Animates to {@code page} over {@code durationMs} following {@code curve} (null for
     * linear), completing {@code done} when it arrives or is superseded. The duration and
     * curve an app passed to animateToPage used to be dropped for the fixed 240ms settle
     * spring, so a one-second linear page turn ran fast and eased.
     */
    void animateToPage(long page, int durationMs, com.codename1.flutter.animation.Curve curve,
                       dart.async.Completer<Object> done) {
        com.codename1.ui.Container pane = pane();
        if (!(pane instanceof SnappingPane)) {
            done.complete(null);
            return;
        }
        int target = (int) Math.round(Math.max(0, Math.min(maxScroll(), page * pageExtent())));
        ((SnappingPane) pane).animateTo(target, durationMs, curve, done);
    }

    /** The fraction of the viewport one page occupies (Flutter's default is 1). */
    private double viewportFraction() {
        PageController c = pageView().getController();
        double f = c == null ? 1.0 : c.viewportFraction();
        return f > 0 && f <= 1 ? f : 1.0;
    }

    // ------------------------------------------------------------------
    // Page snapping
    // ------------------------------------------------------------------

    /** How long the settle animation runs, matching Flutter's page settle feel. */
    private static final int SNAP_MS = 240;

    /**
     * The scroll offset a release settles to — Flutter's
     * {@code PageScrollPhysics._getTargetPixels}.
     *
     * <p>Package-private and static so the rule can be asserted directly: it is the whole
     * difference between a carousel that pages the way Flutter's does and one that drifts
     * to the nearest card.</p>
     *
     * @param from     the offset at the moment the finger lifted
     * @param extent   one page's worth of scroll
     * @param velocity the release velocity, device pixels per millisecond, positive
     *                 towards later pages
     * @param max      the last page's resting offset
     */
    static int settleTarget(int from, double extent, float velocity, int max) {
        double page = from / extent;
        if (velocity < -VELOCITY_TOLERANCE_PX_PER_MS) {
            page -= 0.5;
        } else if (velocity > VELOCITY_TOLERANCE_PX_PER_MS) {
            page += 0.5;
        }
        int target = (int) Math.round(Math.round(page) * extent);
        return Math.max(0, Math.min(target, max));
    }

    /// Below this a release counts as a stop rather than a flick, and the carousel falls
    /// back to the nearest page instead of advancing.
    ///
    /// Flutter's `Tolerance.defaultTolerance.velocity` is 1/(0.05*3) logical pixels per
    /// SECOND; this is the same figure in the device pixels per MILLISECOND that Codename
    /// One's drag speed is measured in. It is deliberately tiny - in Flutter any purposeful
    /// flick clears it, and only a release that is really a stop does not.
    private static final float VELOCITY_TOLERANCE_PX_PER_MS =
            (float) (com.codename1.flutter.rendering.Dp.px(1.0 / (0.05 * 3.0)) / 1000.0);

    @Override
    protected com.codename1.ui.Container createPane(com.codename1.ui.layouts.Layout layout) {
        return new SnappingPane(layout);
    }

    /**
     * A scroll pane that comes to rest ON a page.
     *
     * <p>Codename One scrolls freely and keeps its own momentum after the finger lifts, so
     * a released drag otherwise stops wherever the momentum ran out - which is why the
     * carousel used to sit between two cards. We cannot snap in {@code pointerReleased}
     * because the momentum has not run yet; instead we wait for the scroll position to stop
     * changing and settle from there.</p>
     */
    private final class SnappingPane extends com.codename1.ui.Container {

        private boolean settling;

        /// The settle currently running, so it can be called off.
        ///
        /// Without this a settle is unstoppable once started: a finger landing mid-settle,
        /// or a second flick arriving before the first finished, leaves the old animation
        /// running alongside the new one. Both write the scroll offset every frame, the
        /// loser gets overwritten, and whichever finishes last drags the carousel to ITS
        /// target - which is how it ends up resting between two cards.
        private com.codename1.ui.animations.Animation settleAnim;

        SnappingPane(com.codename1.ui.layouts.Layout layout) {
            super(layout);
            // CN1 writes the scroll offset directly while a finger drags it, so the
            // setter is not an observation point - the scroll listener is the only
            // hook that sees drag, momentum and programmatic scrolling alike.
            addScrollListener(new com.codename1.ui.events.ScrollListener() {
                @Override
                public void scrollChanged(int scrollX, int scrollY, int oldscrollX,
                                          int oldscrollY) {
                    publishMetrics();
                }
            });
        }

        @Override
        public void pointerPressed(int x, int y) {
            // The finger owns the carousel now; a settle still running would fight it.
            cancelSettle();
            super.pointerPressed(x, y);
        }

        /** Stops any settle in flight, leaving the scroll exactly where it got to. */
        private void cancelSettle() {
            settling = false;
            if (pendingDone != null) {
                // Arrived, or superseded by another settle or a finger: either way the
                // animateToPage future completes, as Flutter's does.
                dart.async.Completer<Object> d = pendingDone;
                pendingDone = null;
                d.complete(null);
            }
            if (settleAnim == null) {
                return;
            }
            com.codename1.ui.Form f = getComponentForm();
            if (f != null) {
                f.deregisterAnimated(settleAnim);
            }
            settleAnim = null;
        }

        @Override
        public void pointerReleased(int x, int y) {
            if (!pageView().isPageSnapping()) {
                super.pointerReleased(x, y);
                return;
            }
            // Read the fling BEFORE super consumes the drag state.
            float velocity = getDragSpeed(!horizontal());
            super.pointerReleased(x, y);
            // Codename One has just started its own decay. Flutter runs exactly ONE
            // simulation from the moment of release, aimed at a page; letting the decay
            // play out first and settling afterwards is two motions, and it looks like it -
            // the carousel coasts to a stop and then visibly shifts again.
            stopScrollMomentum();
            settle(velocity);
        }

        /**
         * Settles onto a page the way Flutter's {@code PageScrollPhysics} does: the target
         * is chosen from the position AND the release velocity, then a single spring runs
         * to it.
         *
         * <p>The half-page bias is what makes a flick feel like a flick. Past the velocity
         * tolerance you go to the NEXT page even from a barely-moved carousel; below it you
         * fall back to whichever page you are nearest.</p>
         */
        private void settle(float velocityPxPerMs) {
            double extent = pageExtent();
            int from = horizontal() ? getScrollX() : getScrollY();
            int max = Math.max(0, (horizontal()
                    ? getScrollDimension().getWidth() - getWidth()
                    : getScrollDimension().getHeight() - getHeight()));
            int target = extent > 0 ? settleTarget(from, extent, velocityPxPerMs, max) : from;
            if ("true".equals(com.codename1.ui.Display.getInstance()
                    .getProperty("cn1.flutter.debugSettle", "false"))) {
                com.codename1.flutter.FlutterErrorReport.unimplemented("PageSettle",
                        "from=" + from + " extent=" + (int) extent + " v=" + velocityPxPerMs
                        + " tol=" + VELOCITY_TOLERANCE_PX_PER_MS + " max=" + max
                        + " target=" + target);
            }
            if (extent <= 0) {
                return;
            }
            if (target == from) {
                settling = false;
                return;
            }
            animateScroll(from, target);
        }

        /** The pending completion of a curved animateTo; completed however the motion ends. */
        private dart.async.Completer<Object> pendingDone;

        /** Programmatic paging with the caller's own duration and curve. */
        void animateTo(final int target, int durationMs, final com.codename1.flutter.animation.Curve curve,
                       final dart.async.Completer<Object> done) {
            int from = horizontal() ? getScrollX() : getScrollY();
            final com.codename1.ui.Form form = getComponentForm();
            if (from == target || durationMs <= 0 || form == null) {
                cancelSettle();
                setScroll(target);
                done.complete(null);
                return;
            }
            cancelSettle();
            settling = true;
            pendingDone = done;
            final int start = from;
            final com.codename1.ui.animations.Motion progress =
                    com.codename1.ui.animations.Motion.createLinearMotion(0, 10000, durationMs);
            progress.start();
            settleAnim = new com.codename1.ui.animations.Animation() {
                @Override
                public boolean animate() {
                    if (settleAnim != this) {
                        return false;
                    }
                    double t = progress.getValue() / 10000.0;
                    double eased = curve == null ? t : curve.transform(t);
                    setScroll((int) Math.round(start + (target - start) * eased));
                    if (progress.isFinished()) {
                        setScroll(target);
                        cancelSettle();
                    }
                    return false;   // see animateScroll: setScroll repaints the pane itself
                }

                @Override
                public void paint(com.codename1.ui.Graphics g) {
                }
            };
            form.registerAnimated(settleAnim);
        }

        /** Programmatic paging from the controller. */
        void moveTo(int target, boolean animate) {
            int from = horizontal() ? getScrollX() : getScrollY();
            if (from == target) {
                return;
            }
            if (!animate) {
                setScroll(target);
                return;
            }
            animateScroll(from, target);
        }

        private void animateScroll(int from, final int target) {
            // Exactly one settle may be in flight; starting a second without stopping the
            // first leaves two animations writing the scroll offset every frame.
            cancelSettle();
            final com.codename1.ui.Form form = getComponentForm();
            if (form == null) {
                setScroll(target);
                settling = false;
                return;
            }
            settling = true;
            final com.codename1.ui.animations.Motion motion =
                    // Critically damped, like Flutter's page spring - it eases out of the
                    // release without the symmetric slow start of an ease-in-out, which on
                    // a carousel that is ALREADY moving reads as a hitch before it goes.
                    com.codename1.ui.animations.Motion.createCriticalDampedSpringMotion(
                            from, target, SNAP_MS);
            motion.start();
            settleAnim = new com.codename1.ui.animations.Animation() {
                @Override
                public boolean animate() {
                    if (settleAnim != this) {
                        // Superseded by a newer settle, or called off by a finger landing.
                        return false;
                    }
                    setScroll(motion.getValue());
                    if (motion.isFinished()) {
                        setScroll(target);
                        cancelSettle();
                    }
                    // False, even though this animation changes the screen every frame:
                    // setScroll already repaints the pane, and returning true from a
                    // registered Animation that is NOT a Component makes paintDirty flush
                    // the WHOLE screen after calling paint() on it - which paints nothing
                    // here. That flushes a buffer the frame never drew into, i.e. a
                    // full-screen flicker for the length of the settle.
                    return false;
                }

                @Override
                public void paint(com.codename1.ui.Graphics g) {
                }
            };
            form.registerAnimated(settleAnim);
        }

        private void setScroll(int v) {
            if (horizontal()) {
                setScrollX(v);
            } else {
                setScrollY(v);
            }
            repaint();
        }
    }

    /**
     * Publishes metrics once the viewport is known. Layout is the only point at
     * which a PageView that has never been scrolled can tell its controller the
     * page geometry, and the carousel's very first frame depends on it.
     */
    @Override
    protected Size performLayout(BoxConstraints constraints) {
        Size s = super.performLayout(constraints);
        publishMetrics();
        return s;
    }

    /** One page's extent along the scroll axis, in device pixels. */
    double pageExtent() {
        return (horizontal() ? viewportW : viewportH) * viewportFraction();
    }

    @Override
    protected Widget buildContent() {
        PageView w = pageView();
        DartList<Widget> items = new DartList<Widget>();
        if (w.isBuilderMode()) {
            long count = w.getItemCount() == null ? 0 : w.getItemCount();
            for (long i = 0; i < count; i++) {
                items.add(new PageSlot(w.getItemBuilder().call(this, i)));
            }
        } else if (w.getChildren() != null) {
            for (Widget child : w.getChildren()) {
                items.add(new PageSlot(child));
            }
        }
        if (horizontal()) {
            // A viewportFraction below 1 CENTRES the current page: Flutter rests the
            // scroll at -(1-f)*viewport/2, so page 0 sits inset with its neighbour
            // peeking. Without this leading gap the first page is flush against the
            // leading edge and all the slack piles up on the trailing side.
            if (viewportFraction() < 1) {
                items.insert(0, new PageGap());
                items.add(new PageGap());
            }
            Row row = new Row();
            row.crossAxisAlignment(CrossAxisAlignment.stretch);
            row.mainAxisSize(MainAxisSize.min);
            row.children(items);
            return row;
        }
        Column col = new Column();
        col.crossAxisAlignment(CrossAxisAlignment.stretch);
        col.mainAxisSize(MainAxisSize.min);
        col.children(items);
        return col;
    }

    /**
     * The leading/trailing slack that centres the resting page.
     *
     * <p>Measured at LAYOUT time, not build time: {@code buildContent} runs before the
     * viewport is known, so a gap sized during the build would always compute to zero.
     * Same reason the pages themselves size from {@link #viewport}.</p>
     */
    private final class PageGap extends Widget {
        @Override
        public Element createElement() {
            return new PageGapElement(this);
        }
    }

    private final class PageGapElement extends RenderElement {
        PageGapElement(PageGap widget) {
            super(widget);
        }

        @Override
        protected Size performLayout(BoxConstraints constraints) {
            double slack = viewportW * (1 - viewportFraction()) / 2;
            return new Size(Math.max(0, slack),
                    constraints.hasBoundedHeight() ? constraints.maxHeight() : 0);
        }
    }

    /**
     * One page: {@code viewportFraction} of the viewport along the scroll axis,
     * the full extent across it.
     */
    private final class PageSlot extends Widget {

        private final Widget child;

        PageSlot(Widget child) {
            this.child = child;
        }

        @Override
        public Element createElement() {
            return new PageSlotElement(this);
        }

        Widget child() {
            return child;
        }
    }

    private final class PageSlotElement extends SingleChildRenderElement {

        PageSlotElement(PageSlot widget) {
            super(widget);
        }

        @Override
        protected Widget childWidget() {
            return ((PageSlot) widget()).child();
        }

        /**
         * Sizes against the VIEWPORT, not the incoming constraints: inside a
         * scroll boundary the main axis is unbounded by construction, so a page
         * has to know the pane's extent to take a fraction of it. The extent
         * comes from {@link #viewport}, reported before this layout runs —
         * {@code size()} is not assigned yet at this point.
         */
        @Override
        protected Size performLayout(BoxConstraints constraints) {
            double fraction = viewportFraction();
            double w;
            double h;
            if (horizontal()) {
                w = viewportW * fraction;
                h = constraints.hasBoundedHeight() ? constraints.maxHeight() : viewportH;
            } else {
                h = viewportH * fraction;
                w = constraints.hasBoundedWidth() ? constraints.maxWidth() : viewportW;
            }
            RenderElement c = renderChild();
            if (c != null) {
                c.layout(BoxConstraints.tight(w, h));
                setChildOffset(c, 0, 0);
            }
            return new Size(w, h);
        }
    }
}
