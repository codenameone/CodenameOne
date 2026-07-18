package com.codename1.flutter.widgets;

import com.codename1.flutter.RenderElement;
import com.codename1.flutter.SingleChildRenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;

/**
 * Flutter's RenderConstrainedBox: the widget's additional constraints
 * (converted from logical to device pixels) are
 * {@link BoxConstraints#enforce(BoxConstraints) enforced} within the incoming
 * ones and imposed on the child. Owns no CN1 component.
 */
public class ConstrainedBoxRenderElement extends SingleChildRenderElement {

    public ConstrainedBoxRenderElement(ConstrainedBox widget) {
        super(widget);
    }

    @Override
    protected Widget childWidget() {
        return ((ConstrainedBox) widget()).getChild();
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        BoxConstraints additional = ((ConstrainedBox) widget()).getConstraints();
        BoxConstraints inner = additional == null
                ? constraints
                : toPx(additional).enforce(constraints);
        RenderElement child = renderChild();
        if (child == null) {
            return inner.constrain(Size.ZERO);
        }
        Size cs = child.layout(inner);
        setChildOffset(child, 0, 0);
        return cs;
    }

    private static BoxConstraints toPx(BoxConstraints lp) {
        return new BoxConstraints(
                px(lp.minWidth()), px(lp.maxWidth()),
                px(lp.minHeight()), px(lp.maxHeight()));
    }

    private static double px(double v) {
        return v == Double.POSITIVE_INFINITY ? v : Dp.px(v);
    }
}
