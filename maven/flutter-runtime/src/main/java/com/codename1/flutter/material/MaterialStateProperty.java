package com.codename1.flutter.material;

import com.codename1.flutter.Color;

import dart.core.DartSet;
import dart.runtime.Funcs;

/**
 * A value that may depend on a component's interactive {@link MaterialState}
 * set, mirroring Flutter's {@code MaterialStateProperty}. Built either from a
 * single constant ({@link #all}) or from a resolver callback
 * ({@link #resolveWith}); {@link #resolve} evaluates it for a given state set.
 *
 * <p>The resolver's return type is generic in Flutter ({@code T}); this pass
 * models the color-valued case the gallery uses (checkbox/radio/switch
 * fill/thumb/track colors).</p>
 */
public class MaterialStateProperty {

    private final Funcs.Func1<DartSet<MaterialState>, Color> resolver;
    private final Object constant;
    private final boolean isConstant;

    private MaterialStateProperty(Funcs.Func1<DartSet<MaterialState>, Color> resolver,
                                  Object constant, boolean isConstant) {
        this.resolver = resolver;
        this.constant = constant;
        this.isConstant = isConstant;
    }

    /** A property that is {@code value} in every state. */
    public static MaterialStateProperty all(Object value) {
        return new MaterialStateProperty(null, value, true);
    }

    /** A property computed from the active state set on each read. */
    public static MaterialStateProperty resolveWith(Funcs.Func1<DartSet<MaterialState>, Color> resolver) {
        return new MaterialStateProperty(resolver, null, false);
    }

    /** Evaluates this property for {@code states}. */
    public Object resolve(DartSet<MaterialState> states) {
        if (isConstant) {
            return constant;
        }
        return resolver == null ? null : resolver.call(states);
    }
}
