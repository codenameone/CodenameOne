package com.codename1.flutter.widgets;

import com.codename1.flutter.RenderElement;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Component;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.plaf.Style;

/**
 * Leaf render box for {@link Icon}: a CN1 Label carrying a material
 * FontImage sized in millimeters equivalent to the requested logical pixels.
 */
public class IconRenderElement extends RenderElement {

    /** Flutter's default icon size in logical pixels. */
    public static final double DEFAULT_SIZE_LP = 24;

    public IconRenderElement(Icon widget) {
        super(widget);
    }

    private Icon icon() {
        return (Icon) widget();
    }

    private double sizeLp() {
        return icon().getSize() != null ? icon().getSize() : DEFAULT_SIZE_LP;
    }

    @Override
    protected Component createComponent() {
        Label l = new Label("", "FlutterIcon");
        l.getAllStyles().setPadding(0, 0, 0, 0);
        l.getAllStyles().setMargin(0, 0, 0, 0);
        applyIcon(l);
        return l;
    }

    @Override
    protected void updateComponent(Component c) {
        applyIcon((Label) c);
    }

    private void applyIcon(Label l) {
        if (icon().getIcon() == null) {
            l.setIcon(null);
            return;
        }
        Style s = new Style(l.getUnselectedStyle());
        if (icon().getColor() != null) {
            s.setFgColor(icon().getColor().rgb());
        }
        s.setBgTransparency(0);
        try {
            l.setIcon(FontImage.createMaterial(icon().getIcon().codePoint(), s, Dp.mm(sizeLp())));
        } catch (Exception err) {
            // headless or missing icon font: layout still reserves the box
        }
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        double px = Dp.px(sizeLp());
        return constraints.constrain(new Size(px, px));
    }
}
