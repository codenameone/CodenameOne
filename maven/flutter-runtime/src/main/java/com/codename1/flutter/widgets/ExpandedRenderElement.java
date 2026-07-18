package com.codename1.flutter.widgets;

import com.codename1.flutter.RenderElement;
import com.codename1.flutter.SingleChildRenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Size;

/**
 * Pass-through box carrying the flex factor read by {@link FlexRenderElement}.
 * The Flex parent hands it tight main-axis constraints; it forwards them to
 * its child unchanged.
 */
public class ExpandedRenderElement extends SingleChildRenderElement {

    public ExpandedRenderElement(Expanded widget) {
        super(widget);
    }

    public long flex() {
        return ((Expanded) widget()).getFlex();
    }

    @Override
    protected Widget childWidget() {
        return ((Expanded) widget()).getChild();
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
