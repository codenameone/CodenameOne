package com.codename1.flutter.material;

import com.codename1.flutter.Color;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.Size;
import com.codename1.flutter.services.SystemUiOverlayStyle;

import dart.core.DartList;

/**
 * A material app bar. Under a root Scaffold it renders into the CN1 Form's
 * Toolbar (title component + toolbar background color); elsewhere it renders
 * as a strip at the top of the Flutter canvas.
 */
public class AppBar extends Widget {

    /** Flutter's default toolbar height in logical pixels. */
    public static final double DEFAULT_TOOLBAR_HEIGHT = 56;

    private Widget title;
    private Color backgroundColor;
    private boolean centerTitle;
    private boolean centerTitleSet;
    private DartList<Widget> actions;
    private Widget leading;
    private boolean automaticallyImplyLeading = true;
    private Widget bottom;
    private Double elevation;
    private SystemUiOverlayStyle systemOverlayStyle;

    public void title(Widget v) {
        this.title = v;
    }

    public void backgroundColor(Color v) {
        this.backgroundColor = v;
    }

    public void actions(DartList<Widget> v) {
        this.actions = v;
    }

    public void leading(Widget v) {
        this.leading = v;
    }

    public void automaticallyImplyLeading(boolean v) {
        this.automaticallyImplyLeading = v;
    }

    public void bottom(Widget v) {
        this.bottom = v;
    }

    public void elevation(double v) {
        this.elevation = v;
    }

    public void systemOverlayStyle(SystemUiOverlayStyle v) {
        this.systemOverlayStyle = v;
    }

    public void titleSpacing(double v) {
    }

    public void toolbarHeight(double v) {
    }

    public void iconTheme(IconThemeData v) {
    }

    public void foregroundColor(Color v) {
    }

    /** Flutter's {@code AppBar.flexibleSpace} — a widget stacked behind the toolbar. */
    public void flexibleSpace(Widget v) {
    }

    public DartList<Widget> getActions() {
        return actions;
    }

    public Widget getLeading() {
        return leading;
    }

    public boolean getAutomaticallyImplyLeading() {
        return automaticallyImplyLeading;
    }

    public Widget getBottom() {
        return bottom;
    }

    public Double getElevation() {
        return elevation;
    }

    /** {@code PreferredSizeWidget.preferredSize}: the toolbar's fixed height. */
    public Size preferredSize() {
        return new Size(Double.POSITIVE_INFINITY, DEFAULT_TOOLBAR_HEIGHT);
    }

    public void centerTitle(boolean v) {
        this.centerTitle = v;
        this.centerTitleSet = true;
    }

    public Widget getTitle() {
        return title;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public boolean getCenterTitle() {
        return centerTitle;
    }

    public boolean isCenterTitleSet() {
        return centerTitleSet;
    }

    @Override
    public Element createElement() {
        return new AppBarRenderElement(this);
    }
}
