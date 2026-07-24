package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Clip;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

import dart.core.DartList;

/**
 * A scroll view built from a list of slivers — Flutter's
 * {@code CustomScrollView}. Modeled as a {@link ListView} whose children are
 * the {@code slivers} (each sliver composes into a box widget); the fine-grained
 * sliver scroll protocol is deferred.
 */
public class CustomScrollView extends StatelessWidget {

    private DartList<Widget> slivers;
    private boolean shrinkWrap;

    public void slivers(DartList<Widget> v) {
        this.slivers = v;
    }

    public void controller(Object v) {
    }

    public void scrollDirection(Object v) {
    }

    public void reverse(boolean v) {
    }

    public void shrinkWrap(boolean v) {
        this.shrinkWrap = v;
    }

    public void physics(Object v) {
    }

    public void cacheExtent(double v) {
    }

    public void primary(Object v) {
    }

    public void clipBehavior(Clip v) {
    }

    @Override
    public Widget build(BuildContext context) {
        ListView list = new ListView();
        list.children(slivers != null ? slivers : new DartList<Widget>());
        if (shrinkWrap) {
            list.shrinkWrap(true);
        }
        return list;
    }
}
