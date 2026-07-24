package com.codename1.flutter.cupertino;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.material.TextButton;

import dart.runtime.Funcs;

/**
 * A row of a {@link CupertinoActionSheet} — Flutter's
 * {@code CupertinoActionSheetAction}. Composed onto a material
 * {@link TextButton} (default / destructive styling approximate this pass).
 */
public class CupertinoActionSheetAction extends StatelessWidget {

    private Funcs.VoidFunc0 onPressed;
    private Widget child;

    public void onPressed(Funcs.VoidFunc0 v) {
        this.onPressed = v;
    }

    public void isDefaultAction(boolean v) {
    }

    public void isDestructiveAction(boolean v) {
    }

    public void child(Widget v) {
        this.child = v;
    }

    @Override
    public Widget build(BuildContext context) {
        TextButton b = new TextButton();
        b.onPressed(onPressed);
        b.child(child);
        return b;
    }
}
