package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

import dart.core.DartList;
import dart.runtime.Funcs;

/**
 * A scroll view whose header slivers scroll with an inner scrollable —
 * Flutter's {@code NestedScrollView}. This milestone renders the {@code body};
 * the {@code headerSliverBuilder} slivers and the coordinated
 * outer/inner scroll linkage are deferred.
 */
public class NestedScrollView extends StatelessWidget {

    private Widget body;
    private Funcs.Func2<BuildContext, Boolean, DartList<Widget>> headerSliverBuilder;

    public void body(Widget v) {
        this.body = v;
    }

    public void headerSliverBuilder(Funcs.Func2<BuildContext, Boolean, DartList<Widget>> v) {
        this.headerSliverBuilder = v;
    }

    public void controller(Object v) {
    }

    public void scrollDirection(Object v) {
    }

    public void reverse(boolean v) {
    }

    public void physics(Object v) {
    }

    public void floatHeaderSlivers(boolean v) {
    }

    @Override
    public Widget build(BuildContext context) {
        return body != null ? body : new SizedBox();
    }
}
