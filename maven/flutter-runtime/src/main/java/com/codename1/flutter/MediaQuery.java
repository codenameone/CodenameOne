package com.codename1.flutter;

/**
 * Display-metric lookup, mirroring Flutter's {@code MediaQuery.of(context)}.
 * There is no inherited-widget scoping in this runtime — the metrics are
 * computed on demand from the CN1 Display, so every context sees the same
 * (current) values.
 */
public final class MediaQuery {

    private MediaQuery() {
    }

    public static MediaQueryData of(BuildContext context) {
        return MediaQueryData.fromDisplay();
    }
}
