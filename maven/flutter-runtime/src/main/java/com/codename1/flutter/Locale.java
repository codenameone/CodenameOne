package com.codename1.flutter;

/**
 * A Unicode locale identifier: a required language code and an optional country
 * code (Flutter's {@code Locale(languageCode, [countryCode])}).
 */
public class Locale {

    private final String languageCode;
    private final String countryCode;

    public Locale(String languageCode) {
        this(languageCode, null);
    }

    public Locale(String languageCode, String countryCode) {
        this.languageCode = languageCode;
        this.countryCode = countryCode;
    }

    public String languageCode() {
        return languageCode;
    }

    public String countryCode() {
        return countryCode;
    }

    @Override
    public String toString() {
        return countryCode == null ? languageCode : languageCode + "_" + countryCode;
    }
}
