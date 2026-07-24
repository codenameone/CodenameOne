package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.CrossAxisAlignment;
import com.codename1.flutter.MainAxisSize;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.TextStyle;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Column;
import com.codename1.flutter.widgets.Expanded;
import com.codename1.flutter.widgets.Row;

import dart.core.DartList;

/**
 * A material banner: a prominent message with optional leading icon and action
 * buttons — Flutter's {@code MaterialBanner}. This milestone renders a
 * {@link Row} of the {@code leading} widget and {@code content}, with the
 * {@code actions} laid out in a trailing {@link Row} below.
 */
public class MaterialBanner extends StatelessWidget {

    private Widget content;
    private Widget leading;
    private DartList<Widget> actions;
    private TextStyle contentTextStyle;
    private Color backgroundColor;

    public void content(Widget v) {
        this.content = v;
    }

    public void contentTextStyle(TextStyle v) {
        this.contentTextStyle = v;
    }

    public void actions(DartList<Widget> v) {
        this.actions = v;
    }

    public void elevation(double v) {
    }

    public void leading(Widget v) {
        this.leading = v;
    }

    public void backgroundColor(Color v) {
        this.backgroundColor = v;
    }

    public void surfaceTintColor(Color v) {
    }

    public void shadowColor(Color v) {
    }

    public void dividerColor(Color v) {
    }

    public void padding(Object v) {
    }

    public void leadingPadding(Object v) {
    }

    public void forceActionsBelow(boolean v) {
    }

    public void overflowAlignment(Object v) {
    }

    public void animation(Object v) {
    }

    public void onVisible(Object v) {
    }

    @Override
    public Widget build(BuildContext context) {
        DartList<Widget> top = new DartList<Widget>();
        if (leading != null) {
            top.add(leading);
        }
        if (content != null) {
            Expanded e = new Expanded();
            e.child(content);
            top.add(e);
        }
        Row topRow = new Row();
        topRow.crossAxisAlignment(CrossAxisAlignment.center);
        topRow.children(top);

        DartList<Widget> rows = new DartList<Widget>();
        rows.add(topRow);
        if (actions != null && actions.size() > 0) {
            Row actionRow = new Row();
            actionRow.mainAxisSize(MainAxisSize.min);
            actionRow.children(actions);
            rows.add(actionRow);
        }
        Column col = new Column();
        col.crossAxisAlignment(CrossAxisAlignment.stretch);
        col.mainAxisSize(MainAxisSize.min);
        col.children(rows);
        return col;
    }
}
