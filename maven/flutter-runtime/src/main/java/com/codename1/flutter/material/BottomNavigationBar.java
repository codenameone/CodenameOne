package com.codename1.flutter.material;

import com.codename1.flutter.Color;
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
    private BottomNavigationBarType type;
    private Color backgroundColor;
    private Color selectedItemColor;
    private Color unselectedItemColor;
    private Double selectedFontSize;
    private Double unselectedFontSize;

    public void type(BottomNavigationBarType v) {
        this.type = v;
    }

    public void backgroundColor(Color v) {
        this.backgroundColor = v;
    }

    public void selectedItemColor(Color v) {
        this.selectedItemColor = v;
    }

    public void unselectedItemColor(Color v) {
        this.unselectedItemColor = v;
    }

    public void selectedFontSize(double v) {
        this.selectedFontSize = v;
    }

    public void unselectedFontSize(double v) {
        this.unselectedFontSize = v;
    }

    public void showUnselectedLabels(boolean v) {
    }

    public void showSelectedLabels(boolean v) {
    }

    public void elevation(double v) {
    }

    public void iconSize(double v) {
    }

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
