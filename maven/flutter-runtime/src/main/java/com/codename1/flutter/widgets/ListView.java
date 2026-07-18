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

    public ListView() {
    }

    /**
     * Dart's {@code ListView.builder} named constructor in canonical
     * positional form.
     */
    public static ListView builder(Key key, Long itemCount,
                                   Funcs.Func2<BuildContext, Long, Widget> itemBuilder,
                                   EdgeInsets padding) {
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

    @Override
    public Element createElement() {
        return new ListViewRenderElement(this);
    }
}
