package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * Rotates its {@code child} by an integral number of quarter turns — Flutter's
 * {@code RotatedBox}. Unlike {@code Transform.rotate}, the rotation also affects layout: a
 * 1- or 3-turn box swaps width and height.
 */
public class RotatedBox extends Widget {

    private long quarterTurns;
    private Widget child;

    public void quarterTurns(long v) {
        this.quarterTurns = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public long getQuarterTurns() {
        return quarterTurns;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Element createElement() {
        return new RotatedBoxRenderElement(this);
    }
}
