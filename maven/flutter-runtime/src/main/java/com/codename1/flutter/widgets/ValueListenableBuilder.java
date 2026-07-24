package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * Rebuilds part of the tree whenever a {@code ValueListenable} changes —
 * Flutter's {@code ValueListenableBuilder<T>}. The {@code builder} is a
 * three-argument closure {@code (context, value, child)}; this pass renders the
 * optional pass-through {@code child}, with listenable subscription and rebuild
 * deferred to the state layer. The listenable and builder are held for shape.
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

    @Override
    public Widget build(BuildContext context) {
        return child;
    }
}
