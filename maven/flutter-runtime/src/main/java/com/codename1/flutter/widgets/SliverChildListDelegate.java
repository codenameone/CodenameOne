package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Widget;

import dart.core.DartList;

/**
 * A child delegate backed by an explicit list — Flutter's
 * {@code SliverChildListDelegate}.
 */
public class SliverChildListDelegate extends SliverChildDelegate {

    private final DartList<Widget> children;

    public SliverChildListDelegate(DartList<Widget> children) {
        this.children = children;
    }

    public void addAutomaticKeepAlives(boolean v) {
    }

    public void addRepaintBoundaries(boolean v) {
    }

    public void addSemanticIndexes(boolean v) {
    }

    @Override
    public DartList<Widget> buildChildren(BuildContext context) {
        return children != null ? children : new DartList<Widget>();
    }
}
