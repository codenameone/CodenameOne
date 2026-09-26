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
package com.codename1.flutter;

import com.codename1.flutter.rendering.FlutterRootLayout;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

/**
 * Entry points binding a Flutter widget tree to Codename One.
 *
 * <ul>
 *   <li>{@link #runApp(Widget)} — creates a host Form, mounts the tree and
 *       shows the form. Call from a CN1 lifecycle (Display initialized).</li>
 *   <li>{@link #wrap(Widget)} — returns a CN1 Container hosting the subtree,
 *       for embedding Flutter content inside an ordinary CN1 UI.</li>
 * </ul>
 */
public final class FlutterUI {

    private FlutterUI() {
    }

    /**
     * Inflates {@code app} as the root of a new element tree hosted in a new
     * CN1 Form and shows the form.
     */
    public static void runApp(Widget app) {
        assertEdt();
        long t0 = System.currentTimeMillis();
        installMaterialBaseTheme();
        if (startupTrace()) {
            probeComponentCost();
        }
        long t1 = System.currentTimeMillis();
        RenderHost host = mountInNewForm(app);
        long t2 = System.currentTimeMillis();
        host.form().show();
        long t3 = System.currentTimeMillis();
        // From here on, artwork is resolved in the frame that asks for it.
        com.codename1.flutter.widgets.ImageRenderElement.firstFrameShown();
        collectStartupGarbage(host.form());
        // Attribution for the first frame, gated so it costs nothing normally.
        // "The app takes 250ms to start" is not actionable; knowing whether that
        // is the theme, the widget build, or the first layout is.
        if (startupTrace()) {
            System.out.println("BENCH:STARTUP theme=" + (t1 - t0) + "ms mount=" + (t2 - t1)
                    + "ms show=" + (t3 - t2) + "ms images="
                    + com.codename1.flutter.widgets.ImageRenderElement.scalingCost()
                    + " components=" + RenderElement.componentCost()
                    + " " + com.codename1.flutter.rendering.FlutterRootLayout.rootLayoutCost()
                    + " elements=" + Element.mountedCount()
                    + " layoutBuilder[" + com.codename1.flutter.widgets.LayoutBuilderElement.cost() + "]"
                    + " composed[" + ComposedElement.rebuildCost() + "]"
                    + " icons=" + com.codename1.flutter.widgets.IconRenderElement.glyphCost()
                    + " createdBy:" + RenderElement.componentBreakdown()
                    + " layoutSelf:" + RenderElement.hotLayoutClasses(8)
                    + " replaced:" + Element.replacementCensus(6)
                    + " discarded:" + Element.discardCensus(6)
                    + " display:" + com.codename1.flutter.MediaQueryData.sizeHistory());
            System.out.flush();
        }
    }

    /**
     * Asks for one collection once the first screen is up.
     *
     * <p>Start-up is when a UI toolkit makes the most garbage it will ever
     * make: every image decoded at a size it was then resampled from, every
     * builder temporary, every string built to look something up once. None of
     * it is referenced by the frame now on screen, and an application that then
     * sits idle gives the collector no reason to run — so the peak stays
     * charged to the process. Measured on the Mac build, the collector's own
     * freed-but-unreturned pages alone were 21MB against 0.1MB for the same
     * app built with another toolchain.</p>
     *
     * <p>Deferred, so the collection lands after the frame rather than inside
     * it, and it runs on the collector's thread either way.</p>
     */
    private static void collectStartupGarbage(final Form form) {
        try {
            com.codename1.ui.CN.callSerially(new Runnable() {
                @Override
                public void run() {
                    System.gc();
                }
            });
            // And once more a moment later. Start-up garbage clears in two
            // waves: the first collection frees the objects, and only then do
            // the allocator's pages become wholly empty and returnable. One
            // pass leaves most of them still holding a single survivor.
            if (form != null) {
                com.codename1.ui.util.UITimer.timer(1200, false, form, new Runnable() {
                    @Override
                    public void run() {
                        System.gc();
                    }
                });
            }
        } catch (Throwable ignore) {
            // headless, or a port with no collector to ask
        }
    }

