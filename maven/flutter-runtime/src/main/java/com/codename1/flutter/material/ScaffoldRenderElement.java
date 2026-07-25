package com.codename1.flutter.material;

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
        bodyChild = updateChild(bodyChild, scaffold().getBody(), 1);
        fabChild = updateChild(fabChild, scaffold().getFloatingActionButton(), 2);
        syncDrawer();
        syncBottomNav();
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
        if (fabChild != null) {
            visitor.call(fabChild);
        }
        if (drawerChild != null) {
            visitor.call(drawerChild);
        }
        if (bottomNavChild != null) {
            visitor.call(bottomNavChild);
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
        RenderElement navRender = renderOf(bottomNavChild);
        if (navRender != null && !rootMode) {
            Size ns = navRender.layout(new BoxConstraints(
                    constraints.hasBoundedWidth() ? width : 0,
                    constraints.hasBoundedWidth() ? width : Double.POSITIVE_INFINITY,
                    0, Double.POSITIVE_INFINITY));
            navHeight = ns.height();
            width = Math.max(width, ns.width());
        }

        // Body fills the remaining area.
        RenderElement bodyRender = renderOf(bodyChild);
        if (bodyRender != null) {
            BoxConstraints bodyConstraints;
            if (constraints.hasBoundedWidth() && constraints.hasBoundedHeight()) {
                bodyConstraints = BoxConstraints.tight(width,
                        Math.max(0, height - appBarHeight - navHeight));
            } else {
                bodyConstraints = constraints.loosen().deflate(
                        com.codename1.flutter.EdgeInsets.only(0, appBarHeight, 0, navHeight));
            }
            Size bs = bodyRender.layout(bodyConstraints);
            setChildOffset(bodyRender, 0, appBarHeight);
            width = Math.max(width, bs.width());
            height = Math.max(height, appBarHeight + bs.height() + navHeight);
        }

        Size self = constraints.constrain(new Size(width, height));

        // The bottom strip sits flush with the final bottom edge.
        if (navRender != null && !rootMode) {
            setChildOffset(navRender, 0, Math.max(0, self.height() - navHeight));
        }

        // FAB overlays bottom-right with a 16lp margin, above the bottom strip.
        RenderElement fabRender = renderOf(fabChild);
        if (fabRender != null) {
            Size fs = fabRender.layout(BoxConstraints.loose(self.width(), self.height()));
            double margin = Dp.px(FAB_MARGIN_LP);
            setChildOffset(fabRender,
                    Math.max(0, self.width() - fs.width() - margin),
                    Math.max(0, self.height() - fs.height() - margin - navHeight));
        }
        return self;
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
