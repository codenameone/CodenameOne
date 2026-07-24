package com.codename1.flutter.material;

import com.codename1.flutter.EdgeInsets;
import com.codename1.flutter.TextStyle;

import dart.core.Duration;

/**
 * Material {@code TooltipThemeData}: write-once tooltip styling. Named Dart
 * constructor parameters map to setter methods; unset values stay null.
 */
public class TooltipThemeData {

    private Double height;
    private EdgeInsets padding;
    private EdgeInsets margin;
    private Double verticalOffset;
    private Boolean preferBelow;
    private Boolean excludeFromSemantics;
    private Object decoration;
    private TextStyle textStyle;
    private Object textAlign;
    private Duration waitDuration;
    private Duration showDuration;
    private Object triggerMode;
    private Boolean enableFeedback;

    public void height(double v) {
        this.height = v;
    }

    public void padding(EdgeInsets v) {
        this.padding = v;
    }

    public void margin(EdgeInsets v) {
        this.margin = v;
    }

    public void verticalOffset(double v) {
        this.verticalOffset = v;
    }

    public void preferBelow(boolean v) {
        this.preferBelow = v;
    }

    public void excludeFromSemantics(boolean v) {
        this.excludeFromSemantics = v;
    }

    public void decoration(Object v) {
        this.decoration = v;
    }

    public void textStyle(TextStyle v) {
        this.textStyle = v;
    }

    public void textAlign(Object v) {
        this.textAlign = v;
    }

    public void waitDuration(Duration v) {
        this.waitDuration = v;
    }

    public void showDuration(Duration v) {
        this.showDuration = v;
    }

    public void triggerMode(Object v) {
        this.triggerMode = v;
    }

    public void enableFeedback(boolean v) {
        this.enableFeedback = v;
    }
}
