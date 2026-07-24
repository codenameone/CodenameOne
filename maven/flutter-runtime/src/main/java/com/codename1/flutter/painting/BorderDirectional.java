package com.codename1.flutter.painting;

/**
 * A box border whose sides are resolved against the ambient text direction
 * ({@code start}/{@code end} rather than {@code left}/{@code right}) — Flutter's
 * {@code BorderDirectional}. The settings demo draws a leading rule with a
 * {@code start} side. The sides are held as {@code Object} because a
 * {@code BorderSide} is supplied by the widget layer; this pass retains them for
 * the box decoration to paint.
 */
public class BorderDirectional {

    private Object top;
    private Object bottom;
    private Object start;
    private Object end;

    public void top(Object v) {
        this.top = v;
    }

    public void bottom(Object v) {
        this.bottom = v;
    }

    public void start(Object v) {
        this.start = v;
    }

    public void end(Object v) {
        this.end = v;
    }

    public Object getTop() {
        return top;
    }

    public Object getBottom() {
        return bottom;
    }

    public Object getStart() {
        return start;
    }

    public Object getEnd() {
        return end;
    }
}
