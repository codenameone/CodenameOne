package com.codename1.flutter.l10n;

/**
 * A factory for a set of localized resources, mirroring Flutter's
 * {@code LocalizationsDelegate<T>}. Opaque marker in this runtime; the type
 * parameter {@code T} (the resource type the delegate loads) exists so
 * transpiled {@code LocalizationsDelegate<GalleryLocalizations>} type arguments
 * resolve.
 *
 * @param <T> the localized-resources type this delegate produces
 */
public class LocalizationsDelegate<T> {

    /**
     * Loads the localized resources for {@code locale}. Generated delegates
     * override this with a {@code SynchronousFuture} of the resource instance;
     * the base returns null so opaque runtime delegates (material/cupertino/
     * widgets globals) are simply skipped by the MaterialApp load pass.
     */
    public dart.async.Future<T> load(com.codename1.flutter.Locale locale) {
        return null;
    }
}
