package com.codename1.flutter.material;

/**
 * Material {@code CheckboxThemeData}: write-once checkbox styling. All values
 * here are MaterialStateProperty / border / density objects owned by other
 * runtime areas, so they are held as opaque {@code Object}s in this pass.
 */
public class CheckboxThemeData {

    private Object fillColor;
    private Object checkColor;
    private Object overlayColor;
    private Object materialTapTargetSize;
    private Object shape;
    private Object side;
    private Object visualDensity;
    private Object mouseCursor;
    private Object splashRadius;

    public void fillColor(Object v) {
        this.fillColor = v;
    }

    public void checkColor(Object v) {
        this.checkColor = v;
    }

    public void overlayColor(Object v) {
        this.overlayColor = v;
    }

    public void materialTapTargetSize(Object v) {
        this.materialTapTargetSize = v;
    }

    public void shape(Object v) {
        this.shape = v;
    }

    public void side(Object v) {
        this.side = v;
    }

    public void visualDensity(Object v) {
        this.visualDensity = v;
    }

    public void mouseCursor(Object v) {
        this.mouseCursor = v;
    }

    public void splashRadius(Object v) {
        this.splashRadius = v;
    }
}
