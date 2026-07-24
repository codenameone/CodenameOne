package com.codename1.flutter.scopedmodel;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * scoped_model's {@code ScopedModelDescendant<T>}: rebuilds via
 * {@code builder(context, child, model)} with the nearest ancestor
 * {@link ScopedModel}'s model.
 *
 * <p>Known limitation (this pass): the class-level {@code <T>} is dropped by the
 * transpiler, so the nearest provided value of ANY type is passed. Correct when
 * a single model is in scope (the gallery's shrine study).</p>
 */
public class ScopedModelDescendant<T> extends StatelessWidget {

    private Funcs.Func3<BuildContext, Widget, T, Widget> builder;
    private boolean rebuildOnChange = true;
    private Widget child;

    public void builder(Funcs.Func3<BuildContext, Widget, T, Widget> v) {
        this.builder = v;
    }

    public void rebuildOnChange(boolean v) {
        this.rebuildOnChange = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Widget build(BuildContext context) {
        T model = (T) context.providerValueOfType(Object.class);
        return builder == null ? child : builder.call(context, child, model);
    }
}
