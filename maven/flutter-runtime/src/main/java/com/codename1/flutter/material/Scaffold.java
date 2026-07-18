package com.codename1.flutter.material;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * The basic material page layout: an optional app bar, a body, an optional
 * floating action button overlaid bottom-right, an optional navigation
 * drawer (root Scaffolds only — rendered into the Toolbar side menu) and an
 * optional bottom navigation bar.
 */
public class Scaffold extends Widget {

    private Widget appBar;
    private Widget body;
    private Widget floatingActionButton;
    private Widget drawer;
    private Widget bottomNavigationBar;

    public void appBar(Widget v) {
        this.appBar = v;
    }

    public void body(Widget v) {
        this.body = v;
    }

    public void floatingActionButton(Widget v) {
        this.floatingActionButton = v;
    }

    public void drawer(Widget v) {
        this.drawer = v;
    }

    public void bottomNavigationBar(Widget v) {
        this.bottomNavigationBar = v;
    }

    public Widget getAppBar() {
        return appBar;
    }

    public Widget getBody() {
        return body;
    }

    public Widget getFloatingActionButton() {
        return floatingActionButton;
    }

    public Widget getDrawer() {
        return drawer;
    }

    public Widget getBottomNavigationBar() {
        return bottomNavigationBar;
    }

    @Override
    public Element createElement() {
        return new ScaffoldRenderElement(this);
    }
}
