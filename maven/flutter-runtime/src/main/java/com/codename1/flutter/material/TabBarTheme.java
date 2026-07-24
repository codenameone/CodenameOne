package com.codename1.flutter.material;

import com.codename1.flutter.Color;
import com.codename1.flutter.TextStyle;

/**
 * Material {@code TabBarTheme}: write-once tab-bar styling. Named Dart
 * constructor parameters map to setter methods; unset values stay null.
 */
public class TabBarTheme {

    private Color indicatorColor;
    private Color labelColor;
    private Color unselectedLabelColor;
    private TextStyle labelStyle;
    private TextStyle unselectedLabelStyle;
    private Object indicator;
    private Object indicatorSize;
    private Object labelPadding;
    private Object overlayColor;
    private Object dividerColor;

    public void indicatorColor(Color v) {
        this.indicatorColor = v;
    }

    public void labelColor(Color v) {
        this.labelColor = v;
    }

    public void unselectedLabelColor(Color v) {
        this.unselectedLabelColor = v;
    }

    public void labelStyle(TextStyle v) {
        this.labelStyle = v;
    }

    public void unselectedLabelStyle(TextStyle v) {
        this.unselectedLabelStyle = v;
    }

    public void indicator(Object v) {
        this.indicator = v;
    }

    public void indicatorSize(Object v) {
        this.indicatorSize = v;
    }

    public void labelPadding(Object v) {
        this.labelPadding = v;
    }

    public void overlayColor(Object v) {
        this.overlayColor = v;
    }

    public void dividerColor(Object v) {
        this.dividerColor = v;
    }

    public Color labelColor() {
        return labelColor;
    }
}
