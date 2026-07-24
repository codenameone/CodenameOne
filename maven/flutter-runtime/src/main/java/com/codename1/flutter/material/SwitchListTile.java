package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * A {@link ListTile} whose trailing (or leading) control is a {@link Switch} —
 * Flutter's {@code SwitchListTile}. Tapping the row toggles the switch, firing
 * {@code onChanged(newValue)} with CONTROLLED semantics (see {@link Switch}).
 * Composed as a ListTile hosting the switch.
 */
public class SwitchListTile extends StatelessWidget {

    private boolean value;
    private Funcs.VoidFunc1<Boolean> onChanged;
    private Widget title;
    private Widget subtitle;
    private Widget secondary;

    public void value(boolean v) {
        this.value = v;
    }

    public void onChanged(Funcs.VoidFunc1<Boolean> v) {
        this.onChanged = v;
    }

    public void title(Widget v) {
        this.title = v;
    }

    public void subtitle(Widget v) {
        this.subtitle = v;
    }

    public void secondary(Widget v) {
        this.secondary = v;
    }

    public void isThreeLine(boolean v) {
    }

    public void selected(boolean v) {
    }

    public void dense(boolean v) {
    }

    public void controlAffinity(Object v) {
    }

    public void activeColor(Object v) {
    }

    public void contentPadding(Object v) {
    }

    @Override
    public Widget build(BuildContext context) {
        Switch sw = new Switch();
        sw.value(value);
        sw.onChanged(onChanged);

        ListTile tile = new ListTile();
        if (title != null) {
            tile.title(title);
        }
        if (subtitle != null) {
            tile.subtitle(subtitle);
        }
        if (secondary != null) {
            tile.leading(secondary);
        }
        tile.trailing(sw);
        return tile;
    }
}
