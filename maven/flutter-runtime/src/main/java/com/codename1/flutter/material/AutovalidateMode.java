package com.codename1.flutter.material;

import dart.core.DartList;

/**
 * When a Form (or FormField) auto-validates its fields — Flutter's
 * {@code AutovalidateMode}. Modelled as a class (not a Java enum) because the
 * text-field demo reads {@link #index()} off a value and indexes {@link #values()}
 * to round-trip the choice through a RestorableInt.
 */
public final class AutovalidateMode {

    public static final AutovalidateMode disabled = new AutovalidateMode(0);
    public static final AutovalidateMode always = new AutovalidateMode(1);
    public static final AutovalidateMode onUserInteraction = new AutovalidateMode(2);

    /** The enum-like value list, indexable like Dart's {@code AutovalidateMode.values}. */
    public static final DartList<AutovalidateMode> values = buildValues();

    private static DartList<AutovalidateMode> buildValues() {
        DartList<AutovalidateMode> v = new DartList<AutovalidateMode>();
        v.add(disabled);
        v.add(always);
        v.add(onUserInteraction);
        return v;
    }

    private final int index;

    private AutovalidateMode(int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }

    public static AutovalidateMode disabled() { return disabled; }
    public static AutovalidateMode always() { return always; }
    public static AutovalidateMode onUserInteraction() { return onUserInteraction; }

    public static DartList<AutovalidateMode> values() {
        DartList<AutovalidateMode> v = new DartList<AutovalidateMode>();
        v.add(disabled);
        v.add(always);
        v.add(onUserInteraction);
        return v;
    }
}
