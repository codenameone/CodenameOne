package com.codename1.flutter.cupertino;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * A tabbed iOS page — Flutter's {@code CupertinoTabScaffold}: a bottom
 * {@link CupertinoTabBar} above a body produced per-tab by {@code tabBuilder}.
 * Tab switching is not wired this pass, so it builds and shows the first tab's
 * content (index 0).
 */
public class CupertinoTabScaffold extends StatelessWidget {

    private CupertinoTabBar tabBar;
    private Funcs.Func2<BuildContext, Long, Widget> tabBuilder;

    public void tabBar(CupertinoTabBar v) {
        this.tabBar = v;
    }

    public void tabBuilder(Funcs.Func2<BuildContext, Long, Widget> v) {
        this.tabBuilder = v;
    }

    public void controller(Object v) {
    }

    public void backgroundColor(Color v) {
    }

    public void resizeToAvoidBottomInset(boolean v) {
    }

    public void restorationId(String v) {
    }

    @Override
    public Widget build(BuildContext context) {
        return tabBuilder == null ? null : tabBuilder.call(context, 0L);
    }
}
