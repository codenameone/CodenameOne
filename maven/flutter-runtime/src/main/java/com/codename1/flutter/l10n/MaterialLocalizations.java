package com.codename1.flutter.l10n;

import com.codename1.flutter.BuildContext;

/**
 * Localized strings for Material widgets, mirroring Flutter's
 * {@code MaterialLocalizations}. English defaults; a single shared instance is
 * returned by {@link #of(BuildContext)}.
 */
public class MaterialLocalizations {

    private static final MaterialLocalizations INSTANCE = new MaterialLocalizations();

    /** {@code MaterialLocalizations.delegate} — a static getter, hence a field. */
    public static final LocalizationsDelegate delegate = new LocalizationsDelegate();

    public static MaterialLocalizations of(BuildContext context) {
        return INSTANCE;
    }

    public String backButtonTooltip() {
        return "Back";
    }

    public String closeButtonTooltip() {
        return "Close";
    }

    public String closeButtonLabel() {
        return "CLOSE";
    }

    public String viewLicensesButtonLabel() {
        return "VIEW LICENSES";
    }

    public String nextPageTooltip() {
        return "Next page";
    }

    public String previousPageTooltip() {
        return "Previous page";
    }

    public String openAppDrawerTooltip() {
        return "Open navigation menu";
    }
}
