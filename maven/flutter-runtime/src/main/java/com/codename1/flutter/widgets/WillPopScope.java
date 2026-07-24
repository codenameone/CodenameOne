package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * Intercepts the system back gesture, consulting {@code onWillPop} before the
 * route is popped — Flutter's {@code WillPopScope}. Structural pass-through for
 * this milestone: the {@code child} renders unchanged and the callback is
 * captured (not yet wired to Codename One's back command).
 */
public class WillPopScope extends Widget implements HasChild {

    private Funcs.Func0<Object> onWillPop;
    private Widget child;

    public void onWillPop(Funcs.Func0<Object> v) {
        this.onWillPop = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Funcs.Func0<Object> getOnWillPop() {
        return onWillPop;
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
