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
 * A chip that triggers an action when pressed — Flutter's {@code ActionChip}.
 * This milestone renders {@code avatar} + {@code label} in a {@link Row}; the
 * {@code onPressed} tap is deferred.
 */
public class ActionChip extends StatelessWidget {

    private Widget avatar;
    private Widget label;
    private Color backgroundColor;
    private TextStyle labelStyle;
    private Funcs.VoidFunc0 onPressed;

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

    public void onPressed(Funcs.VoidFunc0 v) {
        this.onPressed = v;
    }

    public void pressElevation(Object v) {
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
