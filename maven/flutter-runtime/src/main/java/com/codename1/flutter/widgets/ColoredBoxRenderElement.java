package com.codename1.flutter.widgets;

import com.codename1.flutter.Color;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.SingleChildRenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Component;
import com.codename1.ui.Display;

/**
 * Render element for {@link ColoredBox}: a CN1 Container (UIID "FlutterBox")
 * filled with the color, covering the element bounds. The child's components
 * attach after the face in tree order, so they paint on top. Sizes to the
 * child, or fills the bounded incoming axes when childless.
 */
public class ColoredBoxRenderElement extends SingleChildRenderElement {

    public ColoredBoxRenderElement(ColoredBox widget) {
        super(widget);
    }

    private ColoredBox box() {
        return (ColoredBox) widget();
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
        applyStyle(face);
        return face;
    }

    @Override
    protected void updateComponent(Component c) {
        applyStyle(c);
    }

    private void applyStyle(Component face) {
        try {
            if (box().getColor() != null) {
                face.getAllStyles().setBgColor(box().getColor().rgb());
                face.getAllStyles().setBgTransparency(box().getColor().alpha());
            } else {
                face.getAllStyles().setBgTransparency(0);
            }
        } catch (Exception err) {
            // styling best-effort
        }
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