    /**
     * Isolates what creating one Codename One component actually costs.
     *
     * <p>Attribution said ~0.3ms per component, uniformly across every element
     * type — which rules out per-widget logic and points at something every
     * component pays. This separates the three candidates: constructing the
     * component, resolving its four styles out of the theme, and mutating
     * those styles.
     */
    private static void probeComponentCost() {
        final int n = 200;
        com.codename1.ui.Label[] kept = new com.codename1.ui.Label[n];
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            kept[i] = new com.codename1.ui.Label("x", "FlutterText");
        }
        long t1 = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            kept[i].getAllStyles();
        }
        long t2 = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            kept[i].getAllStyles().setPadding(0, 0, 0, 0);
        }
        long t3 = System.currentTimeMillis();
        System.out.println("BENCH:PROBE " + n + " labels: construct=" + (t1 - t0)
                + "ms resolveStyles=" + (t2 - t1) + "ms mutateStyles=" + (t3 - t2) + "ms");
        System.out.flush();
    }

    /**
     * Times the FIRST paint of the root container and reports it once.
     *
     * <p>Building and laying out the tree is only half of a first frame; the
     * other half is rasterising it, and that half is invisible to every counter
     * that stops when {@code show()} returns. Without this the gap between
     * "the app finished building" and "the marker printed" is unattributed
     * time, which is where wrong explanations come from.
     */
    private static class TimedRootContainer extends Container {
        private final boolean timed;
        private boolean painted;

        TimedRootContainer(com.codename1.ui.layouts.Layout layout) {
            this(layout, true);
        }

        TimedRootContainer(com.codename1.ui.layouts.Layout layout, boolean timed) {
            super(layout);
            this.timed = timed;
        }

        @Override
        public void paint(com.codename1.ui.Graphics g) {
            if (painted || !timed) {
                super.paint(g);
                return;
            }
            long t0 = System.currentTimeMillis();
            try {
                super.paint(g);
            } finally {
                painted = true;
                System.out.println("BENCH:STARTUP firstPaint="
                        + (System.currentTimeMillis() - t0) + "ms");
                System.out.flush();
            }
        }
    }

    /** {@code cn1.flutter.startupTrace} — prints the first-frame phase split. */
    private static boolean startupTrace() {
        try {
            return "true".equals(com.codename1.ui.Display.getInstance()
                    .getProperty("cn1.flutter.startupTrace", "false"));
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Inflates {@code root} in a fresh CN1 Form (the runApp mounting pattern,
     * minus theme installation and showing). Used by runApp and by
     * Navigator.push for every pushed route; returns the host, from which the
     * Form ({@code host.form()}) and root element ({@code host.rootElement()})
     * are reachable.
     */
    public static RenderHost mountInNewForm(Widget root) {
        return mountInNewForm(root, null);
    }

    /**
     * As {@link #mountInNewForm(Widget)}, but the new tree's ancestor lookups
     * continue from {@code contextFallback} once its own root is reached —
     * how a pushed route inherits the app's Theme, Localizations and providers
     * despite living in its own Form. See {@code Element.contextFallback}.
     */
    public static RenderHost mountInNewForm(Widget root, Element contextFallback) {
        assertEdt();
        // Every route lives inside an Overlay, as it does in Flutter, so that
        // anything reaching for `Overlay.of(context)` — a dialog, a modal
        // sheet, a coach mark — finds the one belonging to the route it is on
        // rather than nothing at all.
        root = com.codename1.flutter.widgets.Overlay.hosting(root);
        Form f = new Form(new BorderLayout());
        // Flutter owns the whole canvas: the widget tree draws its own padding
        // and safe areas, so any CN1 chrome inset on the Form or its content
        // pane is a margin Flutter never asked for (it left pushed pages
        // floating inside a frame).
        stripChrome(f);
        stripChrome(f.getContentPane());
        RenderHost host = new RenderHost();
        host.form(f);
        Container c = startupTrace() ? new TimedRootContainer(new FlutterRootLayout(host))
                : new Container(new FlutterRootLayout(host));
        host.container(c);
        long mt0 = System.currentTimeMillis();
        Element mounted = mount(root, host, new BuildOwner(), contextFallback);
        if (startupTrace()) {
            System.out.println("BENCH:STARTUP   build=" + (System.currentTimeMillis() - mt0) + "ms");
            System.out.flush();
        }
        // Kept on the Form rather than in a static: the Form owns its tree, so a popped
        // route's element cannot outlive it here and currentContext() always answers for
        // whatever is actually showing.
        f.putClientProperty(ROOT_ELEMENT, mounted);
        f.add(BorderLayout.CENTER, c);
        hideUnusedToolbar(f, host);
        return host;
    }

    /**
     * Hides the Form's Toolbar unless a root Scaffold claimed it for its AppBar.
     *
     * <p>Every CN1 Form has a Toolbar, and an empty one is invisible on a display with
     * no cutout -- which is every desktop, which is why this cost nothing in any sweep.
     * On a phone it takes the status-bar inset, so an empty Toolbar paints a band of
     * the FORM's own colour across the top and pushes the content pane down under it.
     * On iOS that was 222px of the Material baseline surface above a page whose own
     * background is the theme's, where the reference simply carries on behind the
     * status bar.</p>
     *
     * <p>The rule is the one {@code Navigator.push} already applied to pushed routes;
     * it belongs here so that the app's first screen -- which is mounted by
     * {@code runApp} and never went through the navigator -- obeys it too.</p>
     */
    private static void hideUnusedToolbar(Form f, RenderHost host) {
        com.codename1.ui.Toolbar tb = f.getToolbar();
        if (tb != null && !host.isFormToolbarBound()) {
            tb.setVisible(false);
            tb.setHidden(true);
        }
    }

    private static final String ROOT_ELEMENT = "cn1$flutterRootElement";

    /**
     * A BuildContext for the tree currently on screen, or null when the current Form
     * is not a Flutter one.
     *
     * <p>Exists because {@code Navigator.pushNamed} needs a context to inherit from: a
     * route pushed with a null context gets no ancestor chain, so {@code Theme.of},
     * {@code MediaQuery.of}, {@code Localizations.of} and every provider above it find
     * nothing — the pushed page then throws on the first thing it looks up. Widgets
     * always have their own context and should pass it; this is for callers OUTSIDE the
     * tree — a deep link, a notification tap, a test harness — which have none of their
     * own and would otherwise pass null.</p>
     */
    public static BuildContext currentContext() {
        if (!Display.isInitialized()) {
            return null;
        }
        Form f = Display.getInstance().getCurrent();
        Object e = f == null ? null : f.getClientProperty(ROOT_ELEMENT);
        return e instanceof Element && ((Element) e).mounted ? (Element) e : null;
    }

    /**
     * Removes a component's theme-supplied padding and margin. The units are
     * set to pixels first: styles derived from the Material theme carry
     * MILLIMETRE units, under which a zero is still zero but any later
     * non-zero write would be reinterpreted at ~18x.
     */
    private static void stripChrome(com.codename1.ui.Component c) {
        if (c == null) {
            return;
        }
        com.codename1.ui.plaf.Style s = c.getAllStyles();
        s.setPaddingUnit(com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS,
                com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS,
                com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS,
                com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS);
        s.setMarginUnit(com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS,
                com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS,
                com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS,
                com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS);
        s.setPadding(0, 0, 0, 0);
        s.setMargin(0, 0, 0, 0);
        if (c instanceof com.codename1.ui.Container) {
            // The safe area is a LAYOUT inset, not padding, so zeroing the style
            // above does not touch it. Codename One holds a full-screen form's
            // content off the display cutout by itself; Flutter's tree does that
            // for itself through MediaQuery, so leaving the flag on insets the
            // canvas twice and leaves a band of the FORM's own colour above
            // everything the app drew -- a white strip across the top of the
            // gallery on iOS, where the reference shows the page carrying on
            // behind the status bar. It costs nothing on a port with no cutout,
            // which is why it survived every desktop sweep.
            ((com.codename1.ui.Container) c).setSafeArea(false);
        }
    }

    /**
     * Unmounts a whole element subtree (recursively). Used by
     * Navigator.pop/Dialogs when a route or dialog is torn down.
     */
    public static void unmountTree(Element root) {
        if (root != null) {
            root.unmountRecursively();
        }
    }

    /**
     * Installs the bundled Material 3 theme (AndroidMaterialTheme) as the
     * base look for a Flutter-owned app. Flutter renders Material Design on
     * every platform, so a full-app Flutter boot replaces the platform
     * default theme; embedded subtrees (wrap()) deliberately do NOT install
     * it to avoid restyling the host app.
     */
    private static void installMaterialBaseTheme() {
        try {
            com.codename1.ui.util.Resources r = com.codename1.ui.util.Resources.open(
                    "/CN1FlutterMaterialTheme.res");
            String[] names = r.getThemeResourceNames();
            if (names.length > 0) {
                com.codename1.ui.plaf.UIManager.getInstance().setThemeProps(r.getTheme(names[0]));
            }
        } catch (Throwable t) {
            com.codename1.io.Log.p("Flutter runtime: could not install Material base theme: " + t);
        }
        installFlutterUiidDerives();
        installFlutterScrollPhysics();
    }

    /**
     * Matches Codename One's fling to Flutter's scroll physics.
     *
     * <p>The CURVE already agrees: CN1's exponential decay uses a 500ms time constant, and
     * Flutter's iOS {@code FrictionSimulation} (drag 0.135) e-folds at 1/-ln(0.135) =
     * 499ms. Only the distance differs. CN1 coasts to
     * {@code release velocity * DecayMotionScaleFactorInt}, 950 by default, while Flutter
     * travels {@code -v/ln(0.135) = 0.4994 * v} — so an identical flick carries 1.90x too
     * far, which reads as the list being slippery and overshooting where you meant to
     * stop.</p>
     *
     * <p>500 makes the two simulations agree to three decimal places rather than being a
     * number tuned by eye.</p>
     *
     * <p>The OVERSCROLL agrees by construction too, once the coefficient matches. Codename
     * One compresses an over-edge drag with {@code c*x*D/(c*x + D)} for finger distance
     * {@code x} and viewport {@code D}. Flutter looks nothing like that at first glance -
     * {@link com.codename1.flutter.widgets.BouncingScrollPhysics} damps each individual
     * drag delta by {@code 0.52*(1 - overscroll/D)^2} - but integrating that friction over
     * a continuous drag gives {@code 0.52*x*D/(0.52*x + D)}: the SAME curve, and the only
     * difference is the coefficient. Codename One uses 0.55, which is UIScrollView's;
     * Flutter uses 0.52.
     *
     * <p>So this is one constant rather than a reimplementation, and
     * {@code RubberBandParityTest} checks the two really do agree rather than taking the
     * derivation's word for it.</p>
     *
     * <p>These are app-level theme constants, so they apply to the whole app rather than
     * only to Flutter subtrees. That is right for {@code runApp}, which owns the app; a
     * host app embedding Flutter through {@code wrap} can set them back afterwards.</p>
     */
    /**
     * The theme constants that make Codename One's scrolling behave like Flutter's.
     *
     * <p>Package-private and separate from the install so a test can check the KEYS, which
     * is not a formality: a theme constant is only a constant if its key carries the
     * {@code @}, because {@code buildTheme} strips that prefix to decide what is a constant
     * and what is an ordinary style property. Without it the entries are stored happily,
     * {@code addThemeProps} reports nothing wrong, and {@code getThemeConstant} keeps
     * returning the default — which is exactly what happened here: the fling-distance
     * constant looked installed for a long time and never once took effect.</p>
     */
    static java.util.Hashtable<String, Object> scrollPhysicsProps() {
        java.util.Hashtable<String, Object> physics = new java.util.Hashtable<String, Object>();
        physics.put("@DecayMotionScaleFactorInt", "500");
        // Hundredths: 52 = 0.52, Flutter's BouncingScrollPhysics friction factor.
        physics.put("@rubberBandCoefficientInt", "52");
        // The Material 3 slider painter is opt-in: without these the slider
        // falls back to a legacy full-height fill with no thumb, which is what
        // made the sliders demo a row of flat lavender bars. Sizes are
        // Material's, converted from logical pixels at 160dpi: a 4dp track and
        // a 20dp round thumb. The track runs CONTINUOUSLY under the thumb, as
        // Flutter's slider draws it, rather than M3's gapped two-segment track.
        physics.put("@sliderTrackThicknessMM", "0.635");
        physics.put("@sliderThumbWidthMM", "3.175");
        physics.put("@sliderThumbHeightMM", "3.175");
        physics.put("@sliderContinuousTrackBool", "true");
        return physics;
    }

    private static void installFlutterScrollPhysics() {
        try {
            com.codename1.ui.plaf.UIManager.getInstance().addThemeProps(scrollPhysicsProps());
        } catch (Throwable t) {
            com.codename1.io.Log.p("Flutter runtime: could not install scroll physics: " + t);
        }
    }

    /**
     * The Flutter* UIIDs have no entries in the theme, so without these
     * derive mappings every Flutter component falls back to the tiny default
     * system font instead of the Material theme's fonts. Geometry (padding,
     * margins) is still zeroed programmatically by the render elements —
     * only fonts and colors flow in from the derived UIIDs.
     */
    private static void installFlutterUiidDerives() {
        try {
            java.util.Hashtable<String, Object> derives = new java.util.Hashtable<String, Object>();
            String[][] map = {
                    {"FlutterText", "Label"},
                    {"FlutterIcon", "Label"},
                    {"FlutterImage", "Label"},
                    {"FlutterDivider", "Label"},
                    {"FlutterElevatedButton", "Button"},
                    {"FlutterTextButton", "Button"},
                    {"FlutterOutlinedButton", "Button"},
                    {"FlutterIconButton", "Button"},
                    {"FlutterCard", "Container"},
                    {"FlutterScroll", "Container"},
                    {"FlutterGesture", "Container"},
                    {"FlutterAppBar", "TitleArea"},
                    {"FlutterTextField", "TextField"},
                    {"FlutterCheckbox", "CheckBox"},
                    {"FlutterSwitch", "Switch"},
                    {"FlutterRadio", "RadioButton"},
                    {"FlutterSlider", "Slider"},
                    {"FlutterListTile", "Container"},
                    {"FlutterBottomNavigationBar", "Container"},
                    {"FlutterDrawer", "Container"},
            };
            for (String[] m : map) {
                derives.put(m[0] + ".derive", m[1]);
            }
            // Every Flutter UIID gets a zero margin, in all four states.
            //
            // These UIIDs derive from Codename One base UIIDs, so they inherit a
            // margin meant for Codename One layouts -- Container's 2px, Switch's
            // 10/15 -- which Flutter geometry must not have: the widget tree
            // decides its own spacing. RenderElement.neutralizeCn1Behaviors was
            // already forcing it to zero, but per COMPONENT, and getAllStyles()
            // creates the selected, pressed and disabled styles plus a proxy to
            // do it: five Style objects each, ~1900 during the first frame of
            // the gallery, on the one primitive ParparVM is slowest at.
            //
            // Declaring it in the theme instead makes the components arrive
            // already correct, so the per-component undo can be skipped
            // entirely. Same rendered result -- the runtime set these to zero
            // anyway -- for none of the allocation.
            String[] uiids = {
                    "FlutterText", "FlutterIcon", "FlutterImage", "FlutterDivider",
                    "FlutterElevatedButton", "FlutterTextButton", "FlutterOutlinedButton",
                    "FlutterIconButton", "FlutterCard", "FlutterScroll", "FlutterGesture",
                    "FlutterAppBar", "FlutterTextField", "FlutterCheckbox", "FlutterSwitch",
                    "FlutterRadio", "FlutterSlider", "FlutterListTile",
                    "FlutterBottomNavigationBar", "FlutterDrawer",
                    // Not derived above -- they take the theme's default UIID, which
                    // is where Container's 2px margin comes from.
                    "FlutterBox", "FlutterEffect", "FlutterScaffold", "FlutterCustomPaint",
            };
            for (String u : uiids) {
                derives.put(u + ".margin", "0,0,0,0");
                derives.put(u + ".sel#margin", "0,0,0,0");
                derives.put(u + ".press#margin", "0,0,0,0");
                derives.put(u + ".dis#margin", "0,0,0,0");
            }
            com.codename1.ui.plaf.UIManager.getInstance().addThemeProps(derives);
        } catch (Throwable t) {
            com.codename1.io.Log.p("Flutter runtime: could not install UIID derives: " + t);
        }
    }

    /**
     * Inflates {@code w} into a CN1 Container that can be embedded anywhere
     * in a regular CN1 component hierarchy.
     */
    public static Container wrap(Widget w) {
        RenderHost host = new RenderHost();
        EmbeddedRoot c = new EmbeddedRoot(new FlutterRootLayout(host), startupTrace(), w, host);
        host.container(c);
        c.mountTree();
        return c;
    }

    /**
     * The container {@link #wrap} returns, which owns the lifetime of the tree
     * mounted into it.
     *
     * <p>Removing a wrapped subtree must unmount it: that is what disposes each
     * State, detaches the notifiers it listened to and stops its animations.
     * {@code wrap} used to hand back a plain Container and drop the mounted
     * root, so a host that swapped embedded Flutter screens in and out kept every
     * old tree's listeners and frame-driver registrations alive after its UI was
     * gone.</p>
     *
     * <p>The catch is that Codename One calls {@code deinitialize} for two
     * different things: the component being REMOVED, and its form merely being
     * hidden -- navigating to another form and back. Unmounting on the second
     * would throw away the embedded state on an ordinary back-navigation, which
     * Flutter keeps. The two are told apart after the fact: removal clears the
     * parent once {@code deinitialize} returns, while a hidden form's components
     * keep theirs. So the check is deferred one EDT cycle, which also treats a
     * component moved between parents in the same cycle as kept.</p>
     *
     * <p>A container added again after it was unmounted mounts a fresh tree from
     * the same widget, as re-inserting a widget does in Flutter. What this cannot
     * see is a whole Form abandoned without its components ever being removed;
     * Codename One has no signal for that, and such a form becomes garbage along
     * with the tree it holds.</p>
     */
    private static final class EmbeddedRoot extends TimedRootContainer {
        private final Widget widget;
        private final RenderHost host;
        private final EmbeddedLifetime lifetime = new EmbeddedLifetime();

        EmbeddedRoot(com.codename1.ui.layouts.Layout layout, boolean timed, Widget widget, RenderHost host) {
            super(layout, timed);
            this.widget = widget;
            this.host = host;
        }

        void mountTree() {
            mount(widget, host, new BuildOwner());
            lifetime.mounted();
        }

        @Override
        protected void initComponent() {
            super.initComponent();
            if (lifetime.onInit()) {
                removeAll();
                mountTree();
            }
        }

        @Override
        protected void deinitialize() {
            super.deinitialize();
            if (lifetime.onDeinit()) {
                com.codename1.ui.CN.callSerially(new Runnable() {
                    @Override
                    public void run() {
                        if (lifetime.settle(getParent() != null)) {
                            unmountTree(host.rootElement());
                        }
                    }
                });
            }
        }
    }

    /**
     * The removed-or-merely-hidden decision {@link EmbeddedRoot} makes, kept free
     * of components so it can be tested without a display. Each method answers
     * what the caller must do next.
     */
    static final class EmbeddedLifetime {
        private boolean mounted;
        private boolean unmountPending;

        void mounted() {
            mounted = true;
        }

        /** Initialized (shown, or added back). @return true to mount a fresh tree. */
        boolean onInit() {
            // Coming back cancels a pending unmount: this was a hide, or a move.
            unmountPending = false;
            return !mounted;
        }

        /** Deinitialized. @return true to schedule {@link #settle} one EDT cycle later. */
        boolean onDeinit() {
            if (!mounted || unmountPending) {
                return false;
            }
            unmountPending = true;
            return true;
        }

        /**
         * One cycle after a deinitialize. @param attached whether the container
         * still has a parent @return true to unmount the tree now.
         */
        boolean settle(boolean attached) {
            if (!unmountPending) {
                return false;
            }
            unmountPending = false;
            if (attached || !mounted) {
                return false;
            }
            mounted = false;
            return true;
        }
    }

    /**
     * Low-level mount used by {@link #runApp}/{@link #wrap} and by unit
     * tests (with a componentless RenderHost).
     */
    public static Element mount(Widget root, RenderHost host, BuildOwner owner) {
        return mount(root, host, owner, null);
    }

    /**
     * As {@link #mount(Widget, RenderHost, BuildOwner)}, with an ancestor-lookup
     * continuation for the new root. It must be linked before the mount, since
     * the first build runs there and may already do a {@code Foo.of(context)}.
     */
    public static Element mount(Widget root, RenderHost host, BuildOwner owner, Element contextFallback) {
        assertEdt();
        Element rootElement = root.createElement();
        host.rootElement(rootElement);
        rootElement.bootstrap(owner, host);
        if (contextFallback != null) {
            rootElement.contextFallback(contextFallback);
        }
        rootElement.mount(null, 0);
        return rootElement;
    }

    /**
     * Framework mutations must happen on the EDT — but only when a Display
     * exists; headless unit tests run without one.
     */
    static void assertEdt() {
        if (Display.isInitialized() && !CN.isEdt()) {
            throw new IllegalStateException("Flutter framework mutation off the EDT; use CN.callSerially");
        }
    }
}
