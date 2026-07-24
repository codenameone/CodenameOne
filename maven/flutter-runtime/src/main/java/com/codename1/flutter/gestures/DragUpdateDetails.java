package com.codename1.flutter.gestures;

import com.codename1.flutter.Offset;

/**
 * The incremental details of a drag — Flutter's {@code DragUpdateDetails}. The
 * reply bottom-drawer reads {@link #primaryDelta()} to drive its
 * AnimationController.
 */
public final class DragUpdateDetails {

    private Offset globalPosition = Offset.zero;
    private Offset localPosition = Offset.zero;
    private Offset delta = Offset.zero;
    private Double primaryDelta;

    public DragUpdateDetails() {
    }

    public DragUpdateDetails(Offset globalPosition, Offset localPosition, Offset delta, Double primaryDelta) {
        this.globalPosition = globalPosition == null ? Offset.zero : globalPosition;
        this.localPosition = localPosition == null ? Offset.zero : localPosition;
        this.delta = delta == null ? Offset.zero : delta;
        this.primaryDelta = primaryDelta;
    }

    public Offset delta() {
        return delta;
    }

    public void delta(Offset v) {
        this.delta = v;
    }

    public Double primaryDelta() {
        return primaryDelta;
    }

    public void primaryDelta(Double v) {
        this.primaryDelta = v;
    }

    public Offset globalPosition() {
        return globalPosition;
    }

    public void globalPosition(Offset v) {
        this.globalPosition = v;
    }

    public Offset localPosition() {
        return localPosition;
    }

    public void localPosition(Offset v) {
        this.localPosition = v;
    }
}
