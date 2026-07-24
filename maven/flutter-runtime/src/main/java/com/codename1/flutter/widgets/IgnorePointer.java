package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * Prevents its subtree from receiving pointer events — Flutter's {@code IgnorePointer}.
 *
 * <p>Structural pass-through for this milestone: the single {@code child}
 * renders unchanged (see {@link PassThroughRenderElement}); the captured
 * parameters are held for a later render pass.</p>
 */
public class IgnorePointer extends Widget implements HasChild {

    private Boolean ignoring;
    private Boolean ignoringSemantics;
    private Widget child;

    public void ignoring(Boolean v) { this.ignoring = v; }
    public void ignoringSemantics(Boolean v) { this.ignoringSemantics = v; }

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
