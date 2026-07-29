package com.codename1.flutter;

/**
 * Clipping modes — Flutter's {@code Clip}. Codename One clips on a rectangle boundary, so
 * {@link com.codename1.flutter.widgets.ClipRect} honours these; the rounded and custom
 * clippers still pass their child through and report the gap.
 */
public enum Clip {
    none, hardEdge, antiAlias, antiAliasWithSaveLayer
}
