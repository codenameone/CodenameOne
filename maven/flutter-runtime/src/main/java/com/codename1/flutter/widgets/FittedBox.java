package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * Scales and positions its {@code child} within itself — Flutter's {@code FittedBox}.
 *
 * <p>Structural pass-through for this milestone: the single {@code child}
 * renders unchanged (see {@link PassThroughRenderElement}); the captured
 * parameters are held for a later render pass.</p>
 */
public class FittedBox extends Widget implements HasChild {

    private Object fit;
    private Object alignment;
    private Object clipBehavior;
    private Widget child;

    public void fit(Object v) { this.fit = v; }
    public void alignment(Object v) { this.alignment = v; }
    public void clipBehavior(Object v) { this.clipBehavior = v; }

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
