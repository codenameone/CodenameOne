package com.codename1.flutter.material;

import com.codename1.flutter.Color;
import com.codename1.flutter.Icons;
import com.codename1.flutter.widgets.Icon;

import dart.runtime.Funcs;

/**
 * A material back button: an {@link IconButton} showing the platform back
 * chevron that, when pressed, pops the current route (Flutter's
 * {@code BackButton}). {@code onPressed} overrides the default pop.
 */
public class BackButton extends IconButton {

    public BackButton() {
        icon(new Icon(Icons.arrow_back));
    }

    public BackButton(com.codename1.flutter.Key key, Color color, Funcs.VoidFunc0 onPressed) {
        this();
        if (color != null) {
            color(color);
        }
        if (onPressed != null) {
            onPressed(onPressed);
        }
    }
}
