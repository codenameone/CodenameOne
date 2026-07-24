package com.codename1.flutter.cupertino;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Container;

import dart.runtime.Funcs;

/**
 * The iOS-13 sliding segmented control — Flutter's
 * {@code CupertinoSlidingSegmentedControl}. Like {@link CupertinoSegmentedControl},
 * captures the segments and selection callback but composes an empty
 * {@link Container} placeholder this pass.
 *
 * @param <T> the segment key type
 */
public class CupertinoSlidingSegmentedControl<T> extends StatelessWidget {

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

    public void thumbColor(Color v) {
    }

    public void backgroundColor(Color v) {
    }

    public void padding(Object v) {
    }

    @Override
    public Widget build(BuildContext context) {
        return new Container();
    }
}
