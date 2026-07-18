package com.codename1.flutter.material;

import com.codename1.flutter.RenderElement;
import com.codename1.flutter.SingleChildRenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;

/**
 * Render element for {@link Drawer}: sizes to the standard Material drawer
 * width (304lp) when unconstrained (the side-menu preferred-size dry pass)
 * and fills whatever the side menu hands it otherwise; the child subtree
 * fills the panel.
 */
public class DrawerRenderElement extends SingleChildRenderElement {

    /** Standard Material drawer width in logical pixels. */
    public static final double WIDTH_LP = 304;

    public DrawerRenderElement(Drawer widget) {
        super(widget);
    }

    @Override
    protected Widget childWidget() {
        return ((Drawer) widget()).getChild();
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        double width = constraints.hasBoundedWidth()
                ? constraints.maxWidth()
                : Dp.px(WIDTH_LP);
        double height = constraints.hasBoundedHeight() ? constraints.maxHeight() : 0;
        RenderElement child = renderChild();
        if (child != null) {
            Size cs = child.layout(new BoxConstraints(
                    width, width, 0,
                    constraints.hasBoundedHeight() ? height : Double.POSITIVE_INFINITY));
            setChildOffset(child, 0, 0);
            height = Math.max(height, cs.height());
        }
        return constraints.constrain(new Size(width, height));
    }
}
