package com.codename1.flutter.provider;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.InheritedValueProvider;
import com.codename1.flutter.Key;
import com.codename1.flutter.Widget;

/**
 * provider's {@code Provider<T>}: publishes {@code value} to its subtree by
 * runtime type. {@link #of(BuildContext, boolean, Class)} walks the element tree
 * for the nearest provider whose value is assignable to the requested type — the
 * Dart {@code Provider.of<T>(context)} witness is threaded in as {@code type} by
 * the transpiler.
 *
 * <p>The {@code create} form (a lazily-invoked factory) is accepted for API
 * shape but not exercised by the gallery, which uses the {@code .value} form.</p>
 */
public class Provider extends SingleChildWidget implements InheritedValueProvider {

    protected Object value;
    protected Object create;
    protected boolean lazy = true;

    public void value(Object v) {
        this.value = v;
    }

    public void create(Object v) {
        this.create = v;
    }

    public void lazy(boolean v) {
        this.lazy = v;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public Object providedValueFor(Class<?> type) {
        return value != null && type.isInstance(value) ? value : null;
    }

    /** The {@code Provider.value(value: ...)} named constructor. */
    public static Provider value(Key key, Object value, Widget child) {
        Provider p = new Provider();
        p.key(key);
        p.value(value);
        p.child(child);
        return p;
    }

    /** {@code Provider.of<T>(context, listen: ...)}. */
    @SuppressWarnings("unchecked")
    public static <T> T of(BuildContext context, boolean listen, Class<T> type) {
        return (T) context.providerValueOfType(type);
    }
}
