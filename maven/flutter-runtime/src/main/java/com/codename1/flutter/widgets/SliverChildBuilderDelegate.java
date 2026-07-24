package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Widget;

import dart.core.DartList;
import dart.runtime.Funcs;

/**
 * A lazily-built child delegate — Flutter's {@code SliverChildBuilderDelegate}.
 * Materializes {@code builder(context, index)} for {@code 0..childCount-1}, or,
 * when {@code childCount} is null (an infinite delegate), until the builder
 * returns null (Flutter's end-of-list convention), capped to keep the eager
 * materialization bounded.
 */
public class SliverChildBuilderDelegate extends SliverChildDelegate {

    private static final int UNBOUNDED_CAP = 10000;

    private final Funcs.Func2<BuildContext, Long, Widget> builder;
    private Long childCount;

    public SliverChildBuilderDelegate(Funcs.Func2<BuildContext, Long, Widget> builder) {
        this.builder = builder;
    }

    public void childCount(long v) {
        this.childCount = v;
    }

    public void addAutomaticKeepAlives(boolean v) {
    }

    public void addRepaintBoundaries(boolean v) {
    }

    public void addSemanticIndexes(boolean v) {
    }

    @Override
    public DartList<Widget> buildChildren(BuildContext context) {
        DartList<Widget> out = new DartList<Widget>();
        if (builder == null) {
            return out;
        }
        long count = childCount != null ? childCount : UNBOUNDED_CAP;
        for (long i = 0; i < count; i++) {
            Widget w = builder.call(context, i);
            if (w == null) {
                break;
            }
            out.add(w);
        }
        return out;
    }
}
