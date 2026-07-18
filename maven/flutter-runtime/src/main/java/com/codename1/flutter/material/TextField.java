package com.codename1.flutter.material;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * A material single-line text input, backed by a CN1
 * {@code com.codename1.ui.TextField} (UIID "FlutterTextField"). Supports a
 * {@link TextEditingController} (two-way sync), {@link InputDecoration}
 * label/hint (rendered as the CN1 hint in M3), obscured (password) input,
 * enabled/disabled state and onChanged/onSubmitted callbacks.
 */
public class TextField extends Widget {

    private TextEditingController controller;
    private InputDecoration decoration;
    private Boolean obscureText;
    private Boolean enabled;
    private Funcs.VoidFunc1<String> onChanged;
    private Funcs.VoidFunc1<String> onSubmitted;

    public void controller(TextEditingController v) {
        this.controller = v;
    }

    public void decoration(InputDecoration v) {
        this.decoration = v;
    }

    public void obscureText(boolean v) {
        this.obscureText = v;
    }

    public void enabled(boolean v) {
        this.enabled = v;
    }

    public void onChanged(Funcs.VoidFunc1<String> v) {
        this.onChanged = v;
    }

    public void onSubmitted(Funcs.VoidFunc1<String> v) {
        this.onSubmitted = v;
    }

    public TextEditingController getController() {
        return controller;
    }

    public InputDecoration getDecoration() {
        return decoration;
    }

    public boolean isObscureText() {
        return obscureText != null && obscureText;
    }

    public boolean isEnabled() {
        return enabled == null || enabled;
    }

    public Funcs.VoidFunc1<String> getOnChanged() {
        return onChanged;
    }

    public Funcs.VoidFunc1<String> getOnSubmitted() {
        return onSubmitted;
    }

    @Override
    public Element createElement() {
        return new TextFieldRenderElement(this);
    }
}
