package com.codename1.flutter;

/**
 * The Porter-Duff / separable blend modes passed to {@code Canvas.drawVertices}
 * and related painting APIs — dart:ui's {@code BlendMode}. new_gallery's
 * transformations demo uses {@link #color}; the full standard set is declared
 * for fidelity.
 */
public enum BlendMode {
    clear, src, dst, srcOver, dstOver, srcIn, dstIn, srcOut, dstOut, srcATop,
    dstATop, xor, plus, modulate, screen, overlay, darken, lighten, colorDodge,
    colorBurn, hardLight, softLight, difference, exclusion, multiply, hue,
    saturation, color, luminosity
}
