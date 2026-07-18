package com.codename1.flutter.widgets;

import com.codename1.flutter.RenderElement;
import com.codename1.flutter.SingleChildRenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;

/**
 * Flutter's RenderConstrainedBox with tight additional constraints for the
 * specified dimensions, merged into the incoming constraints via
 * {@link BoxConstraints#tighten}. Owns no CN1 component.
 */
public class SizedBoxRenderElement extends SingleChildRenderElement {

    public SizedBoxRenderElement(SizedBox widget) {
        super(widget);
    }

    @Override
    protected Widget childWidget() {
        return ((SizedBox) widget()).getChild();
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        SizedBox w = (SizedBox) widget();
        Double widthPx = w.getWidth() == null ? null : Double.valueOf(Dp.px(w.getWidth()));
        Double heightPx = w.getHeight() == null ? null : Double.valueOf(Dp.px(w.getHeight()));
        BoxConstraints inner = constraints.tighten(widthPx, heightPx);
        RenderElement child = renderChild();
        if (child == null) {
            return inner.constrain(new Size(
                    widthPx == null ? 0 : widthPx,
                    heightPx == null ? 0 : heightPx));
        }
        Size cs = child.layout(inner);
        setChildOffset(child, 0, 0);
        return cs;
    }
}
