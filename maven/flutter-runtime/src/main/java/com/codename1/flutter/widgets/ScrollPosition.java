package com.codename1.flutter.widgets;

import com.codename1.flutter.animation.Curve;

import dart.async.Future;
import dart.core.Duration;

/**
 * The live scroll offset of a single scrollable ({@code ScrollPosition} in
 * Flutter), extending {@link ScrollMetrics} with mutation. new_gallery reads
 * {@code controller.position.maxScrollExtent} / {@code .haveDimensions}. Actual
 * animated scrolling is driven by the mounted scroll render element; here the
 * pixel offset is updated synchronously.
 */
public class ScrollPosition extends ScrollMetrics {

    /** Whether the viewport and content dimensions are known yet. */
    public boolean haveDimensions() {
        return hasContentDimensions && hasViewportDimension;
    }

    /** Animate to {@code to}; completes immediately in the M1 model. */
    public Future<Object> animateTo(double to, Duration duration, Curve curve) {
        jumpTo(to);
        return Future.value(null);
    }

    public void jumpTo(double value) {
        this.pixels = Math.max(minScrollExtent, Math.min(maxScrollExtent, value));
        this.hasPixels = true;
    }

    // ------------------------------------------------------------------
    // Framework plumbing
    // ------------------------------------------------------------------

    void applyContentDimensions(double min, double max) {
        this.minScrollExtent = min;
        this.maxScrollExtent = max;
        this.hasContentDimensions = true;
    }

    void applyViewportDimension(double dim) {
        this.viewportDimension = dim;
        this.hasViewportDimension = true;
    }

    void setPixels(double p) {
        this.pixels = p;
        this.hasPixels = true;
    }
}
