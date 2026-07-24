package com.codename1.flutter.material;

import com.codename1.flutter.Color;

import dart.core.DartSet;
import dart.runtime.Funcs;

/**
 * The Material-3 rename of {@link MaterialStateProperty} (identical shape).
 * Newer Flutter aliases {@code MaterialStateProperty} to
 * {@code WidgetStateProperty}; both spellings resolve.
 */
public class WidgetStateProperty {

    private final Funcs.Func1<DartSet<MaterialState>, Color> resolver;
    private final Object constant;
    private final boolean isConstant;

    private WidgetStateProperty(Funcs.Func1<DartSet<MaterialState>, Color> resolver,
                                Object constant, boolean isConstant) {
        this.resolver = resolver;
        this.constant = constant;
        this.isConstant = isConstant;
    }

    /** A property that is {@code value} in every state. */
    public static WidgetStateProperty all(Object value) {
        return new WidgetStateProperty(null, value, true);
    }

    /** A property computed from the active state set on each read. */
    public static WidgetStateProperty resolveWith(Funcs.Func1<DartSet<MaterialState>, Color> resolver) {
        return new WidgetStateProperty(resolver, null, false);
    }

    /** Evaluates this property for {@code states}. */
    public Object resolve(DartSet<MaterialState> states) {
        if (isConstant) {
            return constant;
        }
        return resolver == null ? null : resolver.call(states);
    }
}
