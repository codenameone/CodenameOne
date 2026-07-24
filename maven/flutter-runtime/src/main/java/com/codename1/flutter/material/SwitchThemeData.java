package com.codename1.flutter.material;

/**
 * Material {@code SwitchThemeData}: write-once switch styling. The values are
 * {@link MaterialStateProperty}/density/cursor objects owned by other runtime
 * areas, so they are held opaquely as {@code Object} in this pass. Named Dart
 * constructor parameters map to setter methods.
 */
public class SwitchThemeData {

    private Object thumbColor;
    private Object trackColor;
    private Object trackOutlineColor;
    private Object overlayColor;
    private Object splashRadius;
    private Object materialTapTargetSize;
    private Object thumbIcon;
    private Object mouseCursor;

    public void thumbColor(Object v) {
        this.thumbColor = v;
    }

    public void trackColor(Object v) {
        this.trackColor = v;
    }

    public void trackOutlineColor(Object v) {
        this.trackOutlineColor = v;
    }

    public void overlayColor(Object v) {
        this.overlayColor = v;
    }

    public void splashRadius(Object v) {
        this.splashRadius = v;
    }

    public void materialTapTargetSize(Object v) {
        this.materialTapTargetSize = v;
    }

    public void thumbIcon(Object v) {
        this.thumbIcon = v;
    }

    public void mouseCursor(Object v) {
        this.mouseCursor = v;
    }

    public Object getThumbColor() {
        return thumbColor;
    }

    public Object getTrackColor() {
        return trackColor;
    }
}
