package com.codename1.flutter.l10n;

import com.codename1.flutter.BuildContext;

/**
 * The {@code flutter_localized_countries} package's {@code LocaleNames} — maps a
 * locale code to its display name in the current locale. new_gallery's settings
 * page reaches it via {@code LocaleNames.of(context).nameOf(code)}.
 *
 * <p>The translation table is empty at this milestone, so {@link #nameOf}
 * echoes the locale code back; callers combine it with
 * {@link LocaleNamesLocalizationsDelegate#nativeLocaleNames} for the native
 * name.</p>
 */
public final class LocaleNames {

    private static final LocaleNames INSTANCE = new LocaleNames();

    private LocaleNames() {
    }

    /** Dart's {@code LocaleNames.of(context)} — the ambient locale-name table. */
    public static LocaleNames of(BuildContext context) {
        return INSTANCE;
    }

    /**
     * Dart's {@code nameOf(localeCode)} — the locale's display name in the
     * current locale, or the code itself when no translation is available.
     */
    public String nameOf(String localeCode) {
        return localeCode;
    }
}
