package com.codename1.flutter.l10n;

/**
 * Delegate provider for Widgets-layer localizations, mirroring
 * {@code GlobalWidgetsLocalizations} from {@code flutter_localizations}.
 */
public abstract class GlobalWidgetsLocalizations {

    /** Static getter -> static field. */
    public static final LocalizationsDelegate delegate = new LocalizationsDelegate();

    private GlobalWidgetsLocalizations() {
    }
}
