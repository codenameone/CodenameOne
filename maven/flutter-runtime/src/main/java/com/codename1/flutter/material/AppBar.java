/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
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
    private Widget flexibleSpace;
    private com.codename1.flutter.TextStyle titleTextStyle;
    private boolean primary = true;

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

    /**
     * {@code AppBar.foregroundColor} — the colour of the title and of any icon
     * that has no icon theme of its own.
     *
     * <p>Does NOT fall back to {@code iconTheme}: an icon theme colours icons,
     * and reading it as the bar's foreground tints the title with it too. The
     * icons resolve their own colour through {@code IconTheme}.</p>
     */
    public Color getForegroundColor() {
        return foregroundColor;
    }

    /** Flutter's {@code AppBar.flexibleSpace} — a widget stacked behind the toolbar. */
    public void flexibleSpace(Widget v) {
        this.flexibleSpace = v;
    }

    /**
     * {@code AppBar.flexibleSpace} — the widget that fills the bar behind the
     * leading/title/actions row.
     *
     * <p>This used to be accepted and dropped, which is a quiet way to lose a
     * whole screen: Crane builds its entire bar — logo and FLY/SLEEP/EAT tabs —
     * as {@code AppBar(flexibleSpace: CraneAppBar(...))} with no title at all,
     * so the bar rendered empty.</p>
     */
    public Widget getFlexibleSpace() {
        return flexibleSpace;
    }

    /**
     * {@code AppBar.primary} — whether this bar sits at the top of the screen
     * and must therefore clear the status bar itself.
     */
    public void primary(boolean v) {
        this.primary = v;
    }

    public boolean isPrimary() {
        return primary;
    }

    /** {@code AppBar.titleTextStyle} — the style for the title, over the theme's. */
    public void titleTextStyle(com.codename1.flutter.TextStyle v) {
        this.titleTextStyle = v;
    }

    public com.codename1.flutter.TextStyle getTitleTextStyle() {
        return titleTextStyle;
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

    /** {@code PreferredSizeWidget.preferredSize}: the bar's height. */
    public Size preferredSize() {
        return new Size(Double.POSITIVE_INFINITY,
                toolbarHeight == null ? DEFAULT_TOOLBAR_HEIGHT : toolbarHeight.doubleValue());
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
