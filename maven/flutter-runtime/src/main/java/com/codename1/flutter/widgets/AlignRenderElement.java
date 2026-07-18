package com.codename1.flutter.widgets;

import com.codename1.flutter.Alignment;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.SingleChildRenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Size;

/**
 * Flutter's RenderPositionedBox: loosens the incoming constraints for the
 * child, expands itself to the bounded axes and positions the child by the
 * configured alignment. Owns no CN1 component.
 */
public class AlignRenderElement extends SingleChildRenderElement {

    public AlignRenderElement(Align widget) {
        super(widget);
    }

    private Alignment alignment() {
        Alignment a = ((Align) widget()).getAlignment();
        return a == null ? Alignment.center : a;
    }

    @Override
    protected Widget childWidget() {
        return ((Align) widget()).getChild();
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        RenderElement child = renderChild();
        if (child == null) {
            return constraints.constrain(new Size(
                    constraints.hasBoundedWidth() ? constraints.maxWidth() : 0,
                    constraints.hasBoundedHeight() ? constraints.maxHeight() : 0));
        }
        Size cs = child.layout(constraints.loosen());
        double w = constraints.hasBoundedWidth() ? constraints.maxWidth() : cs.width();
        double h = constraints.hasBoundedHeight() ? constraints.maxHeight() : cs.height();
        Size self = constraints.constrain(new Size(w, h));
        Alignment a = alignment();
        setChildOffset(child,
                Alignment.along(a.x(), self.width(), cs.width()),
                Alignment.along(a.y(), self.height(), cs.height()));
        return self;
    }
}
