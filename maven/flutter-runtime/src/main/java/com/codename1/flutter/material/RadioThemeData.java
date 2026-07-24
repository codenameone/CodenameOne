package com.codename1.flutter.material;

/**
 * Material {@code RadioThemeData}: write-once radio-button styling. The values
 * are {@link MaterialStateProperty}/density/cursor objects owned by other
 * runtime areas, so they are held opaquely as {@code Object} in this pass.
 * Named Dart constructor parameters map to setter methods.
 */
public class RadioThemeData {

    private Object fillColor;
    private Object overlayColor;
    private Object splashRadius;
    private Object materialTapTargetSize;
    private Object visualDensity;
    private Object mouseCursor;

    public void fillColor(Object v) {
        this.fillColor = v;
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

    public void visualDensity(Object v) {
        this.visualDensity = v;
    }

    public void mouseCursor(Object v) {
        this.mouseCursor = v;
    }

    public Object getFillColor() {
        return fillColor;
    }
}
