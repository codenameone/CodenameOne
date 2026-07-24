package com.codename1.flutter.animation;

import com.codename1.flutter.Alignment;

/**
 * Animates the rotation (in turns) of its child — Flutter's
 * {@code RotationTransition}. This pass hosts the child; the rotation
 * transform is deferred.
 */
public class RotationTransition extends AnimatedChildWidget {

    private Animation<Double> turns;
    private Alignment alignment;

    public void turns(Animation<Double> v) {
        this.turns = v;
    }

    public void alignment(Alignment v) {
        this.alignment = v;
    }

    public Animation<Double> getTurns() {
        return turns;
    }
}
