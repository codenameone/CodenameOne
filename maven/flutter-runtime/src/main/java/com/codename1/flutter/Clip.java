package com.codename1.flutter;

/**
 * Clipping modes — Flutter's {@code Clip}. The Codename One runtime treats
 * clipping as best-effort (most wrappers pass their child through
 * unclipped for this milestone), so the value is currently informational.
 */
public enum Clip {
    none, hardEdge, antiAlias, antiAliasWithSaveLayer
}
