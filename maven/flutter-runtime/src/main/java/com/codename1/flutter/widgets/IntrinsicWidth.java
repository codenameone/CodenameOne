package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * Sizes its {@code child} to the child's intrinsic width — Flutter's {@code IntrinsicWidth}.
 *
 * <p>Structural pass-through for this milestone: the single {@code child}
 * renders unchanged (see {@link PassThroughRenderElement}); the captured
 * parameters are held for a later render pass.</p>
 */
public class IntrinsicWidth extends Widget implements HasChild {

    private double stepWidth;
    private double stepHeight;
    private Widget child;

    public void stepWidth(double v) { this.stepWidth = v; }
    public void stepHeight(double v) { this.stepHeight = v; }

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
