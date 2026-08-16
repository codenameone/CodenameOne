package com.codename1.flutter.widgets;

/**
 * A read-only description of a scrollable's content and viewport extents
 * ({@code ScrollMetrics} in Flutter). {@link ScrollPosition} and scroll
 * notifications expose it; new_gallery's carousel physics read {@link #pixels()}
 * and {@link #maxScrollExtent()} to decide button visibility and target offsets.
 *
 * <p>This base holds the extents in fields (defaulting to zero) so subtypes and
 * the render layer can update them as the scrollable lays out.</p>
 */
public class ScrollMetrics {

    protected double pixels;
    protected double minScrollExtent;
    protected double maxScrollExtent;
    protected double viewportDimension;
    protected boolean hasContentDimensions;
    protected boolean hasPixels;
    protected boolean hasViewportDimension;

    public double pixels() {
        return pixels;
    }

    public double minScrollExtent() {
        return minScrollExtent;
    }

    public double maxScrollExtent() {
        return maxScrollExtent;
    }

    public double viewportDimension() {
        return viewportDimension;
    }

    /** Amount of content scrolled off the leading edge. */
    public double extentBefore() {
        return Math.max(0.0, pixels - minScrollExtent);
    }

    /** Amount of content still below the trailing edge. */
    public double extentAfter() {
        return Math.max(0.0, maxScrollExtent - pixels);
    }

    /** Amount of content currently visible in the viewport. */
    public double extentInside() {
        return viewportDimension;
    }

    /** Whether the scrollable is at its minimum or maximum extent. */
    public boolean atEdge() {
        return pixels <= minScrollExtent || pixels >= maxScrollExtent;
    }

    /**
     * Whether the scroll offset has been dragged PAST an extent — Flutter's
     * {@code outOfRange}. Distinct from {@link #atEdge()}: sitting exactly on the edge is
     * in range, and the physics leave a drag untouched until it actually overshoots.
     */
    public boolean outOfRange() {
        return pixels < minScrollExtent || pixels > maxScrollExtent;
    }

    public boolean hasContentDimensions() {
        return hasContentDimensions;
    }

    public boolean hasPixels() {
        return hasPixels;
    }

    public boolean hasViewportDimension() {
        return hasViewportDimension;
    }
}
