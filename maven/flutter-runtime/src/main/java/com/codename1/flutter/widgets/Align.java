package com.codename1.flutter.widgets;

import com.codename1.flutter.Alignment;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * Aligns its child within itself per an {@link Alignment} (default center).
 * Expands to the incoming constraints when they are bounded, otherwise sizes
 * to the child.
 */
public class Align extends Widget {

    private Alignment alignment;
    private Widget child;
    private Double widthFactor;
    private Double heightFactor;

    public void widthFactor(Double v) {
        this.widthFactor = v;
    }

    public void heightFactor(Double v) {
        this.heightFactor = v;
    }

    public Double getWidthFactor() {
        return widthFactor;
    }

    public Double getHeightFactor() {
        return heightFactor;
    }

    public void alignment(Alignment v) {
        this.alignment = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Alignment getAlignment() {
        return alignment;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Element createElement() {
        return new AlignRenderElement(this);
    }
}
