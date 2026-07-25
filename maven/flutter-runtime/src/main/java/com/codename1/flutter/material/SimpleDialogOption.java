package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.EdgeInsets;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * A single tappable option inside a {@link SimpleDialog} — Flutter's
 * {@code SimpleDialogOption}. Tapping fires {@code onPressed} (conventionally to
 * pop the dialog with a value). This pass hosts the {@code child}; the tap
 * gesture is captured for a later interactive pass.
 */
public class SimpleDialogOption extends StatelessWidget {

    private Object onPressed;
    private EdgeInsets padding;
    private Widget child;

    public void onPressed(dart.runtime.Funcs.VoidFunc0 v) {
        this.onPressed = v;
    }

    public void padding(EdgeInsets v) {
        this.padding = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Object getOnPressed() {
        return onPressed;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Widget build(BuildContext context) {
        com.codename1.flutter.FlutterErrorReport.unimplemented("SimpleDialogOption", "renders the child without option padding or tap handling");
        return child;
    }
}
