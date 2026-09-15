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

import com.codename1.flutter.animation.AnimationController;

import com.codename1.flutter.Element;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.FlutterRootLayout;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BorderLayout;

import dart.runtime.Funcs;

/**
 * Render element for {@link Scaffold}. Two modes:
 *
 * <ul>
 *   <li><b>Root mode</b> (topmost render element of a {@code FlutterUI.runApp}
 *       tree, i.e. no render ancestor and the host has a Form): the appBar
 *       subtree is routed into a dedicated {@link RenderHost} whose container
 *       becomes the Form Toolbar's title component (its own FlutterRootLayout
 *       lays the title out); the drawer subtree is routed into the Toolbar's
 *       side menu; the bottomNavigationBar subtree is routed into the Form's
 *       BorderLayout SOUTH region; the body fills the whole Flutter canvas;
 *       the FAB is positioned inside the canvas bottom-right with a 16lp
 *       margin.</li>
 *   <li><b>Embedded mode</b> (inside {@code FlutterUI.wrap} or below other
 *       render elements): the appBar renders as an in-canvas strip at the
 *       top, the bottomNavigationBar as an in-canvas strip at the bottom,
 *       the body fills the rest, the FAB overlays bottom-right above the
 *       bottom strip. The drawer is IGNORED in embedded mode (with a log
 *       warning) — a side menu needs the Form Toolbar.</li>
 * </ul>
 *
 * <p>Drawer and bottomNavigationBar hosts are decided at mount time: adding
 * them to a root Scaffold in a later rebuild logs a warning instead of
 * re-plumbing the Form (M3 limitation).</p>
 */
public class ScaffoldRenderElement extends RenderElement {

    private static final double FAB_MARGIN_LP = 16;

    private Element appBarChild;
    private Element bodyChild;
    private Element fabChild;
    private Element drawerChild;
    private Element bottomNavChild;
    private Element footerChild;

    private boolean rootMode;
    private RenderHost toolbarHost;
    private RenderHost drawerHost;
    private RenderHost southHost;
    private boolean warnedDrawer;
    private boolean warnedLateNav;

    public ScaffoldRenderElement(Scaffold widget) {
        super(widget);
    }

    private Scaffold scaffold() {
        return (Scaffold) widget();
    }

    /**
     * The Scaffold's own face: an opaque fill covering its bounds, which the
     * children then paint over (they attach after it in tree order).
     *
     * <p>Flutter's Scaffold is a Material — it is <em>opaque</em>, not a
     * transparent frame. That matters whenever two Scaffolds are stacked, as
     * in a backdrop: without the fill, the page underneath shows through every
     * gap between the front page's children.</p>
     */
    @Override
    protected Component createComponent() {
        if (!com.codename1.ui.Display.isInitialized()) {
            return null;
        }
        Container face = new Container();
        face.setUIID("FlutterScaffold");
        face.getAllStyles().setPadding(0, 0, 0, 0);
        face.getAllStyles().setMargin(0, 0, 0, 0);
        applyBackground(face);
        return face;
    }

    @Override
    protected void updateComponent(Component c) {
        applyBackground(c);
    }

    @Override
    public void themeChanged() {
        super.themeChanged();
        if (component() != null) {
            applyBackground(component());
        }
    }

    private void applyBackground(Component face) {
        com.codename1.flutter.Color bg = effectiveBackground();
        if (bg != null) {
            ThemeDataAdapter.paintColor(face.getAllStyles(), bg);
        }
    }

