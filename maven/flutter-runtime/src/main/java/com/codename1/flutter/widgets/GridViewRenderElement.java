package com.codename1.flutter.widgets;

import com.codename1.flutter.Widget;

/**
 * Scroll boundary for {@link GridView}: the content is a {@link GridContent}
 * carrying the grid configuration, optionally inset by the padding.
 */
public class GridViewRenderElement extends ScrollRenderElement {

    public GridViewRenderElement(GridView widget) {
        super(widget);
    }

    @Override
    protected Widget buildContent() {
        GridView w = (GridView) widget();
        GridContent gc = new GridContent(w.getCrossAxisCount(), w.getChildAspectRatio(),
                w.getMainAxisSpacing(), w.getCrossAxisSpacing(), w.getChildren());
        if (w.getPadding() == null) {
            return gc;
        }
        Padding p = new Padding();
        p.padding(w.getPadding());
        p.child(gc);
        return p;
    }
}
