package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Clip;
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
 * A compact material chip carrying a label and optional avatar/delete icon —
 * Flutter's {@code Chip}. This milestone renders the {@code avatar} and
 * {@code label} in a horizontal {@link Row}; the rounded background, delete
 * affordance and material styling are deferred.
 */
public class Chip extends StatelessWidget {

    private Widget avatar;
    private Widget label;
    private Widget deleteIcon;
    private Color backgroundColor;
    private Color deleteIconColor;
    private TextStyle labelStyle;
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

    public void deleteIcon(Widget v) {
        this.deleteIcon = v;
    }

    public void onDeleted(Funcs.VoidFunc0 v) {
        this.onDeleted = v;
    }

    public void deleteIconColor(Color v) {
        this.deleteIconColor = v;
    }

    public void deleteButtonTooltipMessage(String v) {
    }

    public void side(Object v) {
    }

    public void shape(Object v) {
    }

    public void clipBehavior(Clip v) {
    }

    public void backgroundColor(Color v) {
        this.backgroundColor = v;
    }

    public void padding(Object v) {
    }

    public void visualDensity(Object v) {
    }

    public void materialTapTargetSize(Object v) {
    }

    public void elevation(double v) {
    }

    public void shadowColor(Color v) {
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
