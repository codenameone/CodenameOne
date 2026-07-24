package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * Whether (and how) to include its {@code child} in the tree — Flutter's {@code Visibility}.
 *
 * <p>Structural pass-through for this milestone: the single {@code child}
 * renders unchanged (see {@link PassThroughRenderElement}); the captured
 * parameters are held for a later render pass.</p>
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
        return child;
    }

    @Override
    public Element createElement() {
        return new PassThroughRenderElement(this);
    }
}
