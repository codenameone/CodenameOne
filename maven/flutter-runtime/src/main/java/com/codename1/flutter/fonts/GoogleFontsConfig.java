package com.codename1.flutter.fonts;

/**
 * Mirror of {@code GoogleFonts.config}. Only {@code allowRuntimeFetching} is
 * modelled; runtime font fetching is never performed here so the flag is
 * inert.
 */
public class GoogleFontsConfig {

    public boolean allowRuntimeFetching = true;

    public boolean allowRuntimeFetching() {
        return allowRuntimeFetching;
    }

    public void allowRuntimeFetching(boolean v) {
        this.allowRuntimeFetching = v;
    }
}
