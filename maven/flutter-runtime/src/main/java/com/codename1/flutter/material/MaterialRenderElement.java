package com.codename1.flutter.material;

import com.codename1.flutter.RenderElement;
import com.codename1.flutter.SingleChildRenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;

/**
 * Render element for {@link Material}: a CN1 Container (UIID "FlutterMaterial")
 * filled with the surface color, covering the element bounds behind the child.
 * Sizes to the child, or fills the bounded incoming axes when childless. The
 * child's components attach after the face, so they paint on top.
 */
public class MaterialRenderElement extends SingleChildRenderElement {

    public MaterialRenderElement(Material widget) {
        super(widget);
    }

    private Material material() {
        return (Material) widget();
    }

    @Override
    protected Widget childWidget() {
        return material().getChild();
    }

    @Override
    protected Component createComponent() {
        if (!Display.isInitialized() || material().getColor() == null) {
            return null;
        }
        Container face = new Container();
        face.setUIID("FlutterMaterial");
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
            if (material().getColor() != null) {
                face.getAllStyles().setBgColor(material().getColor().rgb());
                face.getAllStyles().setBgTransparency(material().getColor().alpha());
            }
        } catch (Exception err) {
            // best-effort
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
