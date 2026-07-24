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

    private final List<Object> resources;

    public LocalizationsScope(List<Object> resources) {
        this.resources = resources;
    }

    @Override
    public Object providedValueFor(Class<?> type) {
        if (resources != null && type != null) {
            for (Object r : resources) {
                if (r != null && type.isInstance(r)) {
                    return r;
                }
            }
        }
        return null;
    }
}
