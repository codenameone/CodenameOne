package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Widget;

import dart.core.DartList;

/**
 * Supplies children to a sliver ({@link SliverList}, {@link SliverGrid}) —
 * Flutter's {@code SliverChildDelegate}. In this runtime a delegate can
 * eagerly materialize its children into a list for the composited scrollable.
 */
public abstract class SliverChildDelegate {

    /** Builds all children of this delegate in order. */
    public abstract DartList<Widget> buildChildren(BuildContext context);
}
