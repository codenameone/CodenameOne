package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * One entry painted into an {@link Overlay} — Flutter's {@code OverlayEntry}.
 * Feature-discovery builds it from a {@code builder}, rebuilds it via
 * {@link #markNeedsBuild()} and tears it down with {@link #remove()}.
 */
public class OverlayEntry {

    private Funcs.Func1<BuildContext, Widget> builder;
    private Boolean opaque;
    private Boolean maintainState;
    private boolean mounted = true;

    public OverlayEntry() {
    }

    public void builder(Funcs.Func1<BuildContext, Widget> v) {
        this.builder = v;
    }

    public void opaque(Boolean v) {
        this.opaque = v;
    }

    public void maintainState(Boolean v) {
        this.maintainState = v;
    }

    public Funcs.Func1<BuildContext, Widget> getBuilder() {
        return builder;
    }

    /** Marks the entry as needing to rebuild its content on the next frame. */
    public void markNeedsBuild() {
    }

    /** Removes this entry from its overlay. */
    public void remove() {
        mounted = false;
    }

    public boolean mounted() {
        return mounted;
    }
}
