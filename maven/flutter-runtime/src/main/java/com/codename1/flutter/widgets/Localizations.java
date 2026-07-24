package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Locale;

/**
 * Flutter's {@code Localizations} inherited-widget lookup helpers.
 *
 * <p>The app uses only the static lookups: {@code Localizations.of<T>(context,
 * type)} to reach a localizations object published up the tree, and
 * {@code Localizations.localeOf(context)} for the ambient {@link Locale}. The
 * transpiler threads the requested {@code T} as a trailing {@code Class<T>}
 * witness for {@code of}.</p>
 */
public final class Localizations {

    private Localizations() {
    }

    /**
     * {@code Localizations.of<T>(context, type)}. Returns the nearest inherited
     * localizations object of the requested type, or {@code null} when absent.
     */
    public static <T> T of(BuildContext context, Object type, Class<T> witness) {
        if (context == null || witness == null) {
            return null;
        }
        try {
            return context.read(witness);
        } catch (Throwable t) {
            return null;
        }
    }

    /** {@code Localizations.localeOf(context)} — the ambient locale. */
    public static Locale localeOf(BuildContext context) {
        if (context != null) {
            Object l = context.providerValueOfType(Locale.class);
            if (l instanceof Locale) {
                return (Locale) l;
            }
        }
        return new Locale("en", "US");
    }
}
