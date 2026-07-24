package com.codename1.flutter.cupertino;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

import dart.core.DartList;

/**
 * A long-press context menu — Flutter's {@code CupertinoContextMenu}. The
 * press-and-hold reveal is not wired this pass, so it composes its child
 * directly (the {@link CupertinoContextMenuAction}s are captured but not shown).
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

    @Override
    public Widget build(BuildContext context) {
        return child;
    }
}
