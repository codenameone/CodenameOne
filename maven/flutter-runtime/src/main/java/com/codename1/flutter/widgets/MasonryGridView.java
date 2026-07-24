package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Element;
import com.codename1.flutter.Key;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * A staggered, Pinterest-style grid from the {@code flutter_staggered_grid_view}
 * package — {@code MasonryGridView}. crane's backdrop builds one via the
 * {@code .count} constructor to lay out destination cards. This milestone
 * captures the grid configuration and item builder; the staggered layout /
 * windowed building is deferred to a later milestone.
 */
public class MasonryGridView extends Widget {

    private String restorationId;
    private long crossAxisCount = 1;
    private Double mainAxisSpacing;
    private Double crossAxisSpacing;
    private Long itemCount;
    private Funcs.Func2<BuildContext, Long, Widget> itemBuilder;

    public MasonryGridView() {
    }

    /** Dart's {@code MasonryGridView.count} named constructor in positional form. */
    public static MasonryGridView count(Key key, String restorationId, long crossAxisCount,
                                        Double mainAxisSpacing, Double crossAxisSpacing,
                                        Long itemCount, Funcs.Func2<BuildContext, Long, Widget> itemBuilder, Object scrollDirection,
                                        Boolean shrinkWrap, Object physics, Object padding,
                                        Object controller) {
        MasonryGridView g = new MasonryGridView();
        g.key(key);
        g.restorationId = restorationId;
        g.crossAxisCount = Math.max(1, crossAxisCount);
        g.mainAxisSpacing = mainAxisSpacing;
        g.crossAxisSpacing = crossAxisSpacing;
        g.itemCount = itemCount;
        g.itemBuilder = itemBuilder;
        return g;
    }

    public long getCrossAxisCount() {
        return crossAxisCount;
    }

    public Long getItemCount() {
        return itemCount;
    }

    public Funcs.Func2<BuildContext, Long, Widget> getItemBuilder() {
        return itemBuilder;
    }

    public String getRestorationId() {
        return restorationId;
    }

    @Override
    public Element createElement() {
        return new MasonryGridViewRenderElement(this);
    }
}
