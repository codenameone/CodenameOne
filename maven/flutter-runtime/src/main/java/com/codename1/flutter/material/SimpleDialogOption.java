package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.EdgeInsets;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * A single tappable option inside a {@link SimpleDialog} — Flutter's
 * {@code SimpleDialogOption}. Tapping fires {@code onPressed} (conventionally to pop the
 * dialog with a value).
 *
 * <p>The tap used to go nowhere, which made a SimpleDialog a list you could read and not
 * answer. Padding follows Material's option metrics (16lp horizontal, 8lp vertical).
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
        com.codename1.flutter.widgets.Padding pad = new com.codename1.flutter.widgets.Padding();
        pad.padding(padding != null ? padding : EdgeInsets.symmetric(8, 16));
        pad.child(child);
        if (!(onPressed instanceof dart.runtime.Funcs.VoidFunc0)) {
            return pad;
        }
        InkWell tap = new InkWell();
        tap.child(pad);
        tap.onTap((dart.runtime.Funcs.VoidFunc0) onPressed);
        return tap;
    }
}
