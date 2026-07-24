package com.codename1.flutter.gestures;

import com.codename1.flutter.Offset;

/**
 * The details of a tap-down event — Flutter's {@code TapDownDetails}.
 */
public final class TapDownDetails {

    private Offset globalPosition = Offset.zero;
    private Offset localPosition = Offset.zero;
    private Object kind;

    public TapDownDetails() {
    }

    public TapDownDetails(Offset globalPosition, Offset localPosition, Object kind) {
        this.globalPosition = globalPosition == null ? Offset.zero : globalPosition;
        this.localPosition = localPosition == null ? Offset.zero : localPosition;
        this.kind = kind;
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

    public Object kind() {
        return kind;
    }

    public void kind(Object v) {
        this.kind = v;
    }
}
