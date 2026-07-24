package com.codename1.flutter;

/**
 * An image painted into a {@link BoxDecoration} — Flutter's
 * {@code DecorationImage}. Retained as configuration; painting the decoration
 * image is deferred for this milestone.
 */
public class DecorationImage {

    private ImageProvider image;
    private BoxFit fit;
    private Object alignment;
    private Object colorFilter;
    private Object repeat;
    private double scale = 1.0;
    private double opacity = 1.0;
    private boolean matchTextDirection;

    public void image(ImageProvider v) {
        this.image = v;
    }

    public void fit(BoxFit v) {
        this.fit = v;
    }

    public void alignment(Object v) {
        this.alignment = v;
    }

    public void colorFilter(Object v) {
        this.colorFilter = v;
    }

    public void repeat(Object v) {
        this.repeat = v;
    }

    public void scale(double v) {
        this.scale = v;
    }

    public void opacity(double v) {
        this.opacity = v;
    }

    public void matchTextDirection(boolean v) {
        this.matchTextDirection = v;
    }

    public ImageProvider getImage() {
        return image;
    }

    public BoxFit getFit() {
        return fit;
    }
}
