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
 * six study cards) and every one of them animates against the controller.
 * Momentum comes from CN1's pane; one-page snapping is deferred.</p>
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

    @Override
    protected void viewport(double width, double height) {
        viewportW = width;
        viewportH = height;
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
        }

        @Override
        public void pointerReleased(int x, int y) {
            super.pointerReleased(x, y);
            awaitMomentum(Integer.MIN_VALUE);
        }

        /** Polls until CN1's momentum stops moving the pane, then settles onto a page. */
        private void awaitMomentum(final int previous) {
            final int current = horizontal() ? getScrollX() : getScrollY();
            if (current == previous) {
                snap();
                return;
            }
            com.codename1.ui.CN.setTimeout(50, new Runnable() {
                @Override
                public void run() {
                    awaitMomentum(current);
                }
            });
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
