package com.codename1.flutter.widgets;

import com.codename1.flutter.Alignment;
import com.codename1.flutter.AlignmentDirectional;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.SingleChildRenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Size;

/**
 * Flutter's RenderFractionallySizedOverflowBox (bounded subset): fills the
 * incoming constraints and lays the child out tight to a fraction of each
 * bounded axis, positioning it by the alignment. Owns no CN1 component.
 */
public class FractionallySizedBoxRenderElement extends SingleChildRenderElement {

    public FractionallySizedBoxRenderElement(FractionallySizedBox widget) {
        super(widget);
    }

    private FractionallySizedBox box() {
        return (FractionallySizedBox) widget();
    }

    private Alignment alignment() {
        Object a = box().getAlignment();
        if (a instanceof Alignment) {
            return (Alignment) a;
        }
        if (a instanceof AlignmentDirectional) {
            return ((AlignmentDirectional) a).resolve();
        }
        return Alignment.center;
    }

    @Override
    protected Widget childWidget() {
        return box().getChild();
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        double selfW = constraints.hasBoundedWidth() ? constraints.maxWidth() : 0;
        double selfH = constraints.hasBoundedHeight() ? constraints.maxHeight() : 0;
        Size self = constraints.constrain(new Size(selfW, selfH));

        RenderElement child = renderChild();
        if (child == null) {
            return self;
        }

        Double wf = box().getWidthFactor();
        Double hf = box().getHeightFactor();
        double minW = 0;
        double maxW = constraints.maxWidth();
        double minH = 0;
        double maxH = constraints.maxHeight();
        if (wf != null && constraints.hasBoundedWidth()) {
            minW = maxW = constraints.maxWidth() * wf;
        }
        if (hf != null && constraints.hasBoundedHeight()) {
            minH = maxH = constraints.maxHeight() * hf;
        }
        Size cs = child.layout(new BoxConstraints(minW, maxW, minH, maxH));
        Alignment a = alignment();
        setChildOffset(child,
                Alignment.along(a.x(), self.width(), cs.width()),
                Alignment.along(a.y(), self.height(), cs.height()));
        return self;
    }
}
