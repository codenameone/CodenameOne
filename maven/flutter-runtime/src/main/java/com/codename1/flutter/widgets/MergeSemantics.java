package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * Merges the semantics of its child subtree into one node. Renders the child
 * unchanged for this milestone. See {@link PassThroughRenderElement}.
 */
public class MergeSemantics extends Widget implements HasChild {

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
