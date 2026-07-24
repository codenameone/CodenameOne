package com.codename1.flutter.cupertino;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Container;

import dart.runtime.Funcs;

/**
 * A horizontal iOS segmented control — Flutter's {@code CupertinoSegmentedControl}.
 * The segment children and selection callback are captured; the segmented
 * visual (a bordered row of tappable segments) is not laid out this pass, so
 * it composes an empty {@link Container} placeholder holding the correct API
 * shape.
 *
 * @param <T> the segment key type
 */
public class CupertinoSegmentedControl<T> extends StatelessWidget {

    private Object children;
    private Funcs.VoidFunc1<Long> onValueChanged;
    private Object groupValue;

    public void children(Object v) {
        this.children = v;
    }

    public void onValueChanged(Funcs.VoidFunc1<Long> v) {
        this.onValueChanged = v;
    }

    public void groupValue(Object v) {
        this.groupValue = v;
    }

    public void unselectedColor(Color v) {
    }

    public void selectedColor(Color v) {
    }

    public void borderColor(Color v) {
    }

    public void pressedColor(Color v) {
    }

    public void padding(Object v) {
    }

    @Override
    public Widget build(BuildContext context) {
        return new Container();
    }
}
