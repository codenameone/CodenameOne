package com.codename1.flutter.navigation;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * A route whose page is produced by a {@code WidgetBuilder}. Pushed with
 * {@link Navigator#push}; the builder runs lazily when the route's element
 * tree mounts, receiving a BuildContext inside the NEW page's tree.
 */
public class MaterialPageRoute {

    private Funcs.Func1<BuildContext, Widget> builder;

    public void builder(Funcs.Func1<BuildContext, Widget> v) {
        this.builder = v;
    }

    public Funcs.Func1<BuildContext, Widget> getBuilder() {
        return builder;
    }
}
