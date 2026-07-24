package com.codename1.flutter;

/**
 * An immutable description of how to paint a box — Flutter's
 * {@code BoxDecoration}. Only the {@link #getColor() background color} and
 * {@link #getShape() shape} are honored by the Codename One runtime for this
 * milestone; border/borderRadius/boxShadow/gradient/image are retained as
 * opaque values (owned by other API categories) but not yet painted.
 */
public class BoxDecoration extends Decoration {

    private Color color;
    private Object image;
    private Object border;
    private Object borderRadius;
    private Object boxShadow;
    private Object gradient;
    private Object backgroundBlendMode;
    private BoxShape shape = BoxShape.rectangle;

    public void color(Color v) {
        this.color = v;
    }

    public void image(Object v) {
        this.image = v;
    }

    public void border(Object v) {
        this.border = v;
    }

    public void borderRadius(Object v) {
        this.borderRadius = v;
    }

    public void boxShadow(Object v) {
        this.boxShadow = v;
    }

    public void gradient(Object v) {
        this.gradient = v;
    }

    public void backgroundBlendMode(Object v) {
        this.backgroundBlendMode = v;
    }

    public void shape(BoxShape v) {
        this.shape = v == null ? BoxShape.rectangle : v;
    }

    public Color getColor() {
        return color;
    }

    public Object getImage() {
        return image;
    }

    public Object getBorder() {
        return border;
    }

    public Object getBorderRadius() {
        return borderRadius;
    }

    public Object getBoxShadow() {
        return boxShadow;
    }

    public Object getGradient() {
        return gradient;
    }

    public Object getBackgroundBlendMode() {
        return backgroundBlendMode;
    }

    public BoxShape getShape() {
        return shape;
    }
}
