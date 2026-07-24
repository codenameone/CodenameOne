package com.codename1.flutter.cupertino;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.material.Switch;

import dart.runtime.Funcs;

/**
 * An iOS-style on/off toggle — Flutter's {@code CupertinoSwitch}. Same
 * controlled semantics as the material switch; composed onto material
 * {@link Switch} (visually approximate this pass).
 */
public class CupertinoSwitch extends StatelessWidget {

    private boolean value;
    private Funcs.VoidFunc1<Boolean> onChanged;

    public void value(boolean v) {
        this.value = v;
    }

    public void onChanged(Funcs.VoidFunc1<Boolean> v) {
        this.onChanged = v;
    }

    public void activeColor(Color v) {
    }

    public void trackColor(Color v) {
    }

    public void thumbColor(Color v) {
    }

    @Override
    public Widget build(BuildContext context) {
        Switch s = new Switch();
        s.value(value);
        s.onChanged(onChanged);
        return s;
    }
}
