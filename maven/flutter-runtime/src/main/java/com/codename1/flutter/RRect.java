package com.codename1.flutter;

/**
 * A rounded rectangle: an axis-aligned {@link Rect} with a {@link Radius} at
 * each corner — Flutter's dart:ui {@code RRect}.
 */
public final class RRect {

    private final Rect rect;
    private final Radius topLeft;
    private final Radius topRight;
    private final Radius bottomLeft;
    private final Radius bottomRight;

    private RRect(Rect rect, Radius topLeft, Radius topRight, Radius bottomLeft, Radius bottomRight) {
        this.rect = rect;
        this.topLeft = topLeft == null ? Radius.zero : topLeft;
        this.topRight = topRight == null ? Radius.zero : topRight;
        this.bottomLeft = bottomLeft == null ? Radius.zero : bottomLeft;
        this.bottomRight = bottomRight == null ? Radius.zero : bottomRight;
    }

    public static RRect fromRectAndRadius(Rect rect, Radius radius) {
        return new RRect(rect, radius, radius, radius, radius);
    }

    public static RRect fromLTRBR(double left, double top, double right, double bottom, Radius radius) {
        return new RRect(Rect.fromLTRB(left, top, right, bottom), radius, radius, radius, radius);
    }

    public static RRect fromRectAndCorners(Rect rect, Radius topLeft, Radius topRight,
                                           Radius bottomLeft, Radius bottomRight) {
        return new RRect(rect, topLeft, topRight, bottomLeft, bottomRight);
    }

    public Rect outerRect() {
        return rect;
    }

    public Radius tlRadius() {
        return topLeft;
    }

    public Radius trRadius() {
        return topRight;
    }

    public Radius blRadius() {
        return bottomLeft;
    }

    public Radius brRadius() {
        return bottomRight;
    }

    /**
     * {@code RRect.middleRect}: the rectangle inside the rounded corners, i.e.
     * the outer rect inset on each edge by the larger of the two corner radii
     * touching that edge.
     */
    public Rect middleRect() {
        return Rect.fromLTRB(
                rect.left() + Math.max(bottomLeft.x(), topLeft.x()),
                rect.top() + Math.max(topLeft.y(), topRight.y()),
                rect.right() - Math.max(topRight.x(), bottomRight.x()),
                rect.bottom() - Math.max(bottomRight.y(), bottomLeft.y()));
    }
}
