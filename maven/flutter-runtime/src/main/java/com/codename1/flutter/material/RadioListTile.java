package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * A {@link ListTile} whose trailing (or leading) control is a {@link Radio} —
 * Flutter's {@code RadioListTile<T>}. Tapping anywhere on the row selects the
 * radio, firing {@code onChanged(value)}; grouping is by value equality against
 * {@code groupValue} (see {@link Radio}). Composed as a ListTile hosting the
 * radio.
 */
public class RadioListTile<T> extends StatelessWidget {

    private Object value;
    private Object groupValue;
    private Funcs.VoidFunc1<T> onChanged;
    private Widget title;
    private Widget subtitle;
    private Widget secondary;

    public void value(Object v) {
        this.value = v;
    }

    public void groupValue(Object v) {
        this.groupValue = v;
    }

    public void onChanged(Funcs.VoidFunc1<T> v) {
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
        Radio radio = new Radio();
        radio.value(value);
        radio.groupValue(groupValue);
        radio.onChanged(onChanged);

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
        tile.trailing(radio);
        return tile;
    }
}
