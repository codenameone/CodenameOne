package com.codename1.flutter;

import com.codename1.flutter.material.TextEditingController;

/**
 * A restorable {@link TextEditingController} ({@code RestorableTextEditingController}
 * in Flutter). It owns a live controller, exposed via {@link #value()}; the text
 * itself is not persisted across launches.
 */
public class RestorableTextEditingController extends RestorableProperty<TextEditingController> {

    private final TextEditingController controller = new TextEditingController();

    public RestorableTextEditingController() {
    }

    /** Named constructor parameter {@code text:} — the initial text. */
    public void text(String v) {
        controller.text(v);
    }

    public TextEditingController value() {
        return controller;
    }

    @Override
    public TextEditingController createDefaultValue() {
        return controller;
    }

    @Override
    public void initWithValue(TextEditingController value) {
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}
