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
    private Double titleSpacing;
    private Double toolbarHeight;

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
        this.titleSpacing = Double.valueOf(v);
    }

    public void toolbarHeight(double v) {
        this.toolbarHeight = Double.valueOf(v);
    }

    /** The gap either side of the title, or null for NavigationToolbar's 16lp default. */
    public Double getTitleSpacing() {
        return titleSpacing;
    }

    /** The bar's height, or null for the 56lp Material default. */
    public Double getToolbarHeight() {
        return toolbarHeight;
    }

    private IconThemeData iconTheme;
    private Color foregroundColor;

    public void iconTheme(IconThemeData v) {
        this.iconTheme = v;
    }

    /** {@code AppBar.iconTheme} — the style for the bar's leading and action icons. */
    public IconThemeData getIconTheme() {
        return iconTheme;
    }

    public void foregroundColor(Color v) {
        this.foregroundColor = v;
    }

    /** {@code AppBar.foregroundColor} — the colour of the title and the icons. */
    public Color getForegroundColor() {
        return foregroundColor != null ? foregroundColor
                : (iconTheme != null ? iconTheme.color() : null);
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
