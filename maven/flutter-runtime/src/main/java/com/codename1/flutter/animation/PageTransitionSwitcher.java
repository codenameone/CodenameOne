package com.codename1.flutter.animation;

import dart.core.Duration;

/**
 * Cross-fades between successive {@code child} widgets using a supplied
 * transition — the {@code animations} package's {@code PageTransitionSwitcher}.
 * The {@code transitionBuilder} is a three-argument closure
 * {@code (child, primaryAnimation, secondaryAnimation)}. This pass hosts the
 * current child directly; running the outgoing/incoming transition is deferred
 * (see {@link AnimatedChildWidget}).
 */
public class PageTransitionSwitcher extends AnimatedChildWidget {

    private Duration duration;
    private boolean reverse;
    private Object transitionBuilder;

    public void duration(Duration v) {
        this.duration = v;
    }

    public void reverse(boolean v) {
        this.reverse = v;
    }

    public void transitionBuilder(dart.runtime.Funcs.Func3<com.codename1.flutter.Widget,
            Animation<Double>, Animation<Double>, com.codename1.flutter.Widget> v) {
        this.transitionBuilder = v;
    }

    public Object getTransitionBuilder() {
        return transitionBuilder;
    }
}
