package com.codename1.flutter.gestures;

import com.codename1.flutter.Offset;

/**
 * The details at the start of a scale/pan gesture — Flutter's
 * {@code ScaleStartDetails}.
 */
public final class ScaleStartDetails {

    private Offset focalPoint = Offset.zero;
    private Offset localFocalPoint = Offset.zero;
    private long pointerCount;

    public ScaleStartDetails() {
    }

    public ScaleStartDetails(Offset focalPoint, Offset localFocalPoint, long pointerCount) {
        this.focalPoint = focalPoint == null ? Offset.zero : focalPoint;
        this.localFocalPoint = localFocalPoint == null ? Offset.zero : localFocalPoint;
        this.pointerCount = pointerCount;
    }

    public Offset focalPoint() {
        return focalPoint;
    }

    public void focalPoint(Offset v) {
        this.focalPoint = v;
    }

    public Offset localFocalPoint() {
        return localFocalPoint;
    }

    public void localFocalPoint(Offset v) {
        this.localFocalPoint = v;
    }

    public Offset globalPosition() {
        return focalPoint;
    }

    public Offset localPosition() {
        return localFocalPoint;
    }

    public long pointerCount() {
        return pointerCount;
    }

    public void pointerCount(long v) {
        this.pointerCount = v;
    }
}
