package com.codename1.flutter.cupertino;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Column;

import dart.core.DartList;
import dart.runtime.Funcs;

/**
 * The iOS spinning-wheel picker — Flutter's {@code CupertinoPicker}. The 3D
 * wheel is not modeled this pass; the item widgets are laid out as a vertical
 * {@link Column} (approximate). The selection callback is captured.
 */
public class CupertinoPicker extends StatelessWidget {

    private DartList<Widget> children;
    private Funcs.VoidFunc1<Long> onSelectedItemChanged;

    public void backgroundColor(Color v) {
    }

    public void itemExtent(double v) {
    }

    public void diameterRatio(double v) {
    }

    public void magnification(double v) {
    }

    public void squeeze(double v) {
    }

    public void useMagnifier(boolean v) {
    }

    public void scrollController(Object v) {
    }

    public void onSelectedItemChanged(Funcs.VoidFunc1<Long> v) {
        this.onSelectedItemChanged = v;
    }

    public void children(DartList<Widget> v) {
        this.children = v;
    }

    @Override
    public Widget build(BuildContext context) {
        Column col = new Column();
        if (children != null) {
            col.children(children);
        }
        return col;
    }
}
