package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * Access point for showing SnackBars — Flutter's {@code ScaffoldMessenger}.
 * As a widget it simply renders its {@code child} (it scopes messenger state
 * to its subtree); M3 keeps one messenger state per app process (a single
 * static state is equivalent for one running app), exposed via the static
 * {@link #of(BuildContext)}.
 */
public class ScaffoldMessenger extends StatelessWidget {

    private static final ScaffoldMessengerState state = new ScaffoldMessengerState();

    private Widget child;

    public ScaffoldMessenger() {
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Widget build(BuildContext context) {
        return child;
    }

    public static ScaffoldMessengerState of(BuildContext context) {
        return state;
    }
}
