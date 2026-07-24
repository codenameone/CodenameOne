package com.codename1.flutter.cupertino;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * Applies a {@link CupertinoThemeData} to its subtree — Flutter's
 * {@code CupertinoTheme}. {@link #of(BuildContext)} walks up to the nearest
 * CupertinoTheme ancestor and returns its data, falling back to a default.
 * As a widget it is a pass-through: it composes its child directly (this
 * runtime does not yet thread Cupertino styling through the element tree).
 */
public class CupertinoTheme extends StatelessWidget {

    private CupertinoThemeData data;
    private Widget child;

    public void data(CupertinoThemeData v) {
        this.data = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public CupertinoThemeData getData() {
        return data;
    }

    public static CupertinoThemeData of(BuildContext context) {
        if (context != null) {
            CupertinoTheme t = context.findAncestorWidgetOfExactType(CupertinoTheme.class);
            if (t != null && t.data != null) {
                return t.data;
            }
        }
        return new CupertinoThemeData();
    }

    @Override
    public Widget build(BuildContext context) {
        return child;
    }
}
