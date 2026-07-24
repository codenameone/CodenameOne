package com.codename1.flutter.provider;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * provider's {@code Consumer<T>}: rebuilds via {@code builder(context, value,
 * child)} with the nearest ancestor-provided value.
 *
 * <p>Known limitation (this pass): the Dart {@code <T>} on {@code Consumer<T>}
 * is a class-level type argument the transpiler currently drops, so the builder
 * receives the nearest provided value of ANY type and the builder closure's
 * concrete model parameter is not re-typed here. Correct when a single value is
 * in scope (the gallery's reply study); general multi-provider disambiguation
 * needs constructor-type-argument threading.</p>
 */
public class Consumer<T> extends StatelessWidget {

    private Funcs.Func3<BuildContext, T, Widget, Widget> builder;
    private Widget child;

    public void builder(Funcs.Func3<BuildContext, T, Widget, Widget> v) {
        this.builder = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Widget build(BuildContext context) {
        T value = (T) context.providerValueOfType(Object.class);
        return builder == null ? child : builder.call(context, value, child);
    }
}
