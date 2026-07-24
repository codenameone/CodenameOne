package com.codename1.flutter.widgets;

/**
 * The state of a {@link Form}, driving validation/save/reset across its fields —
 * Flutter's {@code FormState}. Reached through a
 * {@code GlobalKey<FormState>().currentState}. This pass exposes the control
 * surface; the field registry that makes validate/save fan out lands with the
 * form renderer.
 */
public class FormState {

    /** Validates every field; returns {@code true} when all are valid. */
    public boolean validate() {
        return true;
    }

    /** Saves every field (invokes their {@code onSaved}). */
    public void save() {
    }

    /** Resets every field to its initial value. */
    public void reset() {
    }
}
