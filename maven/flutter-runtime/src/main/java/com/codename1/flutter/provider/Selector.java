package com.codename1.flutter.provider;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * provider's {@code Selector<A, S>}: rebuilds only when a selected slice
 * {@code S} of a provided value {@code A} changes. {@code selector(context, a)}
 * extracts the slice and {@code builder(context, s, child)} renders it.
 *
 * <p>The Dart type argument {@code A} is threaded in by the transpiler as a type token
 * ({@link #providedType}), so the right model is found even when several are in scope.
 * Without it the lookup took the NEAREST provided value of any type: Reply has both its
 * localizations and its EmailStore above the Selector, got the localizations, and failed —
 * with a ClassCastException on the desktop and, because the cast is unchecked there, a
 * wrong object that flowed on until an unrelated switch matched nothing on iOS.</p>
 *
 * @param <A> the provided value type
 * @param <S> the selected slice type
 */
public class Selector<A, S> extends StatelessWidget {

    private Funcs.Func2<BuildContext, A, S> selector;
    private Funcs.Func3<BuildContext, S, Widget, Widget> builder;
    private Object shouldRebuild;
    private Widget child;
    private Class<?> providedType = Object.class;

    /**
     * The model type this selector reads — the Dart {@code A}, emitted by the transpiler.
     * Defaults to {@code Object}, which resolves to the nearest provider of any type and is
     * only correct when a single value is in scope.
     */
    public void providedType(Class<?> v) {
        this.providedType = v == null ? Object.class : v;
    }

    public void selector(Funcs.Func2<BuildContext, A, S> v) {
        this.selector = v;
    }

    public void builder(Funcs.Func3<BuildContext, S, Widget, Widget> v) {
        this.builder = v;
    }

    public void shouldRebuild(Object v) {
        this.shouldRebuild = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Widget build(BuildContext context) {
        A value = (A) context.providerValueOfType(providedType);
        S selected = selector == null ? (S) value : selector.call(context, value);
        return builder == null ? child : builder.call(context, selected, child);
    }
}
