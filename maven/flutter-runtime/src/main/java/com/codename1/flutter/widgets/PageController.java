package com.codename1.flutter.widgets;

import com.codename1.flutter.foundation.Listenable;

import dart.runtime.Funcs;

/**
 * Controls the visible page of a {@link PageView} — Flutter's
 * {@code PageController}. The home carousel reads {@link #page()} and
 * {@code position.haveDimensions} to animate the peeking neighbours. This pass
 * tracks the current page as a plain value; snapping it to a live scroll offset
 * lands with the {@link PageView} renderer.
 */
public class PageController implements Listenable {

    private long initialPage;
    private boolean keepPage = true;
    private double viewportFraction = 1.0;
    private final ScrollPosition position = new ScrollPosition();

    public PageController() {
    }

    public void initialPage(long v) {
        this.initialPage = v;
    }

    public void keepPage(boolean v) {
        this.keepPage = v;
    }

    public void viewportFraction(double v) {
        this.viewportFraction = v;
    }

    /** The current page, possibly fractional while scrolling. */
    public Double page() {
        return (double) initialPage;
    }

    public long initialPage() {
        return initialPage;
    }

    public double viewportFraction() {
        return viewportFraction;
    }

    public ScrollPosition position() {
        return position;
    }

    public boolean hasClients() {
        return false;
    }

    public Object animateToPage(long page, Object duration, Object curve) {
        return null;
    }

    public void jumpToPage(long page) {
    }

    public Object nextPage(Object duration, Object curve) {
        return null;
    }

    public Object previousPage(Object duration, Object curve) {
        return null;
    }

    public void addListener(Funcs.VoidFunc0 listener) {
    }

    public void removeListener(Funcs.VoidFunc0 listener) {
    }

    public void dispose() {
    }
}
