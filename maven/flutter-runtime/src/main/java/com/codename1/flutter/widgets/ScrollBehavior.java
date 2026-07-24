package com.codename1.flutter.widgets;

/**
 * Describes how scrollables should behave app-wide — Flutter's
 * {@code ScrollBehavior}: which input devices drag, whether scrollbars and
 * overscroll indicators appear, and the default physics. {@link #copyWith}
 * produces a derived behaviour with selected properties overridden.
 */
public class ScrollBehavior {

    private Boolean scrollbars;
    private Boolean overscroll;

    public ScrollBehavior() {
    }

    /**
     * {@code ScrollBehavior.copyWith}: a copy of this behaviour with the given
     * properties overridden. Unmodelled properties are accepted and ignored.
     */
    public ScrollBehavior copyWith(Boolean scrollbars, Boolean overscroll,
            Object physics, Object platform, Object dragDevices) {
        ScrollBehavior b = newInstance();
        b.scrollbars = scrollbars != null ? scrollbars : this.scrollbars;
        b.overscroll = overscroll != null ? overscroll : this.overscroll;
        return b;
    }

    /** Allows subclasses (e.g. MaterialScrollBehavior) to preserve their type. */
    protected ScrollBehavior newInstance() {
        return new ScrollBehavior();
    }

    public Boolean getScrollbars() {
        return scrollbars;
    }

    public Boolean getOverscroll() {
        return overscroll;
    }
}
