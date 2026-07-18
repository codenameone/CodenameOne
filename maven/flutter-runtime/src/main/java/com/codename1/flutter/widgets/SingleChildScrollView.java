package com.codename1.flutter.widgets;

import com.codename1.flutter.EdgeInsets;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * Makes its child scrollable along the vertical axis: the child subtree
 * becomes a real CN1 scrollable container boundary laid out with an
 * unbounded main axis inside. Horizontal scrolling is a later milestone
 * (the stub declares no scrollDirection yet).
 */
public class SingleChildScrollView extends Widget {

    private EdgeInsets padding;
    private Widget child;

    public void padding(EdgeInsets v) {
        this.padding = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public EdgeInsets getPadding() {
        return padding;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Element createElement() {
        return new SingleChildScrollViewRenderElement(this);
    }
}
