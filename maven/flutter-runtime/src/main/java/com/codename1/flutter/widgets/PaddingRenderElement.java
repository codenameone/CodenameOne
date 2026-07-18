package com.codename1.flutter.widgets;

import com.codename1.flutter.EdgeInsets;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.SingleChildRenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;

/**
 * Deflates the incoming constraints by the padding (converted from logical
 * pixels to device pixels), lays out the child, and reports the child size
 * plus the insets. Owns no CN1 component.
 */
public class PaddingRenderElement extends SingleChildRenderElement {

    public PaddingRenderElement(Padding widget) {
        super(widget);
    }

    @Override
    protected Widget childWidget() {
        return ((Padding) widget()).getChild();
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        EdgeInsets lp = ((Padding) widget()).getPadding();
        EdgeInsets px = lp == null
                ? EdgeInsets.all(0)
                : EdgeInsets.only(Dp.px(lp.left()), Dp.px(lp.top()), Dp.px(lp.right()), Dp.px(lp.bottom()));
        RenderElement child = renderChild();
        if (child == null) {
            return constraints.constrain(new Size(px.horizontal(), px.vertical()));
        }
        Size cs = child.layout(constraints.deflate(px));
        setChildOffset(child, px.left(), px.top());
        return constraints.constrain(new Size(cs.width() + px.horizontal(), cs.height() + px.vertical()));
    }
}
