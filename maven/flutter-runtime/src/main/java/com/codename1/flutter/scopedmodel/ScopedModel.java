package com.codename1.flutter.scopedmodel;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.InheritedValueProvider;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * scoped_model's {@code ScopedModel<T extends Model>}: publishes {@code model}
 * to its subtree by runtime type and renders its {@code child}.
 * {@link #of(BuildContext, boolean, Class)} walks the tree for the nearest
 * matching model — the Dart {@code ScopedModel.of<T>(context)} witness is
 * threaded in as {@code type} by the transpiler.
 */
public class ScopedModel extends StatelessWidget implements InheritedValueProvider {

    private Object model;
    private Widget child;

    public void model(Object v) {
        this.model = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Object getModel() {
        return model;
    }

    @Override
    public Object providedValueFor(Class<?> type) {
        return model != null && type.isInstance(model) ? model : null;
    }

    @Override
    public Widget build(BuildContext context) {
        return child;
    }

    /** {@code ScopedModel.of<T>(context, rebuildOnChange: ...)}. */
    @SuppressWarnings("unchecked")
    public static <T> T of(BuildContext context, boolean rebuildOnChange, Class<T> type) {
        return (T) context.providerValueOfType(type);
    }
}
