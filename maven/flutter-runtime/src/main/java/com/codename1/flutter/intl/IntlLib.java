package com.codename1.flutter.intl;

/**
 * Backing class for the {@code intl} import prefix. {@code intl.Intl} in Dart
 * resolves through the transpiler to the static field {@link #Intl} here.
 */
public abstract class IntlLib {

    /** The shared {@code Intl} instance reached via {@code intl.Intl}. */
    public static final Intl Intl = new Intl();

    private IntlLib() {
    }
}
