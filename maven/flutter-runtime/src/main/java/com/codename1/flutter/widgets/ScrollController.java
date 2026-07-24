package com.codename1.flutter.widgets;

import com.codename1.flutter.animation.Curve;

import dart.async.Future;
import dart.core.Duration;
import dart.runtime.Funcs;

import java.util.ArrayList;
import java.util.List;

/**
 * Controls the offset of a scrollable ({@code ScrollController} in Flutter). The
 * new_gallery desktop carousel reads {@link #offset()} / {@link #position()},
 * calls {@link #animateTo}, and adds a listener to toggle its paging buttons.
 *
 * <p>The controller owns a {@link ScrollPosition}; the mounted scroll render
 * element keeps that position's extents in sync and forwards user scrolls, which
 * notify listeners.</p>
 */
public class ScrollController {

    private final ScrollPosition scrollPosition = new ScrollPosition();
    private final List<Funcs.VoidFunc0> listeners = new ArrayList<Funcs.VoidFunc0>();
    private double initialScrollOffset;
    private boolean keepScrollOffset = true;
    private String debugLabel;
    private boolean attached;

    public ScrollController() {
    }

    // Named-parameter setters.
    public void initialScrollOffset(double v) {
        this.initialScrollOffset = v;
        this.scrollPosition.setPixels(v);
    }

    public void keepScrollOffset(boolean v) {
        this.keepScrollOffset = v;
    }

    public void debugLabel(String v) {
        this.debugLabel = v;
    }

    public double offset() {
        return scrollPosition.pixels();
    }

    public ScrollPosition position() {
        return scrollPosition;
    }

    public boolean hasClients() {
        return attached;
    }

    public Future<Object> animateTo(double offset, Duration duration, Curve curve) {
        scrollPosition.jumpTo(offset);
        notifyListeners();
        return Future.value(null);
    }

    public void jumpTo(double value) {
        scrollPosition.jumpTo(value);
        notifyListeners();
    }

    public void addListener(Funcs.VoidFunc0 listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(Funcs.VoidFunc0 listener) {
        listeners.remove(listener);
    }

    public void dispose() {
        listeners.clear();
        attached = false;
    }

    // ------------------------------------------------------------------
    // Framework plumbing
    // ------------------------------------------------------------------

    void attach() {
        this.attached = true;
    }

    void detach() {
        this.attached = false;
    }

    void notifyListeners() {
        for (Funcs.VoidFunc0 l : new ArrayList<Funcs.VoidFunc0>(listeners)) {
            l.call();
        }
    }
}
