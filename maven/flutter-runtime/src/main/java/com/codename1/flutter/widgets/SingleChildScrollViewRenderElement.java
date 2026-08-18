package com.codename1.flutter.widgets;

import com.codename1.flutter.Axis;
import com.codename1.flutter.Widget;

/**
 * Scroll boundary for {@link SingleChildScrollView}: the content is the
 * child, optionally inset by the padding (synthesized {@link Padding}).
 */
public class SingleChildScrollViewRenderElement extends ScrollRenderElement {

    public SingleChildScrollViewRenderElement(SingleChildScrollView widget) {
        super(widget);
    }

    @Override
    protected boolean horizontal() {
        return ((SingleChildScrollView) widget()).getScrollDirection() == Axis.horizontal;
    }

    @Override
    protected Widget buildContent() {
        SingleChildScrollView w = (SingleChildScrollView) widget();
        if (w.getPadding() == null) {
            return w.getChild();
        }
        Padding p = new Padding();
        p.padding(w.getPadding());
        p.child(w.getChild());
        return p;
    }
}
