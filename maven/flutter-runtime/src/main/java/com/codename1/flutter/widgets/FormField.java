package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * A single form field wired to {@link Form} validation/save — Flutter's
 * {@code FormField<T>}. Application fields supply a {@code builder} that renders
 * the input from the current {@link FormFieldState}. This pass builds the field
 * from a fresh state; registration with the enclosing form lands with the form
 * renderer.
 *
 * @param <T> the field's value type
 */
public class FormField<T> extends StatelessWidget {

    private Funcs.Func1<FormFieldState<T>, Widget> builder;
    private FormFieldValidator<T> validator;
    private FormFieldSetter<T> onSaved;
    private T initialValue;
    private Boolean enabled;
    private Object autovalidateMode;
    private String restorationId;

    public void builder(Funcs.Func1<FormFieldState<T>, Widget> v) { this.builder = v; }
    public void validator(FormFieldValidator<T> v) { this.validator = v; }
    public void onSaved(FormFieldSetter<T> v) { this.onSaved = v; }
    public void initialValue(T v) { this.initialValue = v; }
    public void enabled(Boolean v) { this.enabled = v; }
    public void autovalidateMode(Object v) { this.autovalidateMode = v; }
    public void restorationId(String v) { this.restorationId = v; }

    @Override
    public Widget build(BuildContext context) {
        if (builder == null) {
            return null;
        }
        FormFieldState<T> state = new FormFieldState<T>();
        if (initialValue != null) {
            state.didChange(initialValue);
        }
        return builder.call(state);
    }
}
