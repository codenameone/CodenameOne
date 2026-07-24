package com.codename1.flutter.widgets;

import com.codename1.flutter.ComposedElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;

import com.codename1.ui.Display;

/**
 * Element for {@link LayoutBuilder}: builds with the viewport constraints
 * (logical pixels), approximating Flutter's layout-time callback with a
 * build-time one. See {@link LayoutBuilder}.
 */
public class LayoutBuilderElement extends ComposedElement {

    private static final double FALLBACK_W_LP = 400;
    private static final double FALLBACK_H_LP = 800;

    public LayoutBuilderElement(LayoutBuilder widget) {
        super(widget);
    }

    @Override
    protected Widget build() {
        return ((LayoutBuilder) widget()).getBuilder().call(this, viewportConstraints());
    }

    private static BoxConstraints viewportConstraints() {
        double wLp = FALLBACK_W_LP;
        double hLp = FALLBACK_H_LP;
        if (Display.isInitialized()) {
            double scale = Dp.scale();
            if (scale > 0) {
                wLp = Display.getInstance().getDisplayWidth() / scale;
                hLp = Display.getInstance().getDisplayHeight() / scale;
            }
        }
        return new BoxConstraints(0, wLp, 0, hLp);
    }
}
