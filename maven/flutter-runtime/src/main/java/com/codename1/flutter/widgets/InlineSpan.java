package com.codename1.flutter.widgets;

/**
 * The base of the styled-text tree — Flutter's {@code InlineSpan}, the common
 * supertype of {@link TextSpan} (and, later, {@code WidgetSpan}). Held as
 * configuration consumed by {@link RichText}; not a Widget.
 */
public abstract class InlineSpan {

    /**
     * {@code InlineSpan.toPlainText}: the concatenated raw text of this span and
     * all descendants, in depth-first order.
     */
    public abstract String toPlainText();
}
