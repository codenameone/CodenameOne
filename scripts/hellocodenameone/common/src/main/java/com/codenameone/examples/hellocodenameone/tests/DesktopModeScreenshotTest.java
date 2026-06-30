package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.CN;
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

import java.util.Hashtable;

/// Shows that the desktop integration features are inert on the phone/tablet ports but reshape the
/// UI on the Mac native (Catalyst) desktop build, where {@code CN.isDesktop()} is true.
///
/// The exact same code runs on every port:
///
/// * On Android / iOS / JavaScript / the JavaSE simulator ({@code CN.isDesktop() == false}) it
///   renders an ordinary mobile screen - a CN1 {@code Toolbar} with a hamburger side-menu button
///   and the usual fading touch scrollbar, which has settled to invisible by the time the
///   screenshot is taken. So the desktop features have no visible impact there.
/// * On the Mac native build ({@code CN.isDesktop() == true}) the test opts into desktop mode
///   ({@code desktop.titleBar=native} plus interactive scrollbars). The screenshot then looks
///   different: the in-app Toolbar and its hamburger are gone (the commands move to the native
///   macOS menu bar, which isn't part of the form raster), and the scrollbar shows an
///   always-visible, draggable thumb that the mobile ports never display.
///
/// The command keyboard accelerators are exercised on the desktop too (they become Mac
/// {@code UIKeyCommand}s), though a still screenshot can't show them.
///
/// The desktop-mode toggles are global, so the test reverts them in {@link #done()} - which runs
/// only after the screenshot has been captured - keeping every other test's baseline (on every
/// port) untouched.
public class DesktopModeScreenshotTest extends BaseTest {
    private boolean desktopEnabled;

    @Override
    public boolean runTest() throws Exception {
        if (CN.isDesktop()) {
            desktopEnabled = true;
            // Read live by the toolbar at show time; hides the Toolbar and bridges commands to the
            // native menu bar. Inert on the mobile ports (gated on isDesktop()).
            Display.getInstance().setProperty("desktop.titleBar", "native");
            // Turn on the always-visible interactive scrollbar (the macOS-style thumb). Injected
            // directly here (rather than via the isDesktop-gated port hook) so it only happens on
            // the desktop branch; reverted in done().
            Hashtable interactive = new Hashtable();
            interactive.put("@interactiveScrollBool", "true");
            UIManager.getInstance().addThemeProps(interactive);
        }

        Form form = createForm("Desktop Mode", new BorderLayout(), "DesktopMode");
        Toolbar toolbar = new Toolbar();
        form.setToolbar(toolbar);
        form.setTitle("Desktop Mode");

        // Commands surface as a hamburger side menu on mobile and as native macOS menu items on the
        // desktop. The shortcuts (Cmd+S / Cmd+R) only take effect in the desktop native menu.
        Command save = new Command("Save");
        save.setDesktopMenu(Command.DESKTOP_MENU_FILE);
        save.setDesktopShortcut('S');
        Command refresh = new Command("Refresh");
        refresh.setDesktopMenu(Command.DESKTOP_MENU_VIEW);
        refresh.setDesktopShortcut('R');
        Command about = new Command("About");
        about.setDesktopMenu(Command.DESKTOP_MENU_ABOUT);
        if (CN.isDesktop()) {
            // In desktop "native" mode the Toolbar is hidden and its side menu is never
            // constructed, so addCommandToSideMenu would NPE. Form.addCommand still registers the
            // commands, which the framework harvests into the native macOS menu bar.
            form.addCommand(save);
            form.addCommand(refresh);
            form.addCommand(about);
        } else {
            // Mobile: a hamburger side menu the desktop build never shows.
            toolbar.addCommandToSideMenu(save);
            toolbar.addCommandToSideMenu(refresh);
            toolbar.addCommandToSideMenu(about);
        }

        // A tall, overflowing list so the scrollbar is meaningful: an always-visible interactive
        // thumb on the desktop versus a faded (invisible) touch scrollbar on mobile.
        Container list = new Container(BoxLayout.y());
        list.setScrollableY(true);
        Style listStyle = list.getAllStyles();
        listStyle.setBgColor(0xfafafa);
        listStyle.setBgTransparency(255);
        listStyle.setPadding(4, 4, 4, 4);
        for (int i = 0; i < 30; i++) {
            Label row = new Label("Row " + (i + 1));
            Style rowStyle = row.getAllStyles();
            rowStyle.setBgColor(rowColor(i));
            rowStyle.setFgColor(0xffffff);
            rowStyle.setBgTransparency(255);
            rowStyle.setMargin(2, 2, 2, 2);
            rowStyle.setPadding(12, 12, 12, 12);
            list.add(row);
        }
        form.add(BorderLayout.CENTER, list);
        form.show();
        return true;
    }

    /// The 30-row scrollable form is heavy enough that on the iOS Metal backend
    /// its first frame is occasionally presented just after the capture fires,
    /// so Display.screenshot() reads the previous test's framebuffer and this
    /// test "captures the wrong form". Force a repaint and an extra settle so the
    /// DesktopMode form is fully presented before the screenshot. 700ms still lost
    /// the race on a starved CI runner (the capture showed the preceding
    /// MutableImageClip form), so use the same 1500ms margin the OrientationLock
    /// test relies on for its post-rotation present.
    @Override
    protected long extraSettleBeforeCaptureMillis() {
        return 1500;
    }

    @Override
    protected synchronized void done() {
        // Revert the global desktop-mode toggles now that the screenshot has been captured, so the
        // rest of the suite (and every other port's baseline) is unaffected by this test.
        if (desktopEnabled) {
            desktopEnabled = false;
            try {
                Display.getInstance().setProperty("desktop.titleBar", "toolbar");
                Hashtable revert = new Hashtable();
                revert.put("@interactiveScrollBool", "false");
                UIManager.getInstance().addThemeProps(revert);
            } catch (Throwable ignored) {
                // best-effort restore; never let teardown fail the test
            }
        }
        super.done();
    }

    private static int rowColor(int i) {
        int[] palette = {0x118ab2, 0x06d6a0, 0xffd166, 0xef476f, 0x8338ec, 0x073b4c};
        return palette[i % palette.length];
    }
}
