package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * Establishes an ambient {@link SliderThemeData} for its subtree — Flutter's
 * {@code SliderTheme}. Descendant {@code Slider}/{@code RangeSlider} widgets
 * read {@code SliderTheme.of(context)} for their visual configuration. This
 * pass hosts the {@code child} and records the data; wiring it into the
 * inherited-widget lookup is deferred, so {@link #of(BuildContext)} returns a
 * fresh default.
 */
public class SliderTheme extends StatelessWidget {

    private SliderThemeData data;
    private Widget child;

    public void data(SliderThemeData v) {
        this.data = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public SliderThemeData getData() {
        return data;
    }

    public Widget getChild() {
        return child;
    }

    /** Dart's {@code SliderTheme.of(context)}: the ambient slider theme. */
    public static SliderThemeData of(BuildContext context) {
        return new SliderThemeData();
    }

    @Override
    public Widget build(BuildContext context) {
        com.codename1.flutter.FlutterErrorReport.unimplemented("SliderTheme", "slider theming is ignored");
        return child;
    }
}
