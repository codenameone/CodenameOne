package com.codename1.flutter;

/**
 * A border drawn around a box, with an independent {@link BorderSide} on each
 * edge — Flutter's {@code Border}.
 */
public final class Border extends BoxBorder {

    private BorderSide top = BorderSide.none;
    private BorderSide right = BorderSide.none;
    private BorderSide bottom = BorderSide.none;
    private BorderSide left = BorderSide.none;

    public Border() {
    }

    /** {@code Border.all(color: ..., width: ..., style: ...)}. */
    public static Border all(Color color, double width, BorderStyle style,
            Double strokeAlign) {
        BorderSide side = new BorderSide();
        if (color != null) {
            side.color(color);
        }
        side.width(width);
        side.style(style == null ? BorderStyle.solid : style);
        Border b = new Border();
        b.top = side;
        b.right = side;
        b.bottom = side;
        b.left = side;
        return b;
    }

    /** {@code Border.symmetric(vertical: ..., horizontal: ...)}. */
    public static Border symmetric(BorderSide vertical, BorderSide horizontal) {
        Border b = new Border();
        BorderSide v = vertical == null ? BorderSide.none : vertical;
        BorderSide h = horizontal == null ? BorderSide.none : horizontal;
        b.top = v;
        b.bottom = v;
        b.left = h;
        b.right = h;
        return b;
    }

    public void top(BorderSide v) {
        this.top = v == null ? BorderSide.none : v;
    }

    public void right(BorderSide v) {
        this.right = v == null ? BorderSide.none : v;
    }

    public void bottom(BorderSide v) {
        this.bottom = v == null ? BorderSide.none : v;
    }

    public void left(BorderSide v) {
        this.left = v == null ? BorderSide.none : v;
    }

    public BorderSide top() {
        return top;
    }

    public BorderSide right() {
        return right;
    }

    public BorderSide bottom() {
        return bottom;
    }

    public BorderSide left() {
        return left;
    }
}
