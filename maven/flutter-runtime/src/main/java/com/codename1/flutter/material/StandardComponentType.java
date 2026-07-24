package com.codename1.flutter.material;

import com.codename1.flutter.Key;
import com.codename1.flutter.ValueKey;

/**
 * One of the standard, individually-keyed components a scaffold builds (the
 * back / close / drawer / more buttons) — Flutter's {@code StandardComponentType}.
 * Each value exposes a stable {@link #key()} the app can target for tests or
 * theming.
 */
public final class StandardComponentType {

    public static final StandardComponentType backButton =
            new StandardComponentType("backButton");
    public static final StandardComponentType closeButton =
            new StandardComponentType("closeButton");
    public static final StandardComponentType drawerButton =
            new StandardComponentType("drawerButton");
    public static final StandardComponentType moreButton =
            new StandardComponentType("moreButton");

    private final Key key;

    private StandardComponentType(String name) {
        this.key = new ValueKey<String>("StandardComponentType." + name);
    }

    /** The stable key identifying this component in the widget tree. */
    public Key key() {
        return key;
    }
}
