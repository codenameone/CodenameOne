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
public class Radio extends Widget {

    private Object value;
    private Object groupValue;
    private Funcs.VoidFunc1<Object> onChanged;

    public void value(Object v) {
        this.value = v;
    }

    public void groupValue(Object v) {
        this.groupValue = v;
    }

    public void onChanged(Funcs.VoidFunc1<Object> v) {
        this.onChanged = v;
    }

    public Object getValue() {
        return value;
    }

    public Object getGroupValue() {
        return groupValue;
    }

    public Funcs.VoidFunc1<Object> getOnChanged() {
        return onChanged;
    }

    @Override
    public Element createElement() {
        return new RadioRenderElement(this);
    }
}
