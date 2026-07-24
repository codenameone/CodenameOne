package com.codename1.flutter.cupertino;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.material.AlertDialog;

import dart.core.DartList;

/**
 * An iOS-style alert dialog — Flutter's {@code CupertinoAlertDialog}: a title,
 * optional content and a set of {@link CupertinoDialogAction} buttons.
 * Composed onto the material {@link AlertDialog} (shown through the same
 * {@code CupertinoDialogRoute} / dialog surface; visually approximate).
 */
public class CupertinoAlertDialog extends StatelessWidget {

    private Widget title;
    private Widget content;
    private DartList<Widget> actions;

    public void title(Widget v) {
        this.title = v;
    }

    public void content(Widget v) {
        this.content = v;
    }

    public void actions(DartList<Widget> v) {
        this.actions = v;
    }

    public void scrollController(Object v) {
    }

    public void actionScrollController(Object v) {
    }

    @Override
    public Widget build(BuildContext context) {
        AlertDialog d = new AlertDialog();
        if (title != null) {
            d.title(title);
        }
        if (content != null) {
            d.content(content);
        }
        if (actions != null) {
            d.actions(actions);
        }
        return d;
    }
}
