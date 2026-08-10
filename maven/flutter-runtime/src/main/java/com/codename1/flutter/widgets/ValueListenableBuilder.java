package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.foundation.ValueListenable;

/**
 * Rebuilds part of the tree whenever a {@code ValueListenable} changes —
 * Flutter's {@code ValueListenableBuilder<T>}. The {@code builder} is a
 * three-argument closure {@code (context, value, child)} that produces the
 * subtree, invoked with the listenable's current value and the optional
 * pass-through {@code child}.
 *
 * <p>{@link ValueListenableBuilderElement} does the listening; this widget is only the
 * configuration.</p>
 *
 * @param <T> the value type the listenable exposes
 */
public class ValueListenableBuilder<T> extends StatelessWidget {

    private Object valueListenable;
    private Object builder;
    private Widget child;

    public void valueListenable(Object v) {
        this.valueListenable = v;
    }

    public void builder(dart.runtime.Funcs.Func3<BuildContext, T, Widget, Widget> v) {
        this.builder = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Object getBuilder() {
        return builder;
    }

    public Object getValueListenable() {
        return valueListenable;
    }

    @Override
    public com.codename1.flutter.Element createElement() {
        return new ValueListenableBuilderElement(this);
    }

    @Override
    public Widget build(BuildContext context) {
        return buildWith(context);
    }

    /** Invokes the builder with the listenable's current value. */
    @SuppressWarnings("unchecked")
    Widget buildWith(BuildContext context) {
        if (builder instanceof dart.runtime.Funcs.Func3) {
            T value = valueListenable instanceof ValueListenable
                    ? ((ValueListenable<T>) valueListenable).value() : null;
            return ((dart.runtime.Funcs.Func3<BuildContext, T, Widget, Widget>) builder)
                    .call(context, value, child);
        }
        return child;
    }
}
