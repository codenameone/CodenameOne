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
        installMaterialBaseTheme();
        mountInNewForm(app).form().show();
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
        Form f = new Form(new BorderLayout());
        // Flutter owns the whole canvas: the widget tree draws its own padding
        // and safe areas, so any CN1 chrome inset on the Form or its content
        // pane is a margin Flutter never asked for (it left pushed pages
        // floating inside a frame).
        stripChrome(f);
        stripChrome(f.getContentPane());
        RenderHost host = new RenderHost();
        host.form(f);
        Container c = new Container(new FlutterRootLayout(host));
        host.container(c);
        Element mounted = mount(root, host, new BuildOwner(), contextFallback);
        // Kept on the Form rather than in a static: the Form owns its tree, so a popped
        // route's element cannot outlive it here and currentContext() always answers for
        // whatever is actually showing.
        f.putClientProperty(ROOT_ELEMENT, mounted);
        f.add(BorderLayout.CENTER, c);
        return host;
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
        Container c = new Container(new FlutterRootLayout(host));
        host.container(c);
        mount(w, host, new BuildOwner());
        return c;
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
