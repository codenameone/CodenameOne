package com.codename1.flutter.widgets;

import com.codename1.flutter.Alignment;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.SingleChildRenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Size;

/**
 * Flutter's RenderPositionedBox with a fixed center alignment: loosens the
 * incoming constraints for the child, expands itself to the bounded axes and
 * positions the child by alignment. Owns no CN1 component.
 */
public class CenterRenderElement extends SingleChildRenderElement {

    public CenterRenderElement(Center widget) {
        super(widget);
    }

    @Override
    protected Widget childWidget() {
        return ((Center) widget()).getChild();
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
        setChildOffset(child,
                Alignment.along(0, self.width(), cs.width()),
                Alignment.along(0, self.height(), cs.height()));
        return self;
    }
}
