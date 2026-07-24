package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * Sizes its child to a fraction of the available space — Flutter's
 * {@code FractionallySizedBox}. {@code widthFactor}/{@code heightFactor} are
 * multiples of the incoming max extent on each axis (null leaves that axis
 * loose); the child is positioned by {@code alignment} (default center).
 */
public class FractionallySizedBox extends Widget {

    private Object alignment;
    private Double widthFactor;
    private Double heightFactor;
    private Widget child;

    public void alignment(Object v) {
        this.alignment = v;
    }

    public void widthFactor(double v) {
        this.widthFactor = v;
    }

    public void heightFactor(double v) {
        this.heightFactor = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Object getAlignment() {
        return alignment;
    }

    public Double getWidthFactor() {
        return widthFactor;
    }

    public Double getHeightFactor() {
        return heightFactor;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Element createElement() {
        return new FractionallySizedBoxRenderElement(this);
    }
}
