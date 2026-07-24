package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

import dart.core.DartList;

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
    private Color backgroundColor;
    private Boolean resizeToAvoidBottomInset;
    private DartList<Widget> persistentFooterButtons;
    private Widget endDrawer;
    private Widget bottomSheet;

    public void backgroundColor(Color v) {
        this.backgroundColor = v;
    }

    public void resizeToAvoidBottomInset(boolean v) {
        this.resizeToAvoidBottomInset = v;
    }

    public void persistentFooterButtons(DartList<Widget> v) {
        this.persistentFooterButtons = v;
    }

    public void endDrawer(Widget v) {
        this.endDrawer = v;
    }

    public void bottomSheet(Widget v) {
        this.bottomSheet = v;
    }

    public void floatingActionButtonLocation(FloatingActionButtonLocation v) {
    }

    /** Whether the body extends behind the bottom navigation bar — Flutter's {@code extendBody}. */
    public void extendBody(boolean v) {
    }

    /** Whether the body extends behind the app bar — Flutter's {@code extendBodyBehindAppBar}. */
    public void extendBodyBehindAppBar(boolean v) {
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

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

    private static final ScaffoldState STATE = new ScaffoldState() {
    };

    /**
     * The nearest scaffold's mutable state ({@code Scaffold.of(context)}),
     * used to show snack bars and bottom sheets and to open the drawers.
     */
    public static ScaffoldState of(BuildContext context) {
        return STATE;
    }

    @Override
    public Element createElement() {
        return new ScaffoldRenderElement(this);
    }
}
