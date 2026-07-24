package com.codename1.flutter.material;

/**
 * The interactive states a material component can be in, mirroring Flutter's
 * {@code MaterialState}. Passed to a {@link MaterialStateProperty} resolver as
 * a set so a theme can vary a value (a color, elevation, ...) per state.
 */
public enum MaterialState {
    hovered, focused, pressed, dragged, selected, scrolledUnder, disabled, error
}
