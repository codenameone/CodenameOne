package com.codename1.flutter.animation;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * Shared base for the transition and implicitly-animated widgets that wrap a
 * single {@code child} (FadeTransition, ScaleTransition, AnimatedContainer,
 * ...). This pass renders the child through without applying the visual
 * transform — the API shape and child hosting are correct; animated pixels
 * come later. Layout is delegated to {@link PassthroughRenderElement}.
 */
public abstract class AnimatedChildWidget extends Widget {

    private Widget child;

    public void child(Widget v) {
        this.child = v;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Element createElement() {
        return new PassthroughRenderElement(this);
    }
}
