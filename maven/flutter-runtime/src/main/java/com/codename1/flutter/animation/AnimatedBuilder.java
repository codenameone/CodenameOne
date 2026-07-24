package com.codename1.flutter.animation;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * Rebuilds via a {@code builder} callback whenever its {@code animation}
 * notifies — Flutter's {@code AnimatedBuilder}. The optional {@code child} is
 * an optimization handed back to the builder unchanged.
 */
public class AnimatedBuilder extends Widget {

    private com.codename1.flutter.foundation.Listenable animation;
    private Funcs.Func2<BuildContext, Widget, Widget> builder;
    private Widget child;

    public void animation(com.codename1.flutter.foundation.Listenable v) {
        this.animation = v;
    }

    public void builder(Funcs.Func2<BuildContext, Widget, Widget> v) {
        this.builder = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public com.codename1.flutter.foundation.Listenable getAnimation() {
        return animation;
    }

    public Funcs.Func2<BuildContext, Widget, Widget> getBuilder() {
        return builder;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Element createElement() {
        return new AnimatedBuilderElement(this);
    }
}
