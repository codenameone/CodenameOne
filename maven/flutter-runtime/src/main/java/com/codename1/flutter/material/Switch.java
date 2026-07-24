package com.codename1.flutter.material;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * A material switch with CONTROLLED semantics (see {@link Checkbox}): a user
 * toggle fires {@code onChanged(newValue)} and the component snaps back to
 * the widget's configured value until a rebuild moves it. Backed by a CN1
 * {@link com.codename1.components.Switch} (UIID "FlutterSwitch").
 */
public class Switch extends Widget {

    private boolean value;
    private Funcs.VoidFunc1<Boolean> onChanged;

    public void value(boolean v) {
        this.value = v;
    }

    public void onChanged(Funcs.VoidFunc1<Boolean> v) {
        this.onChanged = v;
    }

    /** The color of the track/thumb when the switch is on — Flutter's {@code activeColor}. */
    public void activeColor(com.codename1.flutter.Color v) {
    }

    public boolean getValue() {
        return value;
    }

    public Funcs.VoidFunc1<Boolean> getOnChanged() {
        return onChanged;
    }

    @Override
    public Element createElement() {
        return new SwitchRenderElement(this);
    }
}
