package com.codename1.flutter.semantics;

import com.codename1.flutter.rendering.Size;

import dart.core.DartList;

/**
 * The signature of a {@code CustomPainter}'s {@code semanticsBuilder} — Flutter's
 * {@code SemanticsBuilderCallback} typedef,
 * {@code List<CustomPainterSemantics> Function(Size size)}. Given the current
 * paint {@code size}, it returns the accessibility nodes the painter exposes.
 * Declared as a single-abstract-method interface so transpiled painters can
 * supply it with a lambda.
 */
public interface SemanticsBuilderCallback {
    DartList<CustomPainterSemantics> call(Size size);
}
