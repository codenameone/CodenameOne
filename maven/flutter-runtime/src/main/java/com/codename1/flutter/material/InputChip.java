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
 * A chip representing a complex piece of information (a contact, tag, ...) that
 * can be selected, pressed or deleted — Flutter's {@code InputChip}. This
 * milestone renders {@code avatar} + {@code label} in a {@link Row}; the
 * selection/press/delete interactions are deferred.
 */
public class InputChip extends StatelessWidget {

    private Widget avatar;
    private Widget label;
    private Widget deleteIcon;
    private boolean selected;
    private Color backgroundColor;
    private Color deleteIconColor;
    private TextStyle labelStyle;
    private Funcs.VoidFunc1<Boolean> onSelected;
    private Funcs.VoidFunc0 onPressed;
    private Funcs.VoidFunc0 onDeleted;

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

    public void isEnabled(boolean v) {
    }

    public void onSelected(Funcs.VoidFunc1<Boolean> v) {
        this.onSelected = v;
    }

    public void deleteIcon(Widget v) {
        this.deleteIcon = v;
    }

    public void onDeleted(Funcs.VoidFunc0 v) {
        this.onDeleted = v;
    }

    public void deleteIconColor(Color v) {
        this.deleteIconColor = v;
    }

    public void onPressed(Funcs.VoidFunc0 v) {
        this.onPressed = v;
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
        if (deleteIcon != null) {
            kids.add(deleteIcon);
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
