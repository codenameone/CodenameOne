package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * Whether (and how) to include its {@code child} in the tree — Flutter's {@code Visibility}.
 *
 * <p>{@code visible: false} shows the {@code replacement} instead — nothing, by
 * default. It used to draw the child regardless, which is the loudest possible
 * reading of "do not show this".</p>
 *
 * <p>{@code maintainState} is not modelled: a hidden child is rebuilt when it
 * comes back rather than kept alive. {@code maintainSize} is honoured only in
 * that a replacement can hold space if one is given.</p>
 */
public class Visibility extends Widget implements HasChild {

    private boolean visible = true;
    private Widget replacement;
    private boolean maintainState;
    private boolean maintainAnimation;
    private boolean maintainSize;
    private boolean maintainSemantics;
    private boolean maintainInteractivity;
    private Widget child;

    public void visible(boolean v) { this.visible = v; }
    public void replacement(Widget v) { this.replacement = v; }
    public void maintainState(boolean v) { this.maintainState = v; }
    public void maintainAnimation(boolean v) { this.maintainAnimation = v; }
    public void maintainSize(boolean v) { this.maintainSize = v; }
    public void maintainSemantics(boolean v) { this.maintainSemantics = v; }
    public void maintainInteractivity(boolean v) { this.maintainInteractivity = v; }
    public boolean getVisible() { return visible; }

    public void child(Widget v) {
        this.child = v;
    }

    @Override
    public Widget getChild() {
        if (visible) {
            return child;
        }
        if (replacement != null) {
            return replacement;
        }
        // Flutter's default replacement is SizedBox.shrink() — an empty box,
        // not the child it was just told to hide.
        SizedBox empty = new SizedBox();
        empty.width(0);
        empty.height(0);
        return empty;
    }

    @Override
    public Element createElement() {
        return new PassThroughRenderElement(this);
    }
}
