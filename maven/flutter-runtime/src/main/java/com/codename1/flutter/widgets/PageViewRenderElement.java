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
            // A new touch owns the pane; the previous flick's watcher must not fire a
            // snap under the finger.
            stopWatching();
            super.pointerPressed(x, y);
        }

        @Override
        public void pointerReleased(int x, int y) {
            super.pointerReleased(x, y);
            if (pageView().isPageSnapping()) {
                awaitMomentum();
            }
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
            settling = true;
            animateScroll(from, target);
        }

        /// Registered while the release's momentum is still carrying the pane.
        /// Held so a second release cannot stack a second watcher on the form.
        private com.codename1.ui.animations.Animation momentumWatch;

        /// Watches the pane once per frame until CN1's momentum stops moving it, then
        /// settles onto the nearest page.
        ///
        /// This used to poll with {@code CN.setTimeout(50)}, which is wrong on both
        /// counts: {@code Display.setTimeout} allocates a whole {@code java.util.Timer}
        /// thread per call, so a single flick spun up and abandoned one thread per poll;
        /// and a 50ms poll cannot see the moment momentum stops, so the snap started up
        /// to a frame-and-a-half late. Riding the form's animation loop costs nothing
        /// extra - the pane is already keeping the EDT awake while it glides - and
        /// notices the stop on the very frame it happens.
        private void awaitMomentum() {
            final com.codename1.ui.Form form = getComponentForm();
            if (form == null) {
                snap();
                return;
            }
            if (momentumWatch != null) {
                return;
            }
            momentumWatch = new com.codename1.ui.animations.Animation() {
                private int previous = Integer.MIN_VALUE;

                @Override
                public boolean animate() {
                    int current = horizontal() ? getScrollX() : getScrollY();
                    if (current != previous) {
                        previous = current;
                        // False: the pane repaints itself as it scrolls; asking for a
                        // repaint here would add a full one per frame on top.
                        return false;
                    }
                    stopWatching();
                    snap();
                    return false;
                }

                @Override
                public void paint(com.codename1.ui.Graphics g) {
                }
            };
            form.registerAnimated(momentumWatch);
        }

        private void stopWatching() {
            if (momentumWatch == null) {
                return;
            }
            com.codename1.ui.Form f = getComponentForm();
            if (f != null) {
                f.deregisterAnimated(momentumWatch);
            }
            momentumWatch = null;
        }

        private void snap() {
            double extent = pageExtent();
            if (settling || extent <= 0) {
                return;
            }
            int from = horizontal() ? getScrollX() : getScrollY();
            int target = (int) Math.round(Math.round(from / extent) * extent);
            if (target == from) {
                return;
            }
            settling = true;
            animateScroll(from, target);
        }

        private void animateScroll(int from, final int target) {
            final com.codename1.ui.Form form = getComponentForm();
            if (form == null) {
                setScroll(target);
                settling = false;
                return;
            }
            final com.codename1.ui.animations.Motion motion =
                    com.codename1.ui.animations.Motion.createEaseInOutMotion(from, target, SNAP_MS);
            motion.start();
            form.registerAnimated(new com.codename1.ui.animations.Animation() {
                @Override
                public boolean animate() {
                    setScroll(motion.getValue());
                    if (motion.isFinished()) {
                        setScroll(target);
                        settling = false;
                        com.codename1.ui.Form f = getComponentForm();
                        if (f != null) {
                            f.deregisterAnimated(this);
                        }
                    }
                    return true;
                }

                @Override
                public void paint(com.codename1.ui.Graphics g) {
                }
            });
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
