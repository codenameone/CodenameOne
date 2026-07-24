package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Clip;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.SizedBox;

import dart.runtime.Funcs;

/**
 * A material bottom sheet surface — Flutter's {@code BottomSheet}. Renders the
 * widget produced by {@code builder(context)}; the drag-to-dismiss gesture that
 * fires {@code onClosing} is deferred.
 */
public class BottomSheet extends StatelessWidget {

    private Funcs.Func1<BuildContext, Widget> builder;
    private Funcs.VoidFunc0 onClosing;
    private boolean enableDrag = true;
    private Color backgroundColor;

    public void animationController(Object v) {
    }

    public void enableDrag(boolean v) {
        this.enableDrag = v;
    }

    public void onClosing(Funcs.VoidFunc0 v) {
        this.onClosing = v;
    }

    public void builder(Funcs.Func1<BuildContext, Widget> v) {
        this.builder = v;
    }

    public void backgroundColor(Color v) {
        this.backgroundColor = v;
    }

    public void elevation(double v) {
    }

    public void shape(Object v) {
    }

    public void clipBehavior(Clip v) {
    }

    public void constraints(Object v) {
    }

    @Override
    public Widget build(BuildContext context) {
        if (builder != null) {
            Widget w = builder.call(context);
            if (w != null) {
                return w;
            }
        }
        return new SizedBox();
    }
}
