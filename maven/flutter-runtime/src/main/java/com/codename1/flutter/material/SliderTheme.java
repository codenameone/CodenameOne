package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.InheritedWidget;

/**
 * Establishes an ambient {@link SliderThemeData} for its subtree — Flutter's
 * {@code SliderTheme}. Descendant {@code Slider}/{@code RangeSlider} widgets read
 * {@code SliderTheme.of(context)} for their visual configuration.
 *
 * <p>It is a real {@link InheritedWidget}: {@code of} used to return a fresh default no
 * matter what the tree said, so any slider styling in the app was silently discarded.</p>
 */
public class SliderTheme extends InheritedWidget {

    private SliderThemeData data;

    public void data(SliderThemeData v) {
        this.data = v;
    }

    public SliderThemeData getData() {
        return data;
    }

    /** Dart's {@code SliderTheme.of(context)}: the nearest enclosing slider theme. */
    public static SliderThemeData of(BuildContext context) {
        SliderTheme t = context == null ? null
                : context.dependOnInheritedWidgetOfExactType(SliderTheme.class);
        if (t != null && t.data != null) {
            return t.data;
        }
        return new SliderThemeData();
    }

    @Override
    public boolean updateShouldNotify(InheritedWidget oldWidget) {
        return !(oldWidget instanceof SliderTheme) || ((SliderTheme) oldWidget).data != data;
    }
}