    /**
     * {@code Scaffold.backgroundColor} when given, else the theme's
     * {@code scaffoldBackgroundColor}, else {@code colorScheme.background} —
     * Flutter's own resolution order.
     */
    private com.codename1.flutter.Color effectiveBackground() {
        if (scaffold().getBackgroundColor() != null) {
            return scaffold().getBackgroundColor();
        }
        try {
            ThemeData theme = Theme.of(this);
            if (theme.scaffoldBackgroundColor() != null) {
                return theme.scaffoldBackgroundColor();
            }
            return theme.colorScheme().background();
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public void mount(Element parent, int slot) {
        // Decide the mode before children mount (they inherit hosts from it).
        rootMode = false;
        Element a = parent;
        boolean hasRenderAncestor = false;
        while (a != null) {
            if (a instanceof RenderElement) {
                hasRenderAncestor = true;
                break;
            }
            a = a.parent();
        }
        RenderHost mountHost = parent != null ? parent.host() : host();
        if (!hasRenderAncestor && mountHost != null && mountHost.form() != null) {
            rootMode = true;
            if (scaffold().getAppBar() != null) {
                mountHost.formToolbarBound(true);
            }
            prepareToolbarHost(mountHost.form());
            prepareDrawerHost(mountHost.form());
            prepareBottomHost(mountHost.form());
        }
        super.mount(parent, slot);
    }

    private void prepareToolbarHost(Form form) {
        if (scaffold().getAppBar() == null) {
            return;
        }
        Toolbar tb = ensureToolbar(form);
        toolbarHost = new RenderHost();
        toolbarHost.toolbarTitleHost(true);
        toolbarHost.toolbar(tb);
        Container titleCnt = new Container(new FlutterRootLayout(toolbarHost));
        toolbarHost.container(titleCnt);
        toolbarHost.rootSupplier(new Funcs.Func0<Element>() {
            @Override
            public Element call() {
                return appBarChild;
            }
        });
        tb.setTitleComponent(titleCnt);
    }

    /**
     * Routes the drawer subtree into the Form Toolbar's side menu via a
     * dedicated host container (its own FlutterRootLayout sizes the panel).
     */
    private void prepareDrawerHost(Form form) {
        if (scaffold().getDrawer() == null) {
            return;
        }
        Toolbar tb = ensureToolbar(form);
        drawerHost = new RenderHost();
        Container drawerCnt = new Container(new FlutterRootLayout(drawerHost));
        drawerHost.container(drawerCnt);
        drawerHost.rootSupplier(new Funcs.Func0<Element>() {
            @Override
            public Element call() {
                return drawerChild;
            }
        });
        tb.addComponentToSideMenu(drawerCnt);
    }

    /**
     * Routes the bottomNavigationBar subtree into the Form's SOUTH region
     * via a dedicated host container (the Form lays it out; its preferred
     * height comes from the bar's 80lp layout).
     */
    private void prepareBottomHost(Form form) {
        if (scaffold().getBottomNavigationBar() == null) {
            return;
        }
        southHost = new RenderHost();
        Container southCnt = new Container(new FlutterRootLayout(southHost));
        southHost.container(southCnt);
        southHost.rootSupplier(new Funcs.Func0<Element>() {
            @Override
            public Element call() {
                return bottomNavChild;
            }
        });
        form.add(BorderLayout.SOUTH, southCnt);
    }

    private static Toolbar ensureToolbar(Form form) {
        Toolbar tb = form.getToolbar();
        if (tb == null) {
            tb = new Toolbar();
            form.setToolbar(tb);
        }
        // FlutterUI hides the Toolbar of a Form nothing claimed. A Scaffold asking for
        // it here is claiming it, which can happen after that decision was taken if a
        // rebuild introduces an AppBar where there was none.
        tb.setHidden(false);
        tb.setVisible(true);
        return tb;
    }

    @Override
    protected RenderHost hostForChild(int slot) {
        if (slot == 0 && rootMode && toolbarHost != null) {
            return toolbarHost;
        }
        if (slot == 3 && rootMode && drawerHost != null) {
            return drawerHost;
        }
        if (slot == 4 && rootMode && southHost != null) {
            return southHost;
        }
        return host();
    }

    @Override
    protected void syncChildren() {
        appBarChild = updateChild(appBarChild, scaffold().getAppBar(), 0);
        bodyChild = updateChild(bodyChild, bodyWidget(), 1);
        footerChild = updateChild(footerChild, footerWidget(), 5);
        syncDrawer();
        syncBottomNav();
        // LAST, because components attach in mount order and that is this host's
        // paint order. Flutter's _ScaffoldSlot puts the FAB after the persistent
        // footer and the bottom navigation bar, so it floats over both; mounted
        // before them it is painted under them, and a DOCKED fab -- which
        // straddles the bar's top edge by design -- loses its whole bottom half.
        fabChild = updateChild(fabChild, fabWidget(), 2);
    }

    /// Scales the floating action button in and out, instead of it appearing and
    /// vanishing between one frame and the next.
    ///
    /// Flutter's Scaffold runs every change of this slot through
    /// {@code FloatingActionButtonAnimator.scaling} over
    /// {@code kFloatingActionButtonSegue}. Dropping the widget outright is a visibly
    /// different thing: Reply hides the button while its mailbox drawer opens, and the
    /// reference still shows it 50ms in and has scaled it away by 100ms, where ours was
    /// simply gone on the first frame of the gesture -- the button did not leave, it was
    /// never there.
    private static final int FAB_SEGUE_MS = 200;

    private AnimationController fabScale;
    private com.codename1.flutter.Widget lastFab;

    private com.codename1.flutter.Widget fabWidget() {
        com.codename1.flutter.Widget now = scaffold().getFloatingActionButton();
        if (fabScale == null) {
            if (now == null) {
                return null;
            }
            fabScale = new AnimationController();
            fabScale.duration(dart.core.Duration.of(0, 0, 0, 0, FAB_SEGUE_MS, 0));
            // Present from the start: a scaffold that opens WITH a button did not
            // animate one in, and scaling the first frame up would be an entrance
            // nobody asked for.
            fabScale.value(1.0);
            fabScale.addListener(new dart.runtime.Funcs.VoidFunc0() {
                @Override
                public void call() {
                    markNeedsBuild();
                }
            });
        }
        if (now != null) {
            lastFab = now;
            if (fabScale.value().doubleValue() < 1.0) {
                fabScale.forward(null);
            }
        } else if (lastFab != null) {
            // Going: keep building the button that is leaving until it has finished
            // leaving. Nothing else is holding it, so dropping it here is what made the
            // exit instant.
            if (fabScale.value().doubleValue() > 0.0) {
                fabScale.reverse(null);
                now = lastFab;
            } else {
                lastFab = null;
            }
        }
        if (now == null) {
            return null;
        }
        com.codename1.flutter.animation.ScaleTransition scaled =
                new com.codename1.flutter.animation.ScaleTransition();
        scaled.scale(fabScale);
        scaled.child(now);
        return scaled;
    }

    /**
     * {@code Scaffold.persistentFooterButtons} as a row pinned above the bottom
     * of the scaffold — Flutter aligns them to the end over a divider.
     *
     * <p>They were captured and never rendered, so the 2D-transformations demo
     * lost its reset and edit controls along with the strip they sit on.</p>
     */
    private com.codename1.flutter.Widget footerWidget() {
        dart.core.DartList<com.codename1.flutter.Widget> buttons =
                scaffold().getPersistentFooterButtons();
        if (buttons == null || buttons.isEmpty()) {
            return null;
        }
        com.codename1.flutter.widgets.Row row = new com.codename1.flutter.widgets.Row();
        row.children(buttons);
        row.mainAxisAlignment(com.codename1.flutter.MainAxisAlignment.end);
        row.mainAxisSize(com.codename1.flutter.MainAxisSize.max);
        com.codename1.flutter.widgets.Padding pad = new com.codename1.flutter.widgets.Padding();
        pad.padding(com.codename1.flutter.EdgeInsets.symmetric(8, 8));
        pad.child(row);
        return pad;
    }

    /**
     * The body, with the top safe-area inset already spent when this scaffold
     * has an app bar.
     *
     * <p>Flutter's Scaffold does the same. The app bar is what clears the notch,
     * so anything below it — including a nested Scaffold with an app bar of its
     * own — must not clear it a second time. The gallery nests exactly that way:
     * a demo page's Scaffold sits in the body of the page's own Scaffold, and
     * without this its bar would be pushed down by a notch that has already been
     * accounted for.</p>
     */
    /**
     * The body, under the ambient theme's body text style.
     *
     * <p>A Scaffold IS a Material in Flutter, and a Material is what establishes the
     * default text style for what it contains. That matters under a NESTED theme: a
     * {@code Theme} is an inherited widget and wraps nothing, so on its own it changes
     * what {@code Theme.of} answers while the ambient text style stays as whoever built
     * it last left it -- the application's.</p>
     *
     * <p>Without this a page that installs a theme of its own took its sizes from that
     * theme and everything else from the application's, because a Text merges its own
     * style OVER the ambient one and overrides only the fields it sets. Measured on the
     * typography demo, whose 96sp display role states no height and no family: it
     * inherited the application's Montserrat at a line height of 1.43 -- a body role's
     * height on a display role -- and its two wrapped lines sat 141 logical pixels apart
     * against the reference's 115.</p>
     */
    private com.codename1.flutter.Widget underThemeTextStyle(
            com.codename1.flutter.Widget body) {
        com.codename1.flutter.TextStyle style = null;
        try {
            ThemeData theme = Theme.of(this);
            style = theme == null || theme.textTheme() == null
                    ? null : theme.textTheme().bodyMedium();
        } catch (Throwable ignore) {
            // A Scaffold outside any theme still has to render its body.
            style = null;
        }
        return style == null ? body
                : com.codename1.flutter.widgets.DefaultTextStyle.wrap(style, body);
    }

    private com.codename1.flutter.Widget bodyWidget() {
        com.codename1.flutter.Widget body = scaffold().getBody();
        if (body == null) {
            return body;
        }
        body = underThemeTextStyle(body);
        // Flutter's own rule for the body slot: the top padding goes when there
        // is an app bar to stand in for it, and the BOTTOM padding goes when
        // there is a bottom bar or a footer standing in for that. What is left
        // reaches the body, and a scroll view inside it applies it along its own
        // axis -- which is how a full-screen list keeps clear of the display
        // cutout. Leaving the bottom padding in place under a bottom bar counted
        // it twice and lengthened every such list by 34 logical pixels.
        boolean removeTop = scaffold().getAppBar() != null;
        boolean removeBottom = scaffold().getBottomNavigationBar() != null
                || scaffold().getPersistentFooterButtons() != null;
        if (!removeTop && !removeBottom) {
            return body;
        }
        return com.codename1.flutter.MediaQuery.removePadding(this, Boolean.FALSE,
                removeTop ? Boolean.TRUE : Boolean.FALSE, Boolean.FALSE,
                removeBottom ? Boolean.TRUE : Boolean.FALSE, body);
    }

    private void syncDrawer() {
        if (rootMode && drawerHost != null) {
            drawerChild = updateChild(drawerChild, scaffold().getDrawer(), 3);
            return;
        }
        if (scaffold().getDrawer() != null && !warnedDrawer) {
            warnedDrawer = true;
            warn(rootMode
                    ? "Scaffold.drawer added after mount is ignored (M3 limitation)"
                    : "Scaffold.drawer is ignored on embedded Scaffolds (needs the Form Toolbar)");
        }
    }

    private void syncBottomNav() {
        if (rootMode && southHost == null) {
            // decided at mount; a later-added bar can't be re-plumbed into
            // the Form (M3 limitation)
            if (scaffold().getBottomNavigationBar() != null && !warnedLateNav) {
                warnedLateNav = true;
                warn("Scaffold.bottomNavigationBar added after mount is ignored (M3 limitation)");
            }
            return;
        }
        bottomNavChild = updateChild(bottomNavChild, scaffold().getBottomNavigationBar(), 4);
    }

    private static void warn(String msg) {
        try {
            com.codename1.io.Log.p("Flutter runtime: " + msg);
        } catch (Throwable t) {
            // headless: Log has no storage backend
        }
    }

    @Override
    public void visitChildren(Funcs.VoidFunc1<Element> visitor) {
        if (appBarChild != null) {
            visitor.call(appBarChild);
        }
        if (bodyChild != null) {
            visitor.call(bodyChild);
        }
        if (drawerChild != null) {
            visitor.call(drawerChild);
        }
        if (bottomNavChild != null) {
            visitor.call(bottomNavChild);
        }
        if (footerChild != null) {
            visitor.call(footerChild);
        }
        // Visited last for the same reason it is mounted last: this order is
        // the host's paint order, and the FAB floats over the bottom strip.
        if (fabChild != null) {
            visitor.call(fabChild);
        }
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        double width = constraints.hasBoundedWidth() ? constraints.maxWidth() : 0;
        double height = constraints.hasBoundedHeight() ? constraints.maxHeight() : 0;

        // App bar: only laid out here in embedded (strip) mode; in root mode
        // it lives in the Toolbar's title host and the Form lays it out.
        double appBarHeight = 0;
        RenderElement appBarRender = renderOf(appBarChild);
        if (appBarRender != null && !rootMode) {
            Size as = appBarRender.layout(new BoxConstraints(
                    constraints.hasBoundedWidth() ? width : 0,
                    constraints.hasBoundedWidth() ? width : Double.POSITIVE_INFINITY,
                    0, Double.POSITIVE_INFINITY));
            appBarHeight = as.height();
            setChildOffset(appBarRender, 0, 0);
            width = Math.max(width, as.width());
        }

        // Bottom navigation strip: embedded mode only; in root mode it lives
        // in the Form's SOUTH host.
        double navHeight = 0;
        // Computed before the body is laid out, because the body has to stop
        // short of it too.
        double bottomBand = coversTheDisplay(constraints.hasBoundedHeight()
                ? new Size(0, constraints.maxHeight()) : null)
                ? bottomSafeAreaPx() : 0;
        RenderElement navRender = renderOf(bottomNavChild);
        if (navRender != null && !rootMode) {
            Size ns = navRender.layout(new BoxConstraints(
                    constraints.hasBoundedWidth() ? width : 0,
                    constraints.hasBoundedWidth() ? width : Double.POSITIVE_INFINITY,
                    0, Double.POSITIVE_INFINITY));
            navHeight = ns.height();
            width = Math.max(width, ns.width());
        }
        // The bar carries the display's inset itself (BottomAppBar.withBottomInset), so
        // adding it here as well both double-counts it and outlives the bar: a bar that
        // animates away left the band behind as a stripe of its own colour. The band is
        // only ours to add when there is no bar to carry it.
        if (navHeight > 0) {
            bottomBand = 0;
        }

        // Persistent footer buttons sit above the bottom strip.
        double footerHeight = 0;
        RenderElement footerRender = renderOf(footerChild);
        if (footerRender != null) {
            Size fs = footerRender.layout(new BoxConstraints(
                    constraints.hasBoundedWidth() ? width : 0,
                    constraints.hasBoundedWidth() ? width : Double.POSITIVE_INFINITY,
                    0, Double.POSITIVE_INFINITY));
            footerHeight = fs.height();
            width = Math.max(width, fs.width());
        }

        // Body fills the remaining area.
        RenderElement bodyRender = renderOf(bodyChild);
        if (bodyRender != null) {
            // extendBody: the body runs BEHIND the bottom bar instead of stopping above
            // it, which is what lets a notch in that bar reveal the page underneath. The
            // flag used to be discarded, so the mail study's notch came out opaque.
            double bodyBottom = scaffold().getExtendBody()
                    ? 0 : navHeight + footerHeight + bottomBand;
            BoxConstraints bodyConstraints;
            if (constraints.hasBoundedWidth() && constraints.hasBoundedHeight()) {
                bodyConstraints = BoxConstraints.tight(width,
                        Math.max(0, height - appBarHeight - bodyBottom));
            } else {
                bodyConstraints = constraints.loosen().deflate(
                        com.codename1.flutter.EdgeInsets.only(0, appBarHeight, 0, bodyBottom));
            }
            Size bs = bodyRender.layout(bodyConstraints);
            setChildOffset(bodyRender, 0, appBarHeight);
            width = Math.max(width, bs.width());
            height = Math.max(height,
                    appBarHeight + bs.height() + navHeight + footerHeight);
        }

        Size self = constraints.constrain(new Size(width, height));

        // The bottom strip sits above the display's own bottom padding, not
        // flush with the screen. Flutter leaves that band to the scaffold's
        // background: the reply study's bar is Material's 80 logical pixels with
        // 34 of dark beneath it, which together read as one 114-tall bar. Laying
        // the bar flush instead pushed the whole body 34 lower and left a white
        // strip where the reference is dark -- the single largest wrong band on
        // that route, 280 device pixels tall and 97% wrong.
        //
        // Only when this scaffold IS the display, which is the one thing that
        // separates a full-screen study from a demo shown inside a card. The
        // size says it, and by here the size is known.
        if (navRender != null && !rootMode) {
            // The strip OWNS the band: it is laid out that much taller and stays
            // flush with the bottom edge, with its content held at the top. That
            // is what the reference draws -- the reply study's bar reads as one
            // 114 logical pixel block of colour whose Inbox row sits in the top
            // 56 of it, not as an 80 tall bar floating above a gap.
            navRender.layout(BoxConstraints.tight(self.width(), navHeight));
            setChildOffset(navRender, 0, Math.max(0, self.height() - navHeight));
        }
        if (footerRender != null) {
            footerRender.layout(BoxConstraints.tight(self.width(), footerHeight));
            setChildOffset(footerRender, 0,
                    Math.max(0, self.height() - navHeight - bottomBand - footerHeight));
        }

        RenderElement fabRender = renderOf(fabChild);
        if (fabRender != null) {
            Size fs = fabRender.layout(BoxConstraints.loose(self.width(), self.height()));
            // Published for the bottom bar, which has to cut a notch for it. Flutter hands
            // the same thing over as ScaffoldGeometry.floatingActionButtonArea. Read at
            // PAINT time, not build time: the bar is built before this layout runs, so at
            // build time there is nothing to read.
            fabSize = fs;
            setChildOffset(fabRender,
                    fabX(scaffold().getFloatingActionButtonLocation(), self.width(), fs.width()),
                    fabY(scaffold().getFloatingActionButtonLocation(), self.height(),
                            fs.height(), contentInset(navHeight + bottomBand,
                                    bottomSafeAreaPx())));
        }
        return self;
    }

    /// The size the docked floating action button was last laid out at, in DEVICE
    /// pixels, or null.
    private Size fabSize;

    /// The nearest enclosing Scaffold's last floating-action-button size, or null. The
    /// bottom bar needs it to carve its notch, and the bar is not a child of the button
    /// -- they are two slots of the same Scaffold -- so it has to ask.
    public static Size fabSizeOf(com.codename1.flutter.Element from) {
        com.codename1.flutter.Element e = from;
        while (e != null) {
            if (e instanceof ScaffoldRenderElement) {
                return ((ScaffoldRenderElement) e).fabSize;
            }
            e = e.parent();
        }
        return null;
    }

    /// Where the nearest enclosing Scaffold docks its floating action button.
    public static FloatingActionButtonLocation fabLocationOf(
            com.codename1.flutter.Element from) {
        com.codename1.flutter.Element e = from;
        while (e != null) {
            if (e instanceof ScaffoldRenderElement) {
                return ((ScaffoldRenderElement) e).scaffold().getFloatingActionButtonLocation();
            }
            e = e.parent();
        }
        return null;
    }

    /**
     * How far the content stops short of the bottom of the scaffold.
     *
     * <p>The bottom strip when there is one, and the display's own bottom
     * padding when there is not. NOT both: a bottom bar is what holds the
     * content off the edge, so adding the inset on top double-counts it. That
     * lifted the reply study's docked button clear of its bar and into the mail
     * list, where it disappeared behind a card.</p>
     */
    /** @see #contentInsetPx(double) */
    static double contentInset(double navHeight, double bottomSafeArea) {
        return navHeight > 0 ? navHeight : bottomSafeArea;
    }

    /// Whether this scaffold IS the display, which is what decides whether the
    /// display's bottom padding is a band the scaffold owns below its bar.
    private boolean coversTheDisplay(Size self) {
        try {
            if (self == null || self.height() <= 0) {
                return false;
            }
            Size screen = com.codename1.flutter.MediaQuery.sizeOf(this);
            return screen != null && Math.abs(Dp.px(screen.height()) - self.height()) < 2;
        } catch (Throwable noMediaQuery) {
            return false;
        }
    }

    /**
     * The bottom safe-area inset in device pixels.
     *
     * <p>Flutter measures a floating action button from the bottom of the
     * CONTENT, which excludes the display's own bottom padding -- the home
     * indicator on this device. Measuring from the bottom of the scaffold
     * instead put the starter study's button 102 device pixels lower than the
     * reference's, sitting over the indicator rather than above it.</p>
     */
    private double bottomSafeAreaPx() {
        try {
            com.codename1.flutter.EdgeInsets p = com.codename1.flutter.MediaQuery.paddingOf(this);
            return p == null ? 0 : Dp.px(p.bottom());
        } catch (Throwable noMediaQuery) {
            return 0;
        }
    }

    /**
     * Where the FAB sits horizontally, from its
     * {@code FloatingActionButtonLocation} -- start, center or end, with
     * Flutter's 16 logical pixel margin at either edge.
     */
    static double fabX(FloatingActionButtonLocation where, double scaffoldWidth, double fabWidth) {
        double margin = Dp.px(FAB_MARGIN_LP);
        if (where != null && where.name().indexOf("enter") >= 0) {
            return Math.max(0, (scaffoldWidth - fabWidth) / 2);
        }
        if (where != null && where.name().startsWith("start")) {
            return margin;
        }
        if (where != null && where.name().startsWith("miniStart")) {
            return margin;
        }
        // Flutter's default is endFloat.
        return Math.max(0, scaffoldWidth - fabWidth - margin);
    }

    /**
     * Where the FAB sits vertically.
     *
     * <p>A FLOATING fab clears the bottom strip by Flutter's margin. A DOCKED
     * one straddles the strip's top edge -- its centre sits exactly on it,
     * which is what lets a notched BottomAppBar cut a hole for it. Reply's
     * compose button is centreDocked, and with the location discarded it drew
     * as an ordinary bottom-right float, in the corner, over the bar.</p>
     */
    static double fabY(FloatingActionButtonLocation where, double scaffoldHeight,
            double fabHeight, double navHeight) {
        double margin = Dp.px(FAB_MARGIN_LP);
        String name = where == null ? "endFloat" : where.name();
        double contentBottom = scaffoldHeight - navHeight;
        if (name.indexOf("Top") >= 0) {
            return margin;
        }
        if (name.indexOf("Docked") >= 0) {
            // Never below the screen, which is Flutter's own clamp.
            return Math.max(0, Math.min(contentBottom - fabHeight / 2,
                    scaffoldHeight - fabHeight - margin));
        }
        return Math.max(0, contentBottom - fabHeight - margin);
    }

    /**
     * The render element for one of our child slots, or null; children
     * routed to another host (root-mode appBar/drawer/bottom bar) are
     * excluded from this host's layout by the base class's host filter.
     */
    private RenderElement renderOf(Element child) {
        RenderElement r = findRenderElement(child);
        if (r != null && r.host() != host()) {
            return rootMode ? null : r;
        }
        return r;
    }
}
