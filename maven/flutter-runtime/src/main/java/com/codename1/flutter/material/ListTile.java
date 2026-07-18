package com.codename1.flutter.material;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * A material list row: leading | (title above subtitle) | trailing, with a
 * 56lp minimum height, 16lp horizontal padding and an onTap callback.
 * Backed by a CN1 Container (UIID "FlutterListTile") plus a transparent tap
 * overlay.
 */
public class ListTile extends Widget {

    private Widget leading;
    private Widget title;
    private Widget subtitle;
    private Widget trailing;
    private Funcs.VoidFunc0 onTap;

    public void leading(Widget v) {
        this.leading = v;
    }

    public void title(Widget v) {
        this.title = v;
    }

    public void subtitle(Widget v) {
        this.subtitle = v;
    }

    public void trailing(Widget v) {
        this.trailing = v;
    }

    public void onTap(Funcs.VoidFunc0 v) {
        this.onTap = v;
    }

    public Widget getLeading() {
        return leading;
    }

    public Widget getTitle() {
        return title;
    }

    public Widget getSubtitle() {
        return subtitle;
    }

    public Widget getTrailing() {
        return trailing;
    }

    public Funcs.VoidFunc0 getOnTap() {
        return onTap;
    }

    @Override
    public Element createElement() {
        return new ListTileRenderElement(this);
    }
}
