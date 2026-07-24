package com.codename1.flutter.l10n;

/**
 * Delegate provider for Material localizations, mirroring
 * {@code GlobalMaterialLocalizations} from {@code flutter_localizations}.
 */
public abstract class GlobalMaterialLocalizations {

    /** Static getter -> static field. */
    public static final LocalizationsDelegate delegate = new LocalizationsDelegate();

    private GlobalMaterialLocalizations() {
    }
}
