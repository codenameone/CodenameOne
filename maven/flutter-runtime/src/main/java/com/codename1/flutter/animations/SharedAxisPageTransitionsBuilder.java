package com.codename1.flutter.animations;

import com.codename1.flutter.Color;

/**
 * The {@code PageTransitionsBuilder} for the shared-axis (X/Y/Z) motion pattern
 * — the {@code animations} package's {@code SharedAxisPageTransitionsBuilder}.
 * Installed into a {@code PageTransitionsTheme} so route pushes animate along
 * the configured {@link SharedAxisTransitionType}. The optional {@code fillColor}
 * paints behind the transitioning pages. Configuration only in this pass; the
 * builder emits a {@link SharedAxisTransition} when the navigation renderer
 * lands.
 */
public class SharedAxisPageTransitionsBuilder
        extends com.codename1.flutter.material.PageTransitionsBuilder {

    private SharedAxisTransitionType transitionType;
    private Color fillColor;

    public void transitionType(SharedAxisTransitionType v) {
        this.transitionType = v;
    }

    public void fillColor(Color v) {
        this.fillColor = v;
    }

    public SharedAxisTransitionType getTransitionType() {
        return transitionType;
    }

    public Color getFillColor() {
        return fillColor;
    }
}
