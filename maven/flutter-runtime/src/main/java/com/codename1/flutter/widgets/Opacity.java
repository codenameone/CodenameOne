package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * Makes its {@code child} partially transparent — Flutter's {@code Opacity}.
 * The opacity value (0.0 fully transparent .. 1.0 fully opaque) is captured;
 * this pass renders the child at full opacity, with alpha compositing deferred
 * to the paint layer.
 */
public class Opacity extends Widget {

    private double opacity = 1.0;
    private Widget child;

    public void opacity(double v) {
        this.opacity = v;
    }

    public void alwaysIncludeSemantics(boolean v) {
    }

    public void child(Widget v) {
        this.child = v;
    }

    public double getOpacity() {
        return opacity;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public com.codename1.flutter.Element createElement() {
        return new OpacityRenderElement(this);
    }
}
