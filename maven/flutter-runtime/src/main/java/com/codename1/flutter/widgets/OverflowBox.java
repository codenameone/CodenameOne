package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * Lets its child overflow its own constraints — Flutter's {@code OverflowBox}.
 *
 * <p>Structural pass-through for this milestone: the single {@code child}
 * renders unchanged; the imposed min/max constraints and alignment are held
 * for a later render pass.</p>
 */
public class OverflowBox extends Widget implements HasChild {

    private Object alignment;
    private Double minWidth;
    private Double maxWidth;
    private Double minHeight;
    private Double maxHeight;
    private Widget child;

    public void alignment(Object v) { this.alignment = v; }
    public void minWidth(double v) { this.minWidth = v; }
    public void maxWidth(double v) { this.maxWidth = v; }
    public void minHeight(double v) { this.minHeight = v; }
    public void maxHeight(double v) { this.maxHeight = v; }

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
