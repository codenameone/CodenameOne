package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * Forces its {@code child} to a specific aspect ratio — Flutter's {@code AspectRatio}.
 *
 * <p>Structural pass-through for this milestone: the single {@code child}
 * renders unchanged (see {@link PassThroughRenderElement}); the captured
 * parameters are held for a later render pass.</p>
 */
public class AspectRatio extends Widget implements HasChild {

    private double aspectRatio;
    private Widget child;

    public void aspectRatio(double v) { this.aspectRatio = v; }

    public void child(Widget v) {
        this.child = v;
    }

    @Override
    public Widget getChild() {
        return child;
    }

    @Override
    public Element createElement() {
        return new PassThroughRenderElement(this);
    }
}
