package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;

/**
 * Access point for showing SnackBars. M3 keeps one messenger state per app
 * process (Flutter scopes it to the MaterialApp; a single static state is
 * equivalent for one running app).
 */
public final class ScaffoldMessenger {

    private static final ScaffoldMessengerState state = new ScaffoldMessengerState();

    private ScaffoldMessenger() {
    }

    public static ScaffoldMessengerState of(BuildContext context) {
        return state;
    }
}
