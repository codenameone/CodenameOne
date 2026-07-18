package com.codename1.flutter.material;

import com.codename1.flutter.RenderElement;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Label;

/**
 * Leaf render box for {@link Divider}: the element occupies the full
 * {@code height} extent while the owned strip component (UIID
 * "FlutterDivider") is shrunk to the {@code thickness} line centered inside
 * it, painted via its background color.
 */
public class DividerRenderElement extends RenderElement {

    /** Flutter's default divider extent in logical pixels. */
    public static final double DEFAULT_HEIGHT_LP = 16;
    /** Default painted line thickness in logical pixels. */
    public static final double DEFAULT_THICKNESS_LP = 1;
    /** Material light-theme divider color (black at ~12% on white). */
    private static final int DEFAULT_COLOR = 0xE0E0E0;

    public DividerRenderElement(Divider widget) {
        super(widget);
    }

    private Divider divider() {
        return (Divider) widget();
    }

    private double heightLp() {
        return divider().getHeight() != null ? divider().getHeight() : DEFAULT_HEIGHT_LP;
    }

    private double thicknessLp() {
        return divider().getThickness() != null ? divider().getThickness() : DEFAULT_THICKNESS_LP;
    }

    @Override
    protected Component createComponent() {
        if (!Display.isInitialized()) {
            // headless unit tests: no CN1 components can exist
            return null;
        }
        Label strip = new Label("", "FlutterDivider");
        strip.getAllStyles().setPadding(0, 0, 0, 0);
        strip.getAllStyles().setMargin(0, 0, 0, 0);
        applyStyle(strip);
        return strip;
    }

    @Override
    protected void updateComponent(Component c) {
        applyStyle(c);
    }

    private void applyStyle(Component strip) {
        int color = divider().getColor() != null ? divider().getColor().rgb() : DEFAULT_COLOR;
        strip.getAllStyles().setBgColor(color);
        strip.getAllStyles().setBgTransparency(255);
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        double w = constraints.hasBoundedWidth() ? constraints.maxWidth() : 0;
        return constraints.constrain(new Size(w, Dp.px(heightLp())));
    }

    @Override
    public void position(int x, int y) {
        super.position(x, y);
        Component strip = component();
        if (strip != null) {
            int t = Math.max(1, (int) Math.round(Dp.px(thicknessLp())));
            t = (int) Math.min(t, Math.round(size().height()));
            strip.setY(y + (int) Math.round((size().height() - t) / 2));
            strip.setHeight(t);
        }
    }
}
