package com.codename1.flutter.widgets;

import com.codename1.flutter.RenderElement;
import com.codename1.flutter.SingleChildRenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Size;

/**
 * Pass-through box carrying the inset configuration read by
 * {@link StackRenderElement}. The Stack parent hands it the constraints it
 * resolved from the insets; it forwards them to its child unchanged.
 */
public class PositionedRenderElement extends SingleChildRenderElement {

    public PositionedRenderElement(Positioned widget) {
        super(widget);
    }

    public Positioned positioned() {
        return (Positioned) widget();
    }

    @Override
    protected Widget childWidget() {
        return positioned().getChild();
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
