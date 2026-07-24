package com.codename1.flutter;

/**
 * Direction-relative edge insets ({@code start}/{@code end} instead of
 * {@code left}/{@code right}) — Flutter's {@code EdgeInsetsDirectional}.
 *
 * <p>It extends {@link EdgeInsets} so it remains assignable to the
 * {@code EdgeInsets}-typed padding parameters the widget stubs declare. Under
 * the default left-to-right text direction {@code start} maps to {@code left}
 * and {@code end} to {@code right}; full bidi resolution is deferred to the
 * render layer.</p>
 */
public final class EdgeInsetsDirectional extends EdgeInsets {

    public static final EdgeInsetsDirectional zero = new EdgeInsetsDirectional(0, 0, 0, 0);

    private final double start;
    private final double end;

    private EdgeInsetsDirectional(double start, double top, double end, double bottom) {
        // LTR mapping: start -> left, end -> right.
        super(start, top, end, bottom);
        this.start = start;
        this.end = end;
    }

    public static EdgeInsetsDirectional all(double value) {
        return new EdgeInsetsDirectional(value, value, value, value);
    }

    public static EdgeInsetsDirectional only(double start, double top, double end, double bottom) {
        return new EdgeInsetsDirectional(start, top, end, bottom);
    }

    public static EdgeInsetsDirectional symmetric(double horizontal, double vertical) {
        return new EdgeInsetsDirectional(horizontal, vertical, horizontal, vertical);
    }

    public static EdgeInsetsDirectional fromSTEB(double start, double top, double end, double bottom) {
        return new EdgeInsetsDirectional(start, top, end, bottom);
    }

    public double start() {
        return start;
    }

    public double end() {
        return end;
    }
}
