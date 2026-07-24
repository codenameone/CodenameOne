package com.codename1.flutter.widgets;

import com.codename1.flutter.RenderElement;
import com.codename1.flutter.SingleChildRenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Size;

/**
 * A render element that lays out its single child with the incoming
 * constraints and reports the child's size — the structural behavior shared
 * by the wrapper widgets ({@link com.codename1.flutter.widgets.SafeArea},
 * Semantics, Clip*, MouseRegion, Scrollbar, Tooltip, ...). Semantic and visual
 * effects those widgets carry (a11y annotations, clipping, hover) are not yet
 * applied; the child renders unchanged. Owns no CN1 component.
 */
public class PassThroughRenderElement extends SingleChildRenderElement {

    public PassThroughRenderElement(Widget widget) {
        super(widget);
    }

    @Override
    protected Widget childWidget() {
        return ((HasChild) widget()).getChild();
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        RenderElement child = renderChild();
        if (child == null) {
            return constraints.smallest();
        }
        Size cs = child.layout(constraints);
        setChildOffset(child, 0, 0);
        return constraints.constrain(cs);
    }
}
