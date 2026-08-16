package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.EdgeInsets;
import com.codename1.flutter.Element;
import com.codename1.flutter.Key;
import com.codename1.flutter.Widget;

import dart.core.DartList;
import dart.runtime.Funcs;

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
    private Long itemCount;
    private Funcs.Func2<BuildContext, Long, Widget> itemBuilder;

    private GridView() {
    }

    /**
     * Dart's {@code GridView.builder} named constructor. The cross-axis count
     * carried by {@code gridDelegate} is not decoded at this milestone (held
     * opaquely); items build lazily like {@link com.codename1.flutter.widgets.ListView#builder}.
     */
    public static GridView builder(Key key, Long itemCount,
                                   Funcs.Func2<BuildContext, Long, Widget> itemBuilder,
                                   Object gridDelegate, EdgeInsets padding, Boolean shrinkWrap,
                                   Object physics) {
        GridView g = new GridView();
        g.key(key);
        g.itemCount = itemCount;
        g.itemBuilder = itemBuilder;
        g.padding = padding;
        return g;
    }

    /**
     * Dart's {@code GridView.count} named constructor in canonical positional
     * form.
     */
    public static GridView count(Key key, String restorationId, Object physics, Boolean primary,
                                 long crossAxisCount, Double childAspectRatio,
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

    public Long getItemCount() {
        return itemCount;
    }

    public Funcs.Func2<BuildContext, Long, Widget> getItemBuilder() {
        return itemBuilder;
    }

    public boolean isBuilderMode() {
        return itemBuilder != null;
    }

    @Override
    public Element createElement() {
        return new GridViewRenderElement(this);
    }
}
