package com.codename1.flutter.material;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

import dart.core.DartList;
import dart.runtime.Funcs;

/**
 * A material bottom navigation bar: items rendered icon-above-label in
 * equal-width slots, the {@code currentIndex} item tinted with the theme's
 * primary color, {@code onTap(index)} fired on press. 80lp tall (M3
 * navigation bar height). As a root Scaffold's {@code bottomNavigationBar}
 * it renders into the Form's SOUTH region; embedded Scaffolds lay it out as
 * a bottom strip.
 */
public class BottomNavigationBar extends Widget {

    private DartList<BottomNavigationBarItem> items;
    private Long currentIndex;
    private Funcs.VoidFunc1<Long> onTap;

    public void items(DartList<BottomNavigationBarItem> v) {
        this.items = v;
    }

    public void currentIndex(long v) {
        this.currentIndex = v;
    }

    public void onTap(Funcs.VoidFunc1<Long> v) {
        this.onTap = v;
    }

    public DartList<BottomNavigationBarItem> getItems() {
        return items;
    }

    /** Flutter default: 0. */
    public long getCurrentIndex() {
        return currentIndex == null ? 0 : currentIndex;
    }

    public Funcs.VoidFunc1<Long> getOnTap() {
        return onTap;
    }

    @Override
    public Element createElement() {
        return new BottomNavigationBarRenderElement(this);
    }
}
