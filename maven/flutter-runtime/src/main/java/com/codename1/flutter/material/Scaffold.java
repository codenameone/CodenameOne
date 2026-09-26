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
    private FloatingActionButtonLocation floatingActionButtonLocation;
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

    /** {@code Scaffold.persistentFooterButtons} — controls pinned above the bottom edge. */
    public DartList<Widget> getPersistentFooterButtons() {
        return persistentFooterButtons;
    }

    public void endDrawer(Widget v) {
        this.endDrawer = v;
    }

    public void bottomSheet(Widget v) {
        this.bottomSheet = v;
    }

    public void floatingActionButtonLocation(FloatingActionButtonLocation v) {
        this.floatingActionButtonLocation = v;
    }

    /** Where the FAB sits; null means Flutter's default, {@code endFloat}. */
    public FloatingActionButtonLocation getFloatingActionButtonLocation() {
        return floatingActionButtonLocation;
    }

    private boolean extendBody;

    /** Whether the body extends behind the bottom navigation bar — Flutter's {@code extendBody}. */
    public void extendBody(boolean v) {
        this.extendBody = v;
    }

    /**
     * {@code Scaffold.extendBody}.
     *
     * <p>This was an EMPTY SETTER: the flag was discarded, so the body always stopped
     * above the bottom bar. A bar with a notch then had nothing to reveal through it --
     * the mail study cuts a notch for its docked button and the gap came out opaque,
     * showing the scaffold's own background where the list should run underneath.</p>
     */
    public boolean getExtendBody() {
        return extendBody;
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
