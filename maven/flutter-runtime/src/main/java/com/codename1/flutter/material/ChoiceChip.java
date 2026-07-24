package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.CrossAxisAlignment;
import com.codename1.flutter.MainAxisSize;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.TextStyle;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Row;
import com.codename1.flutter.widgets.SizedBox;

import dart.core.DartList;
import dart.runtime.Funcs;

/**
 * A chip that lets the user choose one option from a set — Flutter's
 * {@code ChoiceChip}. This milestone renders {@code avatar} + {@code label} in a
 * {@link Row}; the selected-state styling and {@code onSelected} tap are
 * deferred.
 */
public class ChoiceChip extends StatelessWidget {

    private Widget avatar;
    private Widget label;
    private boolean selected;
    private Color backgroundColor;
    private TextStyle labelStyle;
    private Funcs.VoidFunc1<Boolean> onSelected;

    public void avatar(Widget v) {
        this.avatar = v;
    }

    public void label(Widget v) {
        this.label = v;
    }

    public void labelStyle(TextStyle v) {
        this.labelStyle = v;
    }

    public void labelPadding(Object v) {
    }

    public void selected(boolean v) {
        this.selected = v;
    }

    public void onSelected(Funcs.VoidFunc1<Boolean> v) {
        this.onSelected = v;
    }

    public void pressElevation(Object v) {
    }

    public void disabledColor(Color v) {
    }

    public void selectedColor(Color v) {
    }

    public void tooltip(Object v) {
    }

    public void side(Object v) {
    }

    public void shape(Object v) {
    }

    public void backgroundColor(Color v) {
        this.backgroundColor = v;
    }

    public void padding(Object v) {
    }

    public void elevation(double v) {
    }

    @Override
    public Widget build(BuildContext context) {
        DartList<Widget> kids = new DartList<Widget>();
        if (avatar != null) {
            kids.add(avatar);
        }
        if (label != null) {
            kids.add(label);
        }
        if (kids.size() == 0) {
            return new SizedBox();
        }
        Row row = new Row();
        row.mainAxisSize(MainAxisSize.min);
        row.crossAxisAlignment(CrossAxisAlignment.center);
        row.children(kids);
        return row;
    }
}
