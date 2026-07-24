package com.codename1.flutter.animation;

import com.codename1.flutter.RenderElement;
import com.codename1.flutter.SingleChildRenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Size;

/**
 * The render element backing an {@link AnimatedChildWidget}: it owns no CN1
 * component and lays out its single child with the incoming constraints,
 * reporting the child's size (or the smallest allowed size when there is no
 * child). Visual transforms (opacity, scale, slide) are not yet applied.
 */
public class PassthroughRenderElement extends SingleChildRenderElement {

    public PassthroughRenderElement(AnimatedChildWidget widget) {
        super(widget);
    }

    @Override
    protected Widget childWidget() {
        return ((AnimatedChildWidget) widget()).getChild();
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        RenderElement child = renderChild();
        if (child == null) {
            return constraints.constrain(Size.ZERO);
        }
        Size cs = child.layout(constraints);
        setChildOffset(child, 0, 0);
        return cs;
    }
}
