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

    /** The fraction of the viewport one page occupies (Flutter's default is 1). */
    private double viewportFraction() {
        PageController c = pageView().getController();
        double f = c == null ? 1.0 : c.viewportFraction();
        return f > 0 && f <= 1 ? f : 1.0;
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
         * Measures the VIEWPORT, not the incoming constraints: inside a scroll
         * boundary the main axis is unbounded by construction, so a page has to
         * read the pane's own size to know what a fraction of the viewport is.
         */
        @Override
        protected Size performLayout(BoxConstraints constraints) {
            Size viewport = PageViewRenderElement.this.size();
            double fraction = viewportFraction();
            double w;
            double h;
            if (horizontal()) {
                w = viewport.width() * fraction;
                h = constraints.hasBoundedHeight() ? constraints.maxHeight() : viewport.height();
            } else {
                h = viewport.height() * fraction;
                w = constraints.hasBoundedWidth() ? constraints.maxWidth() : viewport.width();
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
