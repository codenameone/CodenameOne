package com.codename1.flutter.material;

import com.codename1.flutter.Brightness;
import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Element;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.ThemeMode;
import com.codename1.flutter.Widget;
import com.codename1.ui.Display;

/**
 * The material application shell. Renders its {@code home} as its only
 * child, provides the theme that {@link Theme#of} resolves by walking up the
 * element tree, and (M4) selects the EFFECTIVE theme from
 * {@code theme}/{@code darkTheme} per {@code themeMode} — installing it into
 * the CN1 UIManager via {@link ThemeDataAdapter} when the app mounts and
 * whenever the effective theme changes across rebuilds (see
 * {@link MaterialAppElement}).
 */
public class MaterialApp extends StatelessWidget {

    private String title;
    private ThemeData theme;
    private ThemeData darkTheme;
    private ThemeMode themeMode;
    private Widget home;

    public void title(String v) {
        this.title = v;
    }

    public void theme(ThemeData v) {
        this.theme = v;
    }

    public void darkTheme(ThemeData v) {
        this.darkTheme = v;
    }

    public void themeMode(ThemeMode v) {
        this.themeMode = v;
    }

    public void home(Widget v) {
        this.home = v;
    }

    public String getTitle() {
        return title;
    }

    public ThemeData getTheme() {
        return theme;
    }

    public ThemeData getDarkTheme() {
        return darkTheme;
    }

    public ThemeMode getThemeMode() {
        return themeMode;
    }

    public Widget getHome() {
        return home;
    }

    /**
     * The theme this app is actually showing right now: {@code darkTheme}
     * when dark is in effect (per {@link #wantsDark}) and one was provided,
     * else {@code theme} (matching Flutter's fallback to {@code theme} when
     * {@code darkTheme} is absent). With neither set, a default ThemeData is
     * returned whose brightness follows the dark request.
     */
    public ThemeData effectiveTheme() {
        boolean dark = wantsDark(themeMode, platformDark());
        ThemeData t = (dark && darkTheme != null) ? darkTheme : theme;
        if (t == null) {
            t = new ThemeData();
            if (dark) {
                t.brightness(Brightness.dark);
            }
        }
        return t;
    }

    /**
     * The themeMode decision table (pure — headless-testable): {@code dark}
     * and {@code light} are absolute; {@code system} (or null, its default)
     * follows the platform flag, treating null/unknown as light.
     */
    public static boolean wantsDark(ThemeMode mode, Boolean platformDark) {
        if (mode == ThemeMode.dark) {
            return true;
        }
        if (mode == ThemeMode.light) {
            return false;
        }
        return Boolean.TRUE.equals(platformDark);
    }

    /**
     * The platform dark-mode flag from the CN1 Display, or null when no
     * Display exists (headless) or the port can't report it.
     */
    public static Boolean platformDark() {
        try {
            if (Display.isInitialized()) {
                return Display.getInstance().isDarkMode();
            }
        } catch (Throwable ignore) {
            // headless or unsupported port
        }
        return null;
    }

    @Override
    public Widget build(BuildContext context) {
        return home;
    }

    @Override
    public Element createElement() {
        return new MaterialAppElement(this);
    }
}
