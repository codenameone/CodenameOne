package com.codename1.flutter.animation;

/**
 * An {@link Animatable} that maps its parametric value through a {@link Curve}
 * — Flutter's {@code CurveTween}, typically chained in front of another tween
 * ({@code tween.chain(CurveTween(curve: Curves.easeOut))}).
 */
public class CurveTween extends Animatable<Double> {

    private Curve curve = Curves.linear;

    /** Named-parameter setter for the Dart {@code curve:} argument. */
    public void curve(Curve v) {
        this.curve = v == null ? Curves.linear : v;
    }

    public Curve curve() {
        return curve;
    }

    @Override
    public Double transform(double t) {
        return curve.transform(t);
    }
}
