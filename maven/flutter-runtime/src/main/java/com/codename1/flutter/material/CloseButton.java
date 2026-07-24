package com.codename1.flutter.material;

import com.codename1.flutter.Color;
import com.codename1.flutter.Icons;
import com.codename1.flutter.widgets.Icon;

import dart.runtime.Funcs;

/**
 * A material close button: an {@link IconButton} showing an "X" that, when
 * pressed, pops the current route (Flutter's {@code CloseButton}).
 */
public class CloseButton extends IconButton {

    public CloseButton() {
        icon(new Icon(Icons.close));
    }

    public CloseButton(com.codename1.flutter.Key key, Color color, Funcs.VoidFunc0 onPressed) {
        this();
        if (color != null) {
            color(color);
        }
        if (onPressed != null) {
            onPressed(onPressed);
        }
    }
}
