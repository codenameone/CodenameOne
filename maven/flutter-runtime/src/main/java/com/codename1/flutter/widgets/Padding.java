package com.codename1.flutter.widgets;

import com.codename1.flutter.EdgeInsets;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * Insets its child by the given edge padding (logical pixels).
 */
public class Padding extends Widget {

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
        return new PaddingRenderElement(this);
    }
}
