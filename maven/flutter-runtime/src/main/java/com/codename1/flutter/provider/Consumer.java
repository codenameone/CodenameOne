package com.codename1.flutter.provider;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * provider's {@code Consumer<T>}: rebuilds via {@code builder(context, value,
 * child)} with the nearest ancestor-provided value.
 *
 * <p>The Dart type argument {@code T} is threaded in by the transpiler as a type token
 * ({@link #providedType}), so the right model is found with several in scope. Without it
 * the lookup took the NEAREST provided value of any type, which is only ever correct by
 * luck — see {@link Selector} for what that cost.</p>
 */
public class Consumer<T> extends StatelessWidget {

    private Funcs.Func3<BuildContext, T, Widget, Widget> builder;
    private Widget child;
    private Class<?> providedType = Object.class;

    /**
     * The model type this consumer reads — the Dart {@code T}, emitted by the transpiler.
     * Defaults to {@code Object}: the nearest provider of any type.
     */
    public void providedType(Class<?> v) {
        this.providedType = v == null ? Object.class : v;
    }

    public void builder(Funcs.Func3<BuildContext, T, Widget, Widget> v) {
        this.builder = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Widget build(BuildContext context) {
        T value = (T) context.providerValueOfType(providedType);
        return builder == null ? child : builder.call(context, value, child);
    }
}
