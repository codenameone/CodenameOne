package com.codename1.flutter.widgets;

import com.codename1.flutter.CrossAxisAlignment;
import com.codename1.flutter.MainAxisSize;
import com.codename1.flutter.Widget;

import dart.core.DartList;

/**
 * Scroll boundary for {@link ListView}: the content is a stretched column of
 * the children (or, in builder mode, of the eagerly materialized items —
 * this render element is the {@code BuildContext} handed to the item
 * builder), optionally inset by the padding.
 */
public class ListViewRenderElement extends ScrollRenderElement {

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
    protected Widget buildContent() {
        ListView w = listView();
        DartList<Widget> items;
        if (w.isBuilderMode()) {
            items = new DartList<Widget>();
            long count = w.getItemCount();
            for (long i = 0; i < count; i++) {
                items.add(w.getItemBuilder().call(this, i));
            }
        } else {
            items = w.getChildren() == null ? new DartList<Widget>() : w.getChildren();
        }
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
