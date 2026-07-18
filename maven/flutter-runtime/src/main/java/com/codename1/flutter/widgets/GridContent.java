package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

import dart.core.DartList;

/**
 * Internal content widget of {@link GridView} — the non-scrolling grid body
 * placed inside the scroll boundary. Not part of the Dart-facing API.
 */
class GridContent extends Widget {

    private final long crossAxisCount;
    private final Double childAspectRatio;
    private final Double mainAxisSpacing;
    private final Double crossAxisSpacing;
    private final DartList<Widget> children;

    GridContent(long crossAxisCount, Double childAspectRatio, Double mainAxisSpacing,
                Double crossAxisSpacing, DartList<Widget> children) {
        this.crossAxisCount = crossAxisCount;
        this.childAspectRatio = childAspectRatio;
        this.mainAxisSpacing = mainAxisSpacing;
        this.crossAxisSpacing = crossAxisSpacing;
        this.children = children;
    }

    long getCrossAxisCount() {
        return crossAxisCount;
    }

    Double getChildAspectRatio() {
        return childAspectRatio;
    }

    Double getMainAxisSpacing() {
        return mainAxisSpacing;
    }

    Double getCrossAxisSpacing() {
        return crossAxisSpacing;
    }

    DartList<Widget> getChildren() {
        return children;
    }

    @Override
    public Element createElement() {
        return new GridContentRenderElement(this);
    }
}
