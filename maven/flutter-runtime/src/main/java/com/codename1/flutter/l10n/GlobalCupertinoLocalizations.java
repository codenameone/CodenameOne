package com.codename1.flutter.l10n;

/**
 * Delegate provider for Cupertino localizations, mirroring
 * {@code GlobalCupertinoLocalizations} from {@code flutter_localizations}.
 */
public abstract class GlobalCupertinoLocalizations {

    /** Static getter -> static field. */
    public static final LocalizationsDelegate delegate = new LocalizationsDelegate();

    private GlobalCupertinoLocalizations() {
    }
}
