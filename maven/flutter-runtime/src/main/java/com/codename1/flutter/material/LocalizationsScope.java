package com.codename1.flutter.material;

import com.codename1.flutter.InheritedValueProvider;
import com.codename1.flutter.provider.SingleChildWidget;

import java.util.List;

/**
 * Publishes the localized-resource objects loaded from a MaterialApp's
 * {@code localizationsDelegates} to its subtree, keyed by runtime type. This is
 * how {@code GalleryLocalizations.of(context)} (which resolves through
 * {@link com.codename1.flutter.widgets.Localizations#of}) finds its instance:
 * {@code providedValueFor} returns the first loaded object assignable to the
 * requested type.
 */
public class LocalizationsScope extends SingleChildWidget implements InheritedValueProvider {

    private List<Object> resources;
    private final dart.runtime.Funcs.Func0<List<Object>> supplier;

    public LocalizationsScope(List<Object> resources) {
        this.resources = resources;
        this.supplier = null;
    }

    /**
     * A scope whose resources load on first lookup. Deferring matters: loading
     * during the app's build reads the delegate list at the earliest possible
     * moment, which on a lazily-initialised backend can be before the class
     * holding it has run its static initialiser.
     */
    public LocalizationsScope(dart.runtime.Funcs.Func0<List<Object>> supplier) {
        this.supplier = supplier;
    }

    private List<Object> resources() {
        if (resources == null && supplier != null) {
            resources = supplier.call();
            if (resources != null && resources.isEmpty()) {
                try {
                    com.codename1.io.Log.p("Flutter runtime: no localizations resolved for this app; "
                            + "every Foo.of(context) below will be null");
                } catch (Throwable ignore) {
                    // headless: Log has no storage backend
                }
            }
        }
        return resources;
    }

    @Override
    public Object providedValueFor(Class<?> type) {
        List<Object> rs = resources();
        if (rs != null && type != null) {
            for (Object r : rs) {
                if (r != null && type.isInstance(r)) {
                    return r;
                }
            }
        }
        return null;
    }
}
