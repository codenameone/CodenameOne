package com.codename1.flutter.widgets;

import com.codename1.flutter.RenderElement;
import com.codename1.flutter.SingleChildRenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Component;
import com.codename1.ui.Display;

/**
 * Render element for {@link DecoratedBox}: a CN1 Container (UIID "FlutterBox")
 * styled from the decoration, covering the element bounds behind the child.
 * Sizes to the child, or fills the bounded incoming axes when childless.
 */
public class DecoratedBoxRenderElement extends SingleChildRenderElement {

    public DecoratedBoxRenderElement(DecoratedBox widget) {
        super(widget);
    }

    private DecoratedBox box() {
        return (DecoratedBox) widget();
    }

    @Override
    protected Widget childWidget() {
        return box().getChild();
    }

    @Override
    protected Component createComponent() {
        if (!Display.isInitialized()) {
            return null;
        }
        com.codename1.ui.Container face = new com.codename1.ui.Container();
        face.setUIID("FlutterBox");
        face.getAllStyles().setPadding(0, 0, 0, 0);
        face.getAllStyles().setMargin(0, 0, 0, 0);
        FlutterBoxStyle.apply(face, null, box().getDecoration());
        return face;
    }

    @Override
    protected void updateComponent(Component c) {
        FlutterBoxStyle.apply(c, null, box().getDecoration());
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        RenderElement child = renderChild();
        if (child == null) {
            return constraints.constrain(new Size(
                    constraints.hasBoundedWidth() ? constraints.maxWidth() : 0,
                    constraints.hasBoundedHeight() ? constraints.maxHeight() : 0));
        }
        Size cs = child.layout(constraints);
        setChildOffset(child, 0, 0);
        return constraints.constrain(cs);
    }
}
