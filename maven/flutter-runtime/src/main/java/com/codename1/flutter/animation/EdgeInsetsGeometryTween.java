package com.codename1.flutter.animation;

/**
 * A {@link Tween} over {@code EdgeInsetsGeometry} - Flutter's {@code EdgeInsetsGeometryTween}.
 *
 * <p>Insets that step relayout the subtree in one jump at the midpoint instead of
 * easing it.</p>
 */
public class EdgeInsetsGeometryTween extends Tween<com.codename1.flutter.EdgeInsetsGeometry> {

    @Override
    public com.codename1.flutter.EdgeInsetsGeometry lerp(double t) {
        com.codename1.flutter.EdgeInsetsGeometry b = begin();
        com.codename1.flutter.EdgeInsetsGeometry e = end();
        if (b instanceof com.codename1.flutter.EdgeInsets
                || e instanceof com.codename1.flutter.EdgeInsets) {
            return com.codename1.flutter.EdgeInsets.lerp(
                    (com.codename1.flutter.EdgeInsets) (b instanceof com.codename1.flutter.EdgeInsets
                            ? b : null),
                    (com.codename1.flutter.EdgeInsets) (e instanceof com.codename1.flutter.EdgeInsets
                            ? e : null),
                    t);
        }
        return super.lerp(t);
    }
}
