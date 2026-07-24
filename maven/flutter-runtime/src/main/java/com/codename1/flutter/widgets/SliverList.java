package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.CrossAxisAlignment;
import com.codename1.flutter.MainAxisSize;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

import dart.core.DartList;

/**
 * A sliver that lays its delegate's children out linearly — Flutter's
 * {@code SliverList}. Modeled as a non-scrolling {@link Column} (the enclosing
 * {@link CustomScrollView} provides the scroll viewport).
 */
public class SliverList extends StatelessWidget {

    private SliverChildDelegate delegate;

    public void delegate(SliverChildDelegate v) {
        this.delegate = v;
    }

    @Override
    public Widget build(BuildContext context) {
        DartList<Widget> kids = delegate != null
                ? delegate.buildChildren(context) : new DartList<Widget>();
        Column col = new Column();
        col.crossAxisAlignment(CrossAxisAlignment.stretch);
        col.mainAxisSize(MainAxisSize.min);
        col.children(kids);
        return col;
    }
}
