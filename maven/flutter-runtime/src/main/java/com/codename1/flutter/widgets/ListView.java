package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.EdgeInsets;
import com.codename1.flutter.Element;
import com.codename1.flutter.Key;
import com.codename1.flutter.Widget;

import dart.core.DartList;
import dart.core.UnsupportedError;
import dart.runtime.Funcs;

/**
 * A scrollable vertical list. Two modes:
 * <ul>
 *   <li><b>Children mode</b> ({@code new ListView()} + setters): the given
 *       children stacked in a scrollable column.</li>
 *   <li><b>Builder mode</b> ({@link #builder}): M2 materializes
 *       {@code itemBuilder(context, index)} EAGERLY for every index in
 *       {@code 0..itemCount-1}; windowed/lazy building is an M3 milestone,
 *       which is also why a null (infinite) {@code itemCount} is rejected
 *       with an {@link UnsupportedError}.</li>
 * </ul>
 */
public class ListView extends Widget {

    private DartList<Widget> children;
    private EdgeInsets padding;
    private boolean shrinkWrap;
    private Long itemCount;
    private Funcs.Func2<BuildContext, Long, Widget> itemBuilder;
    private String restorationId;
    private ScrollPhysics physics;
    private boolean reverse;
    private com.codename1.flutter.Axis scrollDirection = com.codename1.flutter.Axis.vertical;
    private ScrollController controller;

    public void restorationId(String v) {
        this.restorationId = v;
    }

    public void physics(ScrollPhysics v) {
        this.physics = v;
    }

    public void reverse(boolean v) {
        this.reverse = v;
    }

    public void scrollDirection(com.codename1.flutter.Axis v) {
        this.scrollDirection = v;
    }

    public void controller(ScrollController v) {
        this.controller = v;
    }

    public ScrollPhysics getPhysics() {
        return physics;
    }

    public boolean getReverse() {
        return reverse;
    }

    public com.codename1.flutter.Axis getScrollDirection() {
        return scrollDirection;
    }

    public ListView() {
    }

    /**
     * Dart's {@code ListView.builder} named constructor, in canonical positional form.
     *
     * <p>The trailing parameters are not decoration. A named argument this factory does not
     * declare is dropped by the transpiler without a word, so {@code shrinkWrap: true} —
     * which is how a list inside a Column says "size to your content" — used to be
     * discarded, and the list took the whole height it was offered instead. The settings
     * page's expanding options list is exactly that shape.</p>
     */
    public static ListView builder(Key key, Long itemCount,
                                   Funcs.Func2<BuildContext, Long, Widget> itemBuilder,
                                   EdgeInsets padding, Boolean shrinkWrap, Object physics,
                                   com.codename1.flutter.Axis scrollDirection, Object controller,
                                   String restorationId, Boolean primary, Double itemExtent,
                                   Boolean reverse) {
        if (itemCount == null) {
            throw new UnsupportedError(
                    "ListView.builder without itemCount (an infinite list) is not supported in M2; "
                            + "items are materialized eagerly and windowed building lands in M3");
        }
        ListView l = new ListView();
        l.key(key);
        l.itemCount = itemCount;
        l.itemBuilder = itemBuilder;
        l.padding = padding;
        l.shrinkWrap = shrinkWrap != null && shrinkWrap.booleanValue();
        if (scrollDirection != null) {
            l.scrollDirection(scrollDirection);
        }
        l.restorationId(restorationId);
        if (itemExtent != null) {
            l.itemExtent(itemExtent.doubleValue());
        }
        if (reverse != null) {
            l.reverse(reverse.booleanValue());
        }
        if (primary != null) {
            l.primary(primary.booleanValue());
        }
        return l;
    }

    public void children(DartList<Widget> v) {
        this.children = v;
    }

    public void padding(EdgeInsets v) {
        this.padding = v;
    }

    public void shrinkWrap(boolean v) {
        this.shrinkWrap = v;
    }

    /**
     * A fixed per-item extent along the scroll axis — Flutter's
     * {@code ListView.itemExtent}. Held for a later layout pass.
     */
    public void itemExtent(double v) {
    }

    /**
     * Whether this is the primary scroll view associated with the parent
     * {@code PrimaryScrollController} ({@code ListView.primary}). Accepted for
     * API compatibility; scroll-controller association is not modelled here.
     */
    public void primary(boolean v) {
    }

    public DartList<Widget> getChildren() {
        return children;
    }

    public EdgeInsets getPadding() {
        return padding;
    }

    public boolean getShrinkWrap() {
        return shrinkWrap;
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

    /**
     * Dart's {@code ListView.separated} named constructor. The separators are
     * not materialized at this milestone (a later pass interleaves
     * {@code separatorBuilder(context, index)} between items); the items
     * themselves build exactly like {@link #builder}.
     */
    public static ListView separated(Key key, Boolean primary, Long itemCount,
                                     Funcs.Func2<BuildContext, Long, Widget> itemBuilder,
                                     Funcs.Func2<BuildContext, Long, Widget> separatorBuilder,
                                     EdgeInsets padding, Boolean shrinkWrap) {
        ListView l = builder(key, itemCount, itemBuilder, padding, shrinkWrap,
                null, null, null, null, null, null, null);
        if (shrinkWrap != null) {
            l.shrinkWrap(shrinkWrap);
        }
        return l;
    }

    @Override
    public Element createElement() {
        return new ListViewRenderElement(this);
    }
}
