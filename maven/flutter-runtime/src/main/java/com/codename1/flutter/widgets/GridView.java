package com.codename1.flutter.widgets;

import com.codename1.flutter.EdgeInsets;
import com.codename1.flutter.Element;
import com.codename1.flutter.Key;
import com.codename1.flutter.Widget;

import dart.core.DartList;

/**
 * A scrollable grid with a fixed number of cross-axis cells, created via
 * Dart's {@code GridView.count} named constructor. Cell width is the
 * viewport width divided by {@code crossAxisCount} (minus spacing), cell
 * height is {@code cellWidth / childAspectRatio}.
 */
public class GridView extends Widget {

    private long crossAxisCount = 1;
    private Double childAspectRatio;
    private Double mainAxisSpacing;
    private Double crossAxisSpacing;
    private EdgeInsets padding;
    private DartList<Widget> children;

    private GridView() {
    }

    /**
     * Dart's {@code GridView.count} named constructor in canonical positional
     * form.
     */
    public static GridView count(Key key, long crossAxisCount, Double childAspectRatio,
                                 Double mainAxisSpacing, Double crossAxisSpacing,
                                 EdgeInsets padding, DartList<Widget> children) {
        GridView g = new GridView();
        g.key(key);
        g.crossAxisCount = Math.max(1, crossAxisCount);
        g.childAspectRatio = childAspectRatio;
        g.mainAxisSpacing = mainAxisSpacing;
        g.crossAxisSpacing = crossAxisSpacing;
        g.padding = padding;
        g.children = children;
        return g;
    }

    public long getCrossAxisCount() {
        return crossAxisCount;
    }

    public Double getChildAspectRatio() {
        return childAspectRatio;
    }

    public Double getMainAxisSpacing() {
        return mainAxisSpacing;
    }

    public Double getCrossAxisSpacing() {
        return crossAxisSpacing;
    }

    public EdgeInsets getPadding() {
        return padding;
    }

    public DartList<Widget> getChildren() {
        return children;
    }

    @Override
    public Element createElement() {
        return new GridViewRenderElement(this);
    }
}
