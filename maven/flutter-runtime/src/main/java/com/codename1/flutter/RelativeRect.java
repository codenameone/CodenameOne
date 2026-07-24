package com.codename1.flutter;

import com.codename1.flutter.rendering.Size;

/**
 * A rectangle described as insets from the four edges of a containing box —
 * Flutter's {@code RelativeRect} (used by Positioned/Stack and the
 * RelativeRectTween).
 */
public final class RelativeRect {

    public static final RelativeRect fill = new RelativeRect(0, 0, 0, 0);

    private final double left;
    private final double top;
    private final double right;
    private final double bottom;

    private RelativeRect(double left, double top, double right, double bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public static RelativeRect fromLTRB(double left, double top, double right, double bottom) {
        return new RelativeRect(left, top, right, bottom);
    }

    public static RelativeRect fromRect(Rect rect, Rect container) {
        return fromLTRB(
                rect.left() - container.left(),
                rect.top() - container.top(),
                container.right() - rect.right(),
                container.bottom() - rect.bottom());
    }

    public static RelativeRect fromSize(Rect rect, Size container) {
        return fromLTRB(
                rect.left(),
                rect.top(),
                container.width() - rect.right(),
                container.height() - rect.bottom());
    }

    public double left() {
        return left;
    }

    public double top() {
        return top;
    }

    public double right() {
        return right;
    }

    public double bottom() {
        return bottom;
    }

    public Rect toRect(Rect container) {
        return Rect.fromLTRB(
                left + container.left(),
                top + container.top(),
                container.right() - right,
                container.bottom() - bottom);
    }

    @Override
    public String toString() {
        return "RelativeRect.fromLTRB(" + left + ", " + top + ", " + right + ", " + bottom + ")";
    }
}
