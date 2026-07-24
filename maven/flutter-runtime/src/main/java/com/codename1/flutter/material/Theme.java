package com.codename1.flutter.material;

import com.codename1.flutter.Brightness;
import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * Applies a {@link ThemeData} to a subtree — Flutter's {@code Theme} widget —
 * and provides the static {@link #of(BuildContext)} lookup. As a widget it
 * simply renders its {@code child}; the {@code data} it carries is what a
 * descendant's {@code Theme.of(context)} resolves. When no {@code Theme}
 * ancestor is present, {@link #of} falls back to the nearest
 * {@link MaterialApp}'s effective theme (and a default {@link ThemeData} when
 * there is none).
 */
public class Theme extends StatelessWidget {

    private ThemeData data;
    private Widget child;

    public Theme() {
    }

    public void data(ThemeData v) {
        this.data = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public ThemeData getData() {
        return data;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Widget build(BuildContext context) {
        return child;
    }

    public static ThemeData of(BuildContext context) {
        Theme t = context == null
                ? null
                : context.findAncestorWidgetOfExactType(Theme.class);
        if (t != null && t.data != null) {
            return t.data;
        }
        MaterialApp app = context == null
                ? null
                : context.findAncestorWidgetOfExactType(MaterialApp.class);
        if (app != null) {
            return app.effectiveTheme();
        }
        return new ThemeData();
    }

    /**
     * The brightness of the effective theme ({@code Theme.brightnessOf}).
     */
    public static Brightness brightnessOf(BuildContext context) {
        return of(context).brightness();
    }
}
