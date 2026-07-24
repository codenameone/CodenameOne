package com.codename1.flutter.widgets;

import com.codename1.flutter.Clip;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * Clips its child with a rounded rectangle. Clipping is not yet applied; the
 * child renders unchanged. See {@link PassThroughRenderElement}.
 */
public class ClipRRect extends Widget implements HasChild {

    private Object borderRadius;
    private Object clipper;
    private Clip clipBehavior = Clip.antiAlias;
    private Widget child;

    public void borderRadius(Object v) {
        this.borderRadius = v;
    }

    public void clipper(Object v) {
        this.clipper = v;
    }

    public void clipBehavior(Clip v) {
        this.clipBehavior = v;
    }

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
