package com.codename1.flutter.widgets;

import com.codename1.flutter.EdgeInsets;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * Insets its child to avoid system intrusions (status bar, notch). For this
 * milestone it renders the child unchanged — Codename One's Form already keeps
 * content within the safe area — while accepting the full Flutter parameter
 * set. See {@link PassThroughRenderElement}.
 */
public class SafeArea extends Widget implements HasChild {

    private boolean left = true;
    private boolean top = true;
    private boolean right = true;
    private boolean bottom = true;
    private EdgeInsets minimum;
    private boolean maintainBottomViewPadding;
    private Widget child;

    public void left(boolean v) {
        this.left = v;
    }

    public void top(boolean v) {
        this.top = v;
    }

    public void right(boolean v) {
        this.right = v;
    }

    public void bottom(boolean v) {
        this.bottom = v;
    }

    public void minimum(EdgeInsets v) {
        this.minimum = v;
    }

    public void maintainBottomViewPadding(boolean v) {
        this.maintainBottomViewPadding = v;
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
