package com.codename1.flutter.cupertino;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.material.BottomNavigationBarItem;
import com.codename1.flutter.widgets.Container;

import dart.core.DartList;
import dart.runtime.Funcs;

/**
 * The iOS bottom tab bar — Flutter's {@code CupertinoTabBar}. Used to
 * configure a {@link CupertinoTabScaffold}; the bar itself composes an empty
 * {@link Container} placeholder this pass (the tab items are captured).
 */
public class CupertinoTabBar extends StatelessWidget {

    private DartList<BottomNavigationBarItem> items;
    private Funcs.VoidFunc1<Long> onTap;

    public void items(DartList<BottomNavigationBarItem> v) {
        this.items = v;
    }

    public void onTap(Funcs.VoidFunc1<Long> v) {
        this.onTap = v;
    }

    public void currentIndex(long v) {
    }

    public void backgroundColor(Color v) {
    }

    public void activeColor(Color v) {
    }

    public void inactiveColor(Color v) {
    }

    public void iconSize(double v) {
    }

    public void border(Object v) {
    }

    public DartList<BottomNavigationBarItem> getItems() {
        return items;
    }

    @Override
    public Widget build(BuildContext context) {
        return new Container();
    }
}
