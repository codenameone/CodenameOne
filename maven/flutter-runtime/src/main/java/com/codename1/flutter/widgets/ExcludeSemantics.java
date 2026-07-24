package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * Drops the semantics of its child subtree. Renders the child unchanged for
 * this milestone. See {@link PassThroughRenderElement}.
 */
public class ExcludeSemantics extends Widget implements HasChild {

    private boolean excluding = true;
    private Widget child;

    public void excluding(boolean v) {
        this.excluding = v;
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
