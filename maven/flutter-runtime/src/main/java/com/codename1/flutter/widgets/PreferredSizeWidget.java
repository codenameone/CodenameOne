package com.codename1.flutter.widgets;

import com.codename1.flutter.rendering.Size;

/**
 * A widget that reports the {@link Size} it prefers to occupy — Flutter's
 * {@code PreferredSizeWidget}. App bars and the {@link PreferredSize} adapter
 * implement it so a {@code Scaffold}/{@code AppBar} can reserve the right height
 * for a bottom widget.
 */
public interface PreferredSizeWidget {
    Size preferredSize();
}
