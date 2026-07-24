package com.codename1.flutter.widgets;

import com.codename1.flutter.CrossAxisAlignment;
import com.codename1.flutter.MainAxisSize;
import com.codename1.flutter.Widget;

import dart.core.DartList;

/**
 * Scroll boundary for {@link PageView}. In builder mode it materializes every
 * page eagerly (page lists in new_gallery are short — a handful of study cards),
 * stacked in a {@link Column}; children mode lays the given pages out the same
 * way. One-page snapping and horizontal paging are deferred to a later pass, so
 * this element reuses the vertical scroll boundary for now.
 */
public class PageViewRenderElement extends ScrollRenderElement {

    public PageViewRenderElement(PageView widget) {
        super(widget);
    }

    private PageView pageView() {
        return (PageView) widget();
    }

    @Override
    protected Widget buildContent() {
        PageView w = pageView();
        DartList<Widget> items = new DartList<Widget>();
        if (w.isBuilderMode()) {
            long count = w.getItemCount() == null ? 0 : w.getItemCount();
            for (long i = 0; i < count; i++) {
                items.add(w.getItemBuilder().call(this, i));
            }
        } else if (w.getChildren() != null) {
            items = w.getChildren();
        }
        Column col = new Column();
        col.crossAxisAlignment(CrossAxisAlignment.stretch);
        col.mainAxisSize(MainAxisSize.min);
        col.children(items);
        return col;
    }
}
