package com.codename1.flutter.material;

/**
 * A route transition for one platform — Flutter's {@code PageTransitionsBuilder}.
 *
 * <p>The base type only; concrete builders are supplied by the app (Rally hands
 * {@link PageTransitionsTheme} one per platform). Route transitions are driven by Codename
 * One's own machinery, so these are recorded rather than run — but the type has to exist
 * for a theme that names it in a map to compile.</p>
 */
public class PageTransitionsBuilder {
}
