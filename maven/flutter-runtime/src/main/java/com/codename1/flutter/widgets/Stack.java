package com.codename1.flutter.widgets;

import com.codename1.flutter.Alignment;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

import dart.core.DartList;

/**
 * Overlaps its children: non-positioned children are placed by the stack's
 * alignment, {@link Positioned} children resolve their insets against the
 * stack bounds. Later children paint on top (z-order = child order).
 */
public class Stack extends Widget {

    private Alignment alignment;
    private DartList<Widget> children;
    private com.codename1.flutter.StackFit fit;
    private com.codename1.flutter.Clip clipBehavior;

    public void fit(com.codename1.flutter.StackFit v) {
        this.fit = v;
    }

    public void clipBehavior(com.codename1.flutter.Clip v) {
        this.clipBehavior = v;
    }

    public void alignment(Alignment v) {
        this.alignment = v;
    }

    public void children(DartList<Widget> v) {
        this.children = v;
    }

    public Alignment getAlignment() {
        return alignment;
    }

    public DartList<Widget> getChildren() {
        return children;
    }

    @Override
    public Element createElement() {
        return new StackRenderElement(this);
    }
}
