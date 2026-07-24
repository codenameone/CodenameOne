package com.codename1.flutter.cupertino;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.Key;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.material.ButtonBase;
import com.codename1.flutter.material.ElevatedButton;
import com.codename1.flutter.material.TextButton;

import dart.runtime.Funcs;

/**
 * An iOS-style button — Flutter's {@code CupertinoButton}. The default variant
 * is a borderless tinted-text button; {@code CupertinoButton.filled} has a
 * solid background. A null {@code onPressed} disables it. Composed onto the
 * material {@link TextButton} (default) / {@link ElevatedButton} (filled).
 */
public class CupertinoButton extends StatelessWidget {

    private Funcs.VoidFunc0 onPressed;
    private Widget child;
    private boolean filled;

    public void onPressed(Funcs.VoidFunc0 v) {
        this.onPressed = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public void padding(Object v) {
    }

    public void color(Color v) {
    }

    public void disabledColor(Color v) {
    }

    public void minSize(double v) {
    }

    public void pressedOpacity(double v) {
    }

    public void borderRadius(Object v) {
    }

    public void alignment(Object v) {
    }

    /**
     * Dart's {@code CupertinoButton.filled} named constructor in canonical
     * positional form.
     */
    public static CupertinoButton filled(Key key, Funcs.VoidFunc0 onPressed, Widget child) {
        CupertinoButton b = new CupertinoButton();
        b.key(key);
        b.onPressed = onPressed;
        b.child = child;
        b.filled = true;
        return b;
    }

    @Override
    public Widget build(BuildContext context) {
        ButtonBase b = filled ? new ElevatedButton() : new TextButton();
        b.onPressed(onPressed);
        b.child(child);
        return b;
    }
}
