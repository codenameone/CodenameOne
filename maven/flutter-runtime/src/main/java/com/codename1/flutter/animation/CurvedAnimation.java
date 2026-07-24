package com.codename1.flutter.animation;

import dart.runtime.Funcs;

/**
 * An {@link Animation} that runs its {@code parent}'s value through a
 * {@link Curve} (and an optional {@code reverseCurve} while the parent is
 * running backward) — Flutter's {@code CurvedAnimation}. Status and listener
 * registration forward to the parent so dependents rebuild on every tick.
 */
public class CurvedAnimation extends Animation<Double> {

    private Animation<Double> parent;
    private Curve curve = Curves.linear;
    private Curve reverseCurve;

    /** Named-parameter setter for {@code parent:}. */
    public void parent(Animation<Double> v) {
        this.parent = v;
    }

    /** Named-parameter setter for {@code curve:}. */
    public void curve(Curve v) {
        this.curve = v == null ? Curves.linear : v;
    }

    /** Named-parameter setter for {@code reverseCurve:}. */
    public void reverseCurve(Curve v) {
        this.reverseCurve = v;
    }

    @Override
    public Double value() {
        double t = parent == null ? 0.0 : parent.value();
        Curve active = curve;
        if (reverseCurve != null && parent != null && parent.status() == AnimationStatus.reverse) {
            active = reverseCurve;
        }
        return active.transform(t);
    }

    @Override
    public AnimationStatus status() {
        return parent == null ? AnimationStatus.dismissed : parent.status();
    }

    @Override
    public void addListener(Funcs.VoidFunc0 listener) {
        if (parent != null) {
            parent.addListener(listener);
        }
    }

    @Override
    public void removeListener(Funcs.VoidFunc0 listener) {
        if (parent != null) {
            parent.removeListener(listener);
        }
    }

    @Override
    public void addStatusListener(Funcs.VoidFunc1<AnimationStatus> listener) {
        if (parent != null) {
            parent.addStatusListener(listener);
        }
    }

    @Override
    public void removeStatusListener(Funcs.VoidFunc1<AnimationStatus> listener) {
        if (parent != null) {
            parent.removeStatusListener(listener);
        }
    }
}
