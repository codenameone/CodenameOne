package com.codename1.flutter.gestures;

import com.codename1.flutter.Offset;

/**
 * The details at the start of a drag — Flutter's {@code DragStartDetails}.
 */
public final class DragStartDetails {

    private Offset globalPosition = Offset.zero;
    private Offset localPosition = Offset.zero;

    public DragStartDetails() {
    }

    public DragStartDetails(Offset globalPosition, Offset localPosition) {
        this.globalPosition = globalPosition == null ? Offset.zero : globalPosition;
        this.localPosition = localPosition == null ? Offset.zero : localPosition;
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
