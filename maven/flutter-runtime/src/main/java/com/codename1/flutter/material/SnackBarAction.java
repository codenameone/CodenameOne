package com.codename1.flutter.material;

import com.codename1.flutter.Color;

import dart.runtime.Funcs;

/**
 * An action button shown alongside a {@link SnackBar}'s content — Flutter's
 * {@code SnackBarAction}. Configuration only (label + press callback); the
 * {@link SnackBar} consumes it when shown.
 */
public class SnackBarAction {

    private String label;
    private Funcs.VoidFunc0 onPressed;
    private Color textColor;
    private Color disabledTextColor;

    public void label(String v) {
        this.label = v;
    }

    public void onPressed(Funcs.VoidFunc0 v) {
        this.onPressed = v;
    }

    public void textColor(Color v) {
        this.textColor = v;
    }

    public void disabledTextColor(Color v) {
        this.disabledTextColor = v;
    }

    public String getLabel() {
        return label;
    }

    public String label() {
        return label;
    }

    public Funcs.VoidFunc0 getOnPressed() {
        return onPressed;
    }
}
