package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * Rotates its {@code child} by an integral number of quarter turns — Flutter's
 * {@code RotatedBox}. Unlike {@code Transform.rotate}, the rotation also affects
 * layout (a 1- or 3-turn box swaps width/height). This pass hosts the child
 * un-rotated; the quarter-turn count is captured for a later render pass.
 */
public class RotatedBox extends StatelessWidget {

    private long quarterTurns;
    private Widget child;

    public void quarterTurns(long v) {
        this.quarterTurns = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public long getQuarterTurns() {
        return quarterTurns;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Widget build(BuildContext context) {
        return child;
    }
}
