package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.MainAxisAlignment;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.TextStyle;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Row;
import com.codename1.flutter.widgets.SingleChildScrollView;

import dart.core.DartList;
import dart.runtime.Funcs;

/**
 * A horizontal row of tabs — Flutter's {@code TabBar}. Renders the {@code tabs}
 * as an evenly-spaced (or, when {@code isScrollable}, horizontally scrollable)
 * {@link Row}. Selection tinting, the sliding indicator and gesture-driven tab
 * switching via the {@link TabController} are deferred; the labels render.
 */
public class TabBar extends StatelessWidget {

    private DartList<Widget> tabs;
    private TabController controller;
    private boolean isScrollable;
    private Color labelColor;
    private Color unselectedLabelColor;
    private TextStyle labelStyle;
    private TextStyle unselectedLabelStyle;
    private Funcs.VoidFunc1<Long> onTap;

    public void tabs(DartList<Widget> v) {
        this.tabs = v;
    }

    public void controller(TabController v) {
        this.controller = v;
    }

    public void isScrollable(boolean v) {
        this.isScrollable = v;
    }

    public void indicatorColor(Color v) {
    }

    public void indicatorWeight(double v) {
    }

    public void indicatorPadding(Object v) {
    }

    public void indicator(Object v) {
    }

    public void indicatorSize(Object v) {
    }

    public void labelColor(Color v) {
        this.labelColor = v;
    }

    public void labelStyle(TextStyle v) {
        this.labelStyle = v;
    }

    public void labelPadding(Object v) {
    }

    public void unselectedLabelColor(Color v) {
        this.unselectedLabelColor = v;
    }

    public void unselectedLabelStyle(TextStyle v) {
        this.unselectedLabelStyle = v;
    }

    public void padding(Object v) {
    }

    public void dragStartBehavior(Object v) {
    }

    public void mouseCursor(Object v) {
    }

    public void enableFeedback(boolean v) {
    }

    public void physics(Object v) {
    }

    public void onTap(Funcs.VoidFunc1<Long> v) {
        this.onTap = v;
    }

    @Override
    public Widget build(BuildContext context) {
        Row row = new Row();
        row.mainAxisAlignment(MainAxisAlignment.spaceBetween);
        row.children(tabs != null ? tabs : new DartList<Widget>());
        if (isScrollable) {
            SingleChildScrollView sv = new SingleChildScrollView();
            sv.child(row);
            return sv;
        }
        return row;
    }
}
