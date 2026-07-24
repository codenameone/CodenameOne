package com.codename1.flutter.widgets;

import com.codename1.flutter.RenderElement;
import com.codename1.flutter.SingleChildRenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Size;

/**
 * Layout-transparent host for {@link Directionality}: passes the incoming
 * constraints straight to the child and reports the child's size at the
 * origin. Owns no CN1 component.
 */
public class DirectionalityRenderElement extends SingleChildRenderElement {

    public DirectionalityRenderElement(Directionality widget) {
        super(widget);
    }

    @Override
    protected Widget childWidget() {
        return ((Directionality) widget()).getChild();
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        RenderElement child = renderChild();
        if (child == null) {
            return constraints.constrain(new Size(0, 0));
        }
        Size cs = child.layout(constraints);
        setChildOffset(child, 0, 0);
        return constraints.constrain(cs);
    }
}
