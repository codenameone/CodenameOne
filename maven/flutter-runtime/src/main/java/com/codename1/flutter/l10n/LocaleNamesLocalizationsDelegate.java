package com.codename1.flutter.l10n;

import dart.core.DartMap;

/**
 * The {@code flutter_localized_countries} package's
 * {@code LocaleNamesLocalizationsDelegate}. Only the static
 * {@link #nativeLocaleNames} lookup (locale-code -&gt; the locale's own native
 * display name) is consumed by new_gallery's settings page. The table is empty
 * at this milestone; callers fall back to the translated name when a native
 * name is absent.
 */
public class LocaleNamesLocalizationsDelegate extends LocalizationsDelegate<Object> {

    /** Locale code to the locale's native display name. */
    public static final DartMap<String, String> nativeLocaleNames = new DartMap<String, String>();

    public LocaleNamesLocalizationsDelegate() {
    }
}
