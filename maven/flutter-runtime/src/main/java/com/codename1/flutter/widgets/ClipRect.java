package com.codename1.flutter.widgets;

import com.codename1.flutter.Clip;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * Clips its child to a rectangle. See {@link ClipRectRenderElement}.
 */
public class ClipRect extends Widget implements HasChild {

    private Object clipper;
    private Clip clipBehavior = Clip.hardEdge;
    private Widget child;

    public void clipper(Object v) {
        this.clipper = v;
    }

    public void clipBehavior(Clip v) {
        this.clipBehavior = v;
    }

    public Clip getClipBehavior() {
        return clipBehavior;
    }

    public void child(Widget v) {
        this.child = v;
    }

    @Override
    public Widget getChild() {
        return child;
    }

    /**
     * {@code Clip.none} means DO NOT CLIP, so it must not get the clipping pane at all —
     * that pane clips regardless of what the behaviour says, which is why asking for no
     * clipping used to cut the content anyway and merely report that it had.
     */
    @Override
    public Element createElement() {
        if (clipBehavior == Clip.none) {
            return new PassThroughRenderElement(this);
        }
        return new ClipRectRenderElement(this);
    }
}
