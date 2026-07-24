package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Key;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * Establishes an ambient {@link IconThemeData} for its subtree — Flutter's
 * {@code IconTheme}. Descendant {@code Icon}s read {@code IconTheme.of(context)}
 * for their default size/color. This pass hosts the {@code child} and records
 * the data; wiring the value into the inherited-widget lookup is deferred, so
 * {@link #of(BuildContext)} returns a fresh default.
 */
public class IconTheme extends StatelessWidget {

    private IconThemeData data;
    private Widget child;

    public void data(IconThemeData v) {
        this.data = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public IconThemeData getData() {
        return data;
    }

    public Widget getChild() {
        return child;
    }

    /** Dart's {@code IconTheme.of(context)}: the ambient icon theme. */
    public static IconThemeData of(BuildContext context) {
        return new IconThemeData();
    }

    /** Dart's {@code IconTheme.merge(...)} named constructor. */
    public static IconTheme merge(Key key, IconThemeData data, Widget child) {
        IconTheme t = new IconTheme();
        t.key(key);
        t.data(data);
        t.child(child);
        return t;
    }

    @Override
    public Widget build(BuildContext context) {
        return child;
    }
}
