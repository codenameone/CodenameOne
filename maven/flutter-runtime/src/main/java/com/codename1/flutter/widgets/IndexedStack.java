package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

import dart.core.DartList;

/**
 * Shows a single child of a stack by {@code index}, keeping the others in the
 * tree — Flutter's {@code IndexedStack}. This pass lays every child out (via
 * {@link SimpleChildrenRenderElement}); showing only the selected index is a
 * later paint-pass refinement, so {@code index} is captured.
 */
public class IndexedStack extends Widget {

    private Object alignment;
    private Object textDirection;
    private Object sizing;
    private long index;
    private DartList<Widget> children;

    public void alignment(Object v) { this.alignment = v; }
    public void textDirection(Object v) { this.textDirection = v; }
    public void sizing(Object v) { this.sizing = v; }
    public void index(long v) { this.index = v; }

    public void children(DartList<Widget> v) {
        this.children = v;
    }

    public long getIndex() {
        return index;
    }

    public DartList<Widget> getChildren() {
        return children;
    }

    @Override
    public Element createElement() {
        return new SimpleChildrenRenderElement(this, new SimpleChildrenRenderElement.Children() {
            @Override
            public DartList<Widget> get() {
                return children;
            }
        });
    }
}
