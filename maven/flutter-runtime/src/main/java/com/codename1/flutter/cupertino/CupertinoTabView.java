package com.codename1.flutter.cupertino;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * A single tab's navigation root — Flutter's {@code CupertinoTabView}. Runs
 * its {@code builder} to produce the tab content (the per-tab navigator stack
 * is not modeled this pass).
 */
public class CupertinoTabView extends StatelessWidget {

    private Funcs.Func1<BuildContext, Widget> builder;

    public void builder(Funcs.Func1<BuildContext, Widget> v) {
        this.builder = v;
    }

    public void restorationScopeId(String v) {
    }

    public void defaultTitle(String v) {
    }

    public void routes(Object v) {
    }

    public void onGenerateRoute(Object v) {
    }

    public void onUnknownRoute(Object v) {
    }

    public void navigatorObservers(Object v) {
    }

    @Override
    public Widget build(BuildContext context) {
        return builder == null ? null : builder.call(context);
    }
}
