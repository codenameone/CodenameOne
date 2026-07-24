package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * Isolates its subtree onto its own layer for cheaper repaints — Flutter's {@code RepaintBoundary}.
 *
 * <p>Structural pass-through for this milestone: the single {@code child}
 * renders unchanged (see {@link PassThroughRenderElement}); the captured
 * parameters are held for a later render pass.</p>
 */
public class RepaintBoundary extends Widget implements HasChild {

    private Widget child;

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
