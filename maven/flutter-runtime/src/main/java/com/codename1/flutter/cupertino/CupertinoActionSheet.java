package com.codename1.flutter.cupertino;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Column;

import dart.core.DartList;

/**
 * An iOS bottom action sheet — Flutter's {@code CupertinoActionSheet}: an
 * optional title / message, a list of {@link CupertinoActionSheetAction}s and
 * an optional cancel button. Laid out as a vertical {@link Column} of those
 * parts this pass (the sheet chrome / slide-up is approximate).
 */
public class CupertinoActionSheet extends StatelessWidget {

    private Widget title;
    private Widget message;
    private DartList<Widget> actions;
    private Widget cancelButton;

    public void title(Widget v) {
        this.title = v;
    }

    public void message(Widget v) {
        this.message = v;
    }

    public void actions(DartList<Widget> v) {
        this.actions = v;
    }

    public void messageScrollController(Object v) {
    }

    public void actionScrollController(Object v) {
    }

    public void cancelButton(Widget v) {
        this.cancelButton = v;
    }

    @Override
    public Widget build(BuildContext context) {
        DartList<Widget> kids = new DartList<Widget>();
        if (title != null) {
            kids.add(title);
        }
        if (message != null) {
            kids.add(message);
        }
        if (actions != null) {
            kids.addAll(actions);
        }
        if (cancelButton != null) {
            kids.add(cancelButton);
        }
        Column col = new Column();
        col.children(kids);
        return col;
    }
}
