package com.codename1.flutter.material;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * A material checkbox with CONTROLLED semantics: the widget's {@code value}
 * is authoritative. A user toggle fires {@code onChanged(newValue)} and the
 * component is snapped back to the configured value — the app's
 * setState/rebuild is what actually moves the checkbox. Backed by a CN1
 * {@link com.codename1.ui.CheckBox} (UIID "FlutterCheckbox").
 */
public class Checkbox extends Widget {

    private boolean value;
    private Funcs.VoidFunc1<Boolean> onChanged;

    public void value(boolean v) {
        this.value = v;
    }

    public void onChanged(Funcs.VoidFunc1<Boolean> v) {
        this.onChanged = v;
    }

    public boolean getValue() {
        return value;
    }

    public Funcs.VoidFunc1<Boolean> getOnChanged() {
        return onChanged;
    }

    @Override
    public Element createElement() {
        return new CheckboxRenderElement(this);
    }
}
