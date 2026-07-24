package com.codename1.flutter;

/**
 * Direction-relative corner radii ({@code start}/{@code end} corners) —
 * Flutter's {@code BorderRadiusDirectional}.
 */
public final class BorderRadiusDirectional extends BorderRadiusGeometry {

    public static final BorderRadiusDirectional zero =
            new BorderRadiusDirectional(Radius.zero, Radius.zero, Radius.zero, Radius.zero);

    private final Radius topStart;
    private final Radius topEnd;
    private final Radius bottomStart;
    private final Radius bottomEnd;

    private BorderRadiusDirectional(Radius topStart, Radius topEnd,
                                    Radius bottomStart, Radius bottomEnd) {
        this.topStart = topStart == null ? Radius.zero : topStart;
        this.topEnd = topEnd == null ? Radius.zero : topEnd;
        this.bottomStart = bottomStart == null ? Radius.zero : bottomStart;
        this.bottomEnd = bottomEnd == null ? Radius.zero : bottomEnd;
    }

    public static BorderRadiusDirectional all(Radius radius) {
        return new BorderRadiusDirectional(radius, radius, radius, radius);
    }

    public static BorderRadiusDirectional circular(double radius) {
        return all(Radius.circular(radius));
    }

    public static BorderRadiusDirectional only(Radius topStart, Radius topEnd,
                                               Radius bottomStart, Radius bottomEnd) {
        return new BorderRadiusDirectional(topStart, topEnd, bottomStart, bottomEnd);
    }

    public static BorderRadiusDirectional vertical(Radius top, Radius bottom) {
        return new BorderRadiusDirectional(top, top, bottom, bottom);
    }

    public static BorderRadiusDirectional horizontal(Radius start, Radius end) {
        return new BorderRadiusDirectional(start, end, start, end);
    }

    public Radius topStart() {
        return topStart;
    }

    public Radius topEnd() {
        return topEnd;
    }

    public Radius bottomStart() {
        return bottomStart;
    }

    public Radius bottomEnd() {
        return bottomEnd;
    }
}
