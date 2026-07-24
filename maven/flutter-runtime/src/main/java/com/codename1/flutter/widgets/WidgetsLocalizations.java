package com.codename1.flutter.widgets;

import com.codename1.flutter.Locale;

import dart.core.DartIterable;
import dart.core.DartList;

/**
 * The widget-layer localizations surface — Flutter's
 * {@code WidgetsLocalizations}. new_gallery only calls the static
 * {@link #basicLocaleListResolution} helper as the fallback for its
 * {@code localeListResolutionCallback}.
 */
public abstract class WidgetsLocalizations {

    private WidgetsLocalizations() {
    }

    /**
     * Flutter's top-level {@code basicLocaleListResolution}: pick the best
     * supported locale for the user's preferred list. This minimal
     * implementation returns the first preferred locale whose language matches a
     * supported locale (preferring an exact language+country match), falling
     * back to the first supported locale.
     */
    public static Locale basicLocaleListResolution(DartList<Locale> preferredLocales,
                                                   DartIterable<Locale> supportedLocales) {
        Locale firstSupported = null;
        if (supportedLocales != null) {
            for (Locale s : supportedLocales) {
                if (firstSupported == null) {
                    firstSupported = s;
                    break;
                }
            }
        }
        if (preferredLocales == null || preferredLocales.isEmpty()) {
            return firstSupported;
        }
        for (int i = 0; i < preferredLocales.size(); i++) {
            Locale preferred = preferredLocales.get(i);
            if (preferred == null || supportedLocales == null) {
                continue;
            }
            Locale languageMatch = null;
            for (Locale supported : supportedLocales) {
                if (supported == null) {
                    continue;
                }
                if (eq(preferred.languageCode(), supported.languageCode())) {
                    if (eq(preferred.countryCode(), supported.countryCode())) {
                        return supported;
                    }
                    if (languageMatch == null) {
                        languageMatch = supported;
                    }
                }
            }
            if (languageMatch != null) {
                return languageMatch;
            }
        }
        return firstSupported != null ? firstSupported : preferredLocales.get(0);
    }

    private static boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
