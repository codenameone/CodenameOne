package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * Marks a child of Row/Column as flexible: it receives a share of the free
 * main-axis space proportional to its flex factor (default 1). Only has an
 * effect when its render element sits directly below a Flex.
 */
public class Expanded extends Widget {

    private Widget child;
    private long flex = 1;

    public void child(Widget v) {
        this.child = v;
    }

    public void flex(long v) {
        this.flex = v;
    }

    public Widget getChild() {
        return child;
    }

    public long getFlex() {
        return flex;
    }

    @Override
    public Element createElement() {
        return new ExpandedRenderElement(this);
    }
}
