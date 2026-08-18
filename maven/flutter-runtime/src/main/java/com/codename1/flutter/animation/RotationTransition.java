package com.codename1.flutter.animation;

import com.codename1.flutter.Alignment;
import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Transform;

/**
 * Rotates its child about its centre from an {@link Animation} measured in TURNS —
 * Flutter's {@code RotationTransition}, where 1.0 is a full revolution.
 */
public class RotationTransition extends AnimatedChildWidget {

    private Animation<Double> turns;
    private Alignment alignment;

    public void turns(Animation<Double> v) {
        this.turns = v;
        listenable(v);
    }

    public void alignment(Alignment v) {
        this.alignment = v;
    }

    public Animation<Double> getTurns() {
        return turns;
    }

    @Override
    public Widget build(BuildContext context) {
        double radians = valueOf(turns, 0.0) * 2 * Math.PI;
        return Transform.rotate(null, radians, null, alignment, null, null, getChild());
    }
}
