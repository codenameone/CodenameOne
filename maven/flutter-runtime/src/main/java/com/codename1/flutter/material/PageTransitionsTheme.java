package com.codename1.flutter.material;

/**
 * Per-platform page transition builders — Flutter's {@code PageTransitionsTheme}.
 *
 * <p>Recorded, not yet driven: route transitions here come from Codename One's own
 * transition machinery. Rally configures one, so the type has to exist for its theme to
 * transpile at all.</p>
 */
public class PageTransitionsTheme {

    private Object builders;

    public void builders(Object v) {
        this.builders = v;
    }

    public Object getBuilders() {
        return builders;
    }
}
