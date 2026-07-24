package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * Excludes its subtree from focus traversal — Flutter's {@code ExcludeFocus}.
 *
 * <p>Structural pass-through for this milestone: the single {@code child}
 * renders unchanged (see {@link PassThroughRenderElement}); the captured
 * parameters are held for a later render pass.</p>
 */
public class ExcludeFocus extends Widget implements HasChild {

    private boolean excluding = true;
    private Widget child;

    public void excluding(boolean v) { this.excluding = v; }

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
