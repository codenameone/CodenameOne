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
        assertEdt();
        Form f = new Form(new BorderLayout());
        RenderHost host = new RenderHost();
        host.form(f);
        Container c = new Container(new FlutterRootLayout(host));
        host.container(c);
        mount(root, host, new BuildOwner());
        f.add(BorderLayout.CENTER, c);
        return host;
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
        assertEdt();
        Element rootElement = root.createElement();
        host.rootElement(rootElement);
        rootElement.bootstrap(owner, host);
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
