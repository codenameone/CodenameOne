package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;
import com.codename1.flutter.Color;

/**
 * Clips/elevates its {@code child} to an arbitrary shape — Flutter's {@code PhysicalShape}.
 *
 * <p>Structural pass-through for this milestone: the single {@code child}
 * renders unchanged (see {@link PassThroughRenderElement}); the captured
 * parameters are held for a later render pass.</p>
 */
public class PhysicalShape extends Widget implements HasChild {

    private Object clipper;
    private Object clipBehavior;
    private double elevation;
    private Color color;
    private Color shadowColor;
    private Widget child;

    public void clipper(Object v) { this.clipper = v; }
    public void clipBehavior(Object v) { this.clipBehavior = v; }
    public void elevation(double v) { this.elevation = v; }
    public void color(Color v) { this.color = v; }
    public void shadowColor(Color v) { this.shadowColor = v; }

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
