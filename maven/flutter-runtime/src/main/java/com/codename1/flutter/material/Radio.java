package com.codename1.flutter.material;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * A material radio button. Grouping is by VALUE EQUALITY, not by a CN1
 * ButtonGroup: this radio renders selected exactly when {@code value} equals
 * {@code groupValue} (Dart {@code ==} semantics). Selecting it fires
 * {@code onChanged(value)}; the app's rebuild with a new groupValue is what
 * moves the selection (controlled semantics). Backed by a CN1
 * {@link com.codename1.ui.RadioButton} (UIID "FlutterRadio").
 */
public class Radio<T> extends Widget {

    private T value;
    private T groupValue;
    private Funcs.VoidFunc1<T> onChanged;

    public void value(T v) {
        this.value = v;
    }

    public void groupValue(T v) {
        this.groupValue = v;
    }

    public void onChanged(Funcs.VoidFunc1<T> v) {
        this.onChanged = v;
    }

    public T getValue() {
        return value;
    }

    public T getGroupValue() {
        return groupValue;
    }

    public Funcs.VoidFunc1<T> getOnChanged() {
        return onChanged;
    }

    @Override
    public Element createElement() {
        return new RadioRenderElement(this);
    }
}
