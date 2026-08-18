package com.codename1.flutter.cupertino;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.material.Dialogs;
import com.codename1.flutter.widgets.Column;
import com.codename1.flutter.widgets.GestureDetector;

import dart.core.DartList;
import dart.runtime.Funcs;

/**
 * A long-press context menu — Flutter's {@code CupertinoContextMenu}.
 *
 * <p>Press and hold reveals the actions. Flutter blurs the page and floats a scaled preview
 * of the child above the list; here the actions are presented as a modal sheet, so the menu
 * is reachable and its actions run even though the reveal is plainer than iOS's.</p>
 */
public class CupertinoContextMenu extends StatelessWidget {

    private DartList<Widget> actions;
    private Widget child;

    public void actions(DartList<Widget> v) {
        this.actions = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public void previewBuilder(Object v) {
    }

    public DartList<Widget> getActions() {
        return actions;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Widget build(final BuildContext context) {
        if (actions == null || actions.isEmpty()) {
            return child;
        }
        GestureDetector press = new GestureDetector();
        press.child(child);
        press.onLongPress(new Funcs.VoidFunc0() {
            @Override
            public void call() {
                Dialogs.showDialog(context, new Funcs.Func1<BuildContext, Widget>() {
                    @Override
                    public Widget call(BuildContext dialogContext) {
                        Column list = new Column();
                        list.mainAxisSize(com.codename1.flutter.MainAxisSize.min);
                        list.children(actions);
                        return list;
                    }
                });
            }
        });
        return press;
    }
}
