package com.codename1.flutter.animation;

import com.codename1.flutter.ComposedElement;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * Element for {@link AnimatedBuilder}: subscribes to the widget's animation on
 * mount and rebuilds (re-invoking the builder) on every notification, mirroring
 * Flutter's {@code AnimatedWidget}/{@code _AnimatedState} listen-and-rebuild.
 */
public class AnimatedBuilderElement extends ComposedElement {

    private com.codename1.flutter.foundation.Listenable listened;
    private final Funcs.VoidFunc0 handler = new Funcs.VoidFunc0() {
        @Override
        public void call() {
            markNeedsBuild();
        }
    };

    public AnimatedBuilderElement(AnimatedBuilder widget) {
        super(widget);
    }

    @Override
    public void mount(Element parent, int slot) {
        super.mount(parent, slot);
        subscribe();
    }

    @Override
    public void update(Widget newWidget) {
        unsubscribe();
        super.update(newWidget);
        subscribe();
    }

    @Override
    public void unmount() {
        unsubscribe();
        super.unmount();
    }

    private void subscribe() {
        listened = ((AnimatedBuilder) widget()).getAnimation();
        if (listened != null) {
            listened.addListener(handler);
        }
    }

    private void unsubscribe() {
        if (listened != null) {
            listened.removeListener(handler);
            listened = null;
        }
    }

    @Override
    protected Widget build() {
        AnimatedBuilder w = (AnimatedBuilder) widget();
        Funcs.Func2<com.codename1.flutter.BuildContext, Widget, Widget> b = w.getBuilder();
        if (b == null) {
            return null;
        }
        return b.call(this, w.getChild());
    }
}
