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
 * <p>Known limitation (shared with {@link Consumer}): the Dart {@code <A, S>}
 * type arguments the transpiler currently drops, so the {@code selector} closure
 * receives the nearest provided value as {@code Object} and its concrete model
 * type is not re-threaded here. Correct when a single value is in scope; general
 * disambiguation needs constructor-type-argument threading.</p>
 *
 * @param <A> the provided value type
 * @param <S> the selected slice type
 */
public class Selector<A, S> extends StatelessWidget {

    private Funcs.Func2<BuildContext, A, S> selector;
    private Funcs.Func3<BuildContext, S, Widget, Widget> builder;
    private Object shouldRebuild;
    private Widget child;

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
        A value = (A) context.providerValueOfType(Object.class);
        S selected = selector == null ? (S) value : selector.call(context, value);
        return builder == null ? child : builder.call(context, selected, child);
    }
}
