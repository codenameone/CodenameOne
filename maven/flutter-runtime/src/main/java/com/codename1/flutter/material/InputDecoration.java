package com.codename1.flutter.material;

/**
 * Decoration configuration for a {@link TextField}. M3 renders both
 * {@code labelText} and {@code hintText} through the CN1 hint mechanism
 * (labelText wins when both are set) — a floating label is a later
 * milestone.
 */
public class InputDecoration {

    private String labelText;
    private String hintText;

    public void labelText(String v) {
        this.labelText = v;
    }

    public void hintText(String v) {
        this.hintText = v;
    }

    public String getLabelText() {
        return labelText;
    }

    public String getHintText() {
        return hintText;
    }
}
