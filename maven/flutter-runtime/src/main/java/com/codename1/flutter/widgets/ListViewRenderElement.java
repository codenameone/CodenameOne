package com.codename1.flutter.widgets;

import com.codename1.flutter.CrossAxisAlignment;
import com.codename1.flutter.MainAxisSize;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.Dp;
import com.codename1.ui.Component;
import com.codename1.ui.events.ScrollListener;

import dart.core.DartList;

/**
 * Scroll boundary for {@link ListView}. In builder mode it WINDOWS the list: only the items in (and a
 * little around) the viewport are materialized, with empty {@link SizedBox} spacers standing in for
 * the off-screen items so the scroll geometry is preserved. As the user scrolls, the visible window is
 * recomputed and this element is rebuilt, so the number of live components stays roughly constant
 * regardless of {@code itemCount}. This replaces the previous eager build of every item, which made
 * long lists both memory-heavy and janky. Children mode (a fixed list of children) still builds all.
 */
public class ListViewRenderElement extends ScrollRenderElement {

    private static final int INITIAL = 24;
    private static final int BUFFER = 8;

    private Component pane;
    private int winStart;
    private int winCount = INITIAL;
    private double itemH;            // measured item height in physical px (0 until measured)
    private boolean measured;

    public ListViewRenderElement(ListView widget) {
        super(widget);
    }

    private ListView listView() {
        return (ListView) widget();
    }

    @Override
    protected boolean shrinkWrap() {
        return listView().getShrinkWrap();
    }

    @Override
    protected Component createComponent() {
        Component c = super.createComponent();
        pane = c;
        if (c != null) {
            c.addScrollListener(new ScrollListener() {
                @Override
                public void scrollChanged(int scrollX, int scrollY, int oldX, int oldY) {
                    onScroll(scrollY);
                }
            });
        }
        return c;
    }

    /** Recomputes the visible window on scroll and rebuilds when it changed. */
    private void onScroll(int scrollY) {
        if (pane == null || !listView().isBuilderMode()) {
            return;
        }
        if (!measured) {
            measure();
        }
        double ih = itemH > 0 ? itemH : Dp.px(64);
        long count = listView().getItemCount();
        int viewport = pane.getHeight();
        int start = Math.max(0, (int) (scrollY / ih) - BUFFER);
        int cnt = (int) Math.ceil(viewport / ih) + BUFFER * 2;
        if (start + (long) cnt > count) {
            cnt = (int) Math.max(0, count - start);
        }
        // Throttle: the BUFFER of extra items above/below already covers small scrolls, so only
        // rebuild once the window has drifted by half the buffer. This keeps the viewport always
        // populated while avoiding a rebuild on every scroll frame (which would itself cause jank).
        boolean drifted = Math.abs(start - winStart) >= BUFFER / 2;
        boolean grew = cnt > winCount;
        if (drifted || grew) {
            winStart = start;
            winCount = Math.max(cnt, winCount);
            markNeedsBuild();
        }
    }

    /**
     * Measures the real item height once, from the bootstrap window (built with no spacers), so the
     * scroll-position math and spacer sizes are accurate.
     */
    private void measure() {
        if (pane == null) {
            return;
        }
        long count = listView().getItemCount();
        int built = (int) Math.min(count, winCount);
        if (built <= 0) {
            return;
        }
        double contentH = pane.getScrollDimension().getHeight();
        if (contentH > 0) {
            itemH = contentH / built;
            measured = true;
        }
    }

    @Override
    protected Widget buildContent() {
        ListView w = listView();
        DartList<Widget> items = new DartList<Widget>();
        if (!w.isBuilderMode()) {
            items = w.getChildren() == null ? new DartList<Widget>() : w.getChildren();
            return wrap(w, items);
        }
        long count = w.getItemCount();
        int start = winStart;
        if (start >= count) {
            start = (int) Math.max(0, count - 1);
        }
        int end = (int) Math.min(count, start + (long) winCount);
        // top spacer for the items scrolled off above (only once a real item height is known)
        if (measured && start > 0) {
            items.add(spacer(start * itemH));
        }
        for (long i = start; i < end; i++) {
            items.add(w.getItemBuilder().call(this, i));
        }
        // bottom spacer for the items below the window
        if (measured && end < count) {
            items.add(spacer((count - end) * itemH));
        }
        return wrap(w, items);
    }

    private Widget spacer(double physicalHeight) {
        SizedBox s = new SizedBox();
        double scale = Dp.scale();
        s.height(scale > 0 ? physicalHeight / scale : physicalHeight);
        return s;
    }

    private Widget wrap(ListView w, DartList<Widget> items) {
        Column col = new Column();
        col.crossAxisAlignment(CrossAxisAlignment.stretch);
        col.mainAxisSize(MainAxisSize.min);
        col.children(items);
        if (w.getPadding() == null) {
            return col;
        }
        Padding p = new Padding();
        p.padding(w.getPadding());
        p.child(col);
        return p;
    }
}
