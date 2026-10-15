/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;

import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the desktop window-chrome behavior added to {@link Form}/{@link Toolbar}: the
 * {@code native} mode detaches the CN1 Toolbar (no strip painted) and bridges its commands to the
 * native menu bar; the {@code custom} mode keeps the Toolbar attached so it acts as the window's
 * title bar (undecorated window) while still bridging the commands to the native menu bar. The
 * legacy {@code toolbar} mode and mobile remain unchanged.
 */
class DesktopChromeTest extends UITestBase {

    @FormTest
    void customChromePublishesOriginalOnTopSideMenuCommandsAndLaterChanges() {
        boolean onTop = Toolbar.isOnTopSideMenu();
        try {
            Toolbar.setOnTopSideMenu(true);
            desktopMode("custom");
            Form f = new Form("Document");
            Command save = new Command("Save");
            save.setDesktopMenu("File");
            f.getToolbar().addCommandToSideMenu(save);
            f.show();
            DisplayTest.flushEdt();
            assertTrue(implementation.getLastNativeCommands().contains(save));
            Command later = new Command("Export");
            f.getToolbar().addCommandToSideMenu(later);
            assertTrue(implementation.getLastNativeCommands().contains(later));
            f.getToolbar().removeCommand(later);
            assertFalse(implementation.getLastNativeCommands().contains(later));
        } finally {
            Toolbar.setOnTopSideMenu(onTop);
        }
    }

    @FormTest
    void customTitleBarExcludesButtonsAndPointerListenersFromWindowDrags() {
        desktopMode("custom");
        Form f = new Form("Document");
        Toolbar toolbar = f.getToolbar();
        Button save = new Button("Save");
        save.setEnabled(false);
        Label rename = new Label("Rename");
        rename.addPointerReleasedListener(e -> { });
        Label title = new Label("Document");
        Container content = BoxLayout.encloseX(save, rename, title);
        toolbar.setTitleComponent(content);
        f.show();
        DisplayTest.flushEdt();
        assertFalse(toolbar.isTitleBarDragPoint(save.getAbsoluteX() + 1, save.getAbsoluteY() + 1),
                "even a disabled button must not initiate a window drag");
        assertFalse(toolbar.isTitleBarDragPoint(rename.getAbsoluteX() + 1, rename.getAbsoluteY() + 1),
                "custom pointer controls keep their gestures");
        assertTrue(toolbar.isTitleBarDragPoint(title.getAbsoluteX() + 1, title.getAbsoluteY() + 1),
                "an ordinary title label remains a native drag region");
        assertFalse(toolbar.isTitleBarDragPoint(toolbar.getAbsoluteX() + 1,
                toolbar.getAbsoluteY() + toolbar.getHeight()), "the document below is not a title bar");
    }

    @FormTest
    void customPointerHandlersAndLongPressListenersAreNotWindowDragRegions() {
        desktopMode("custom");
        Form form = new Form("Custom controls");
        Label custom = new Label("Custom click") {
            @Override public void pointerReleased(int x, int y) { }
        };
        Label longPress = new Label("Long press");
        longPress.addLongPressListener(e -> { });
        Label ordinary = new Label("Plain title");
        form.getToolbar().setTitleComponent(BoxLayout.encloseX(custom, longPress, ordinary));
        form.show();
        DisplayTest.flushEdt();
        assertFalse(form.getToolbar().isTitleBarDragPoint(custom.getAbsoluteX() + 1,
                custom.getAbsoluteY() + 1), "an overridden pointer handler must keep its press");
        assertFalse(form.getToolbar().isTitleBarDragPoint(longPress.getAbsoluteX() + 1,
                longPress.getAbsoluteY() + 1), "a long-press listener must keep its press");
        assertTrue(form.getToolbar().isTitleBarDragPoint(ordinary.getAbsoluteX() + 1,
                ordinary.getAbsoluteY() + 1));
    }

    @FormTest
    void passiveCustomTitleBackgroundCanOptInWithoutTakingChildInput() {
        desktopMode("custom");
        Form form = new Form("Passive custom title");
        Container custom = new Container(BoxLayout.x()) { };
        Label title = new Label("Title");
        Button button = new Button("Save");
        custom.add(title).add(button);
        Toolbar.setTitleBarDragRegion(custom, true);
        form.getToolbar().setTitleComponent(custom);
        form.show();
        DisplayTest.flushEdt();
        assertTrue(form.getToolbar().isTitleBarDragPoint(title.getAbsoluteX() + 1, title.getAbsoluteY() + 1));
        assertFalse(form.getToolbar().isTitleBarDragPoint(button.getAbsoluteX() + 1, button.getAbsoluteY() + 1));
        Toolbar.setTitleBarDragRegion(title, false);
        assertFalse(form.getToolbar().isTitleBarDragPoint(title.getAbsoluteX() + 1, title.getAbsoluteY() + 1));
        Toolbar.setTitleBarDragRegion(title, true);
        title.addLongPressListener(e -> { });
        assertFalse(form.getToolbar().isTitleBarDragPoint(title.getAbsoluteX() + 1, title.getAbsoluteY() + 1));
    }

    @FormTest
    void customTitleRegionsFollowResizeAndClearWhenToolbarIsRemoved() {
        desktopMode("custom");
        Form f = new Form("Document");
        f.show();
        DisplayTest.flushEdt();
        Toolbar toolbar = f.getToolbar();
        Graphics graphics = Image.createImage(800, 800).getGraphics();
        toolbar.paint(graphics);
        int width = toolbar.getWidth();
        assertEquals(width, implementation.getLastTitleBarRegions()[2]);
        toolbar.setWidth(width + 100);
        toolbar.paint(graphics);
        assertEquals(width + 100, implementation.getLastTitleBarRegions()[2]);
        toolbar.deinitialize();
        assertEquals(0, implementation.getLastTitleBarRegions().length,
                "a removed title bar must not leave invisible window drag handles");
    }

    @FormTest
    void hidingToolbarImmediatelyClearsNativeAndFallbackDragRegions() {
        desktopMode("custom");
        Form form = new Form("Document");
        form.show();
        DisplayTest.flushEdt();
        Toolbar toolbar = form.getToolbar();
        Graphics graphics = Image.createImage(800, 800).getGraphics();
        toolbar.paint(graphics);
        assertTrue(implementation.getLastTitleBarRegions().length > 0);
        toolbar.setVisible(false);
        assertEquals(0, implementation.getLastTitleBarRegions().length,
                "hiding must clear regions before any subsequent paint");
        assertFalse(toolbar.isTitleBarDragPoint(toolbar.getAbsoluteX() + 1, toolbar.getAbsoluteY() + 1));
        toolbar.paint(graphics); // a manual paint must not republish a hidden band
        assertEquals(0, implementation.getLastTitleBarRegions().length);
        toolbar.setVisible(true);
        toolbar.paint(graphics);
        assertTrue(implementation.getLastTitleBarRegions().length > 0);
    }

    @FormTest
    void hidingTitleToolbarBeforeFirstPaintClearsNativeChrome() {
        desktopMode("custom");
        implementation.setNativeTitleBarInsets(70, 130, 6, 4, 50);
        Form form = new Form("Not painted yet");
        form.show();
        Toolbar toolbar = form.getToolbar();
        toolbar.calcPreferredSize(); // native ports enable integrated chrome here
        assertNull(implementation.getLastTitleBarRegions(), "no paint regions published yet");
        toolbar.setVisible(false);
        assertNotNull(implementation.getLastTitleBarRegions(), "hide must send a native clear");
        assertEquals(0, implementation.getLastTitleBarRegions().length);
        toolbar.setVisible(true);
        toolbar.paint(Image.createImage(800, 800).getGraphics());
        assertTrue(implementation.getLastTitleBarRegions().length > 0,
                "showing again must restore native drag regions");
    }

    @FormTest
    void removingTitleToolbarBeforeFirstPaintClearsNativeChrome() {
        desktopMode("custom");
        implementation.setNativeTitleBarInsets(70, 130, 6, 4, 50);
        Form form = new Form("Removed before paint");
        form.show();
        Toolbar toolbar = form.getToolbar();
        toolbar.doLayout(); // layout also queries the native insets
        assertNull(implementation.getLastTitleBarRegions(), "no paint regions published yet");
        toolbar.remove();
        assertNotNull(implementation.getLastTitleBarRegions(), "removal must send a native clear");
        assertEquals(0, implementation.getLastTitleBarRegions().length);
    }

    @FormTest
    void nativeControlsReservePhysicalSpaceWithoutChangingApplicationStyle() {
        desktopMode("custom");
        implementation.setNativeTitleBarInsets(70, 130, 6, 4, 50);
        Form form = new Form("Document");
        Toolbar toolbar = form.getToolbar();
        toolbar.getAllStyles().setPaddingUnit(com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS);
        toolbar.getAllStyles().setPadding(2, 3, 5, 7);
        toolbar.setTitleCentered(false);
        Container content = new Container(new com.codename1.ui.layouts.BorderLayout());
        Button left = new Button("Save");
        Button right = new Button("Search");
        content.add(com.codename1.ui.layouts.BorderLayout.WEST, left);
        content.add(com.codename1.ui.layouts.BorderLayout.EAST, right);
        toolbar.setTitleComponent(content);
        form.show();
        DisplayTest.flushEdt();
        assertTrue(toolbar.getPreferredH() >= 50);
        assertTrue(content.getX() >= 75);
        assertTrue(content.getX() + content.getWidth() <= toolbar.getWidth() - 137);
        assertEquals(5, toolbar.getStyle().getPaddingLeftNoRTL());
        assertEquals(7, toolbar.getStyle().getPaddingRightNoRTL());
        assertEquals(2, toolbar.getStyle().getPaddingTop());
        assertEquals(3, toolbar.getStyle().getPaddingBottom());
        int x = content.getX();
        toolbar.layoutContainer();
        assertEquals(x, content.getX(), "repeated layouts must not accumulate native insets");
        implementation.setNativeTitleBarInsets(0, 90, 0, 0, 35);
        toolbar.setShouldCalcPreferredSize(true);
        toolbar.layoutContainer();
        assertTrue(content.getX() < x, "changed native geometry releases the old reserved space");
        assertTrue(content.getX() + content.getWidth() <= toolbar.getWidth() - 97);
        toolbar.setRTL(true);
        toolbar.layoutContainer();
        assertTrue(content.getX() + content.getWidth() <= toolbar.getWidth() - 90,
                "OS controls stay on their physical side in an RTL document");
    }

    @FormTest
    void nativeTitleInsetsPreserveMixedPaddingUnitsAcrossMeasurementsAndLayouts() {
        desktopMode("custom");
        implementation.setNativeTitleBarInsets(70, 130, 6, 4, 50);
        Form form = new Form("Mixed units");
        Toolbar toolbar = form.getToolbar();
        form.show();
        DisplayTest.flushEdt();
        Style style = toolbar.getStyle();
        byte[] units = {Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_PIXELS,
                Style.UNIT_TYPE_SCREEN_PERCENTAGE, Style.UNIT_TYPE_REM};
        style.setPaddingUnit(units);
        style.setPadding(2, 3, 5, 7);
        for (boolean rtl : new boolean[]{false, true}) {
            toolbar.setRTL(rtl);
            for (int pass = 0; pass < 3; pass++) {
                toolbar.calcPreferredSize();
                assertArrayEquals(units, style.getPaddingUnit(), "measurement must preserve per-side units");
                toolbar.doLayout();
                assertArrayEquals(units, style.getPaddingUnit(), "layout must preserve per-side units");
                assertEquals(2f, style.getPaddingFloatValue(false, Component.TOP));
                assertEquals(3f, style.getPaddingFloatValue(false, Component.BOTTOM));
                assertEquals(5f, style.getPaddingFloatValue(false, Component.LEFT));
                assertEquals(7f, style.getPaddingFloatValue(false, Component.RIGHT));
            }
        }
        style.setPaddingUnit((byte[]) null);
        toolbar.calcPreferredSize();
        toolbar.doLayout();
        assertNull(style.getPaddingUnit(), "implicit pixel units must stay implicit");
    }

    @FormTest
    void mobileAndLegacyToolbarsDoNotReserveDesktopControls() {
        implementation.setNativeTitleBarInsets(200, 200, 50, 50, 300);
        Toolbar.setGlobalToolbar(true);
        implementation.setDesktop(false);
        Form mobile = new Form("Mobile");
        mobile.show();
        DisplayTest.flushEdt();
        assertTrue(mobile.getToolbar().getPreferredH() < 300);
        desktopMode("toolbar");
        Form legacy = new Form("Legacy");
        legacy.show();
        DisplayTest.flushEdt();
        assertTrue(legacy.getToolbar().getPreferredH() < 300);
    }

    private void desktopMode(String titleBarMode) {
        implementation.setDesktop(true);
        implementation.setDesktopTitleBarMode(titleBarMode);
        Toolbar.setGlobalToolbar(true);
    }

    @FormTest
    void nativeModeDetachesToolbarAndBridgesCommands() {
        desktopMode("native");
        Form f = new Form("My App");
        Command save = new Command("Save");
        // addCommand routes into the toolbar's menu bar, which getAllNativeMenuCommands harvests
        f.addCommand(save);
        f.show();
        DisplayTest.flushEdt();

        assertNotNull(f.getToolbar(), "toolbar object must still exist for the command API");
        assertNull(f.getToolbar().getParent(), "in native mode the toolbar must not be attached/painted");
        assertEquals("My App", f.getTitle(), "title is still tracked for the OS window");

        Vector bridged = implementation.getLastNativeCommands();
        assertNotNull(bridged, "commands must be bridged to the native menu bar");
        assertTrue(bridged.contains(save), "the side-menu command must be in the native menu set");
    }

    @FormTest
    void customModeKeepsToolbarAsTitleBarAndBridgesCommands() {
        desktopMode("custom");
        Form f = new Form("Custom");
        Command save = new Command("Save");
        f.addCommand(save);
        f.show();
        DisplayTest.flushEdt();

        // the visible Toolbar IS the window title bar in custom mode, so it stays attached/painted
        assertNotNull(f.getToolbar().getParent(), "in custom mode the toolbar is shown as the title bar");
        assertEquals("Custom", f.getTitle(), "title is tracked and shown by the toolbar title bar");

        Vector bridged = implementation.getLastNativeCommands();
        assertNotNull(bridged, "commands must also be bridged to the native menu bar");
        assertTrue(bridged.contains(save), "the side-menu command must be in the native menu set");
    }

    /**
     * A port whose {@code setNativeCommands} discards them must keep drawing the Toolbar.
     *
     * <p>{@code MenuBar.updateCommands} used to call {@code setNativeCommands} and RETURN
     * whenever the behavior was NATIVE -- drawing no soft buttons, because on a platform with
     * a real menu bar drawing them too would duplicate every command. On a platform without
     * one, that meant the commands went to a method that discards them and were never drawn
     * at all. Silently: nothing in that path can tell "handled natively" from "dropped".</p>
     *
     * <p>Latent until a theme asked for it, which the desktop native themes now do --
     * {@code commandBehavior: Native}, right for the platforms they model and not yet
     * honourable by the Windows and Linux ports.</p>
     */
    @FormTest
    void aPortWithNoNativeMenuBarKeepsTheToolbar() {
        desktopMode("native");
        implementation.setNativeCommandsSupported(false);

        Form f = new Form("No native menu");
        Command save = new Command("Save");
        f.addCommand(save);
        f.show();
        DisplayTest.flushEdt();

        assertNotNull(f.getToolbar().getParent(),
                "hiding the toolbar would take away the only place these commands are drawn");
        assertTrue(f.getToolbar().getAllNativeMenuCommands().contains(save),
                "and the command is still in it");
    }

    @FormTest
    void aPortWithANativeMenuBarStillHidesTheToolbar() {
        // The other direction, so the guard above cannot silently disable native mode
        // everywhere: this is the case DesktopChromeTest's first test already covers, kept
        // beside its opposite.
        desktopMode("native");
        implementation.setNativeCommandsSupported(true);

        Form f = new Form("Native menu");
        f.addCommand(new Command("Save"));
        f.show();
        DisplayTest.flushEdt();

        assertNull(f.getToolbar().getParent(),
                "with somewhere for the commands to go, the toolbar is hidden as before");
    }

    /**
     * The same guard one level down, in {@code MenuBar.updateCommands}, which is the path a
     * form takes when the application sets {@code Display.COMMAND_BEHAVIOR_NATIVE} directly
     * rather than through the desktop title-bar mode.
     */
    /**
     * The desktop native themes declare {@code commandBehavior: Native}. With a Toolbar the
     * commands live on the Toolbar, not on the form's MenuBar, so publishing the MenuBar's list
     * at show time handed the native menu bar nothing: the menu bar came up empty and the
     * commands, whose Toolbar was hidden, could not be reached at all.
     */
    @FormTest
    void nativeCommandBehaviourWithAToolbarPublishesTheToolbarsCommands() {
        desktopMode("native");
        Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_NATIVE);
        try {
            Form f = new Form("My App");
            Command save = Command.create("Save", null, null);
            f.getToolbar().addCommandToOverflowMenu(save);
            f.show();
            DisplayTest.flushEdt();

            Vector bridged = implementation.getLastNativeCommands();
            assertNotNull(bridged, "commands must be bridged to the native menu bar");
            assertTrue(bridged.contains(save), "the Toolbar's command must reach the native menu bar");
        } finally {
            Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_DEFAULT);
        }
    }

    @FormTest
    void nativeModeToolbarBarAndOverflowCommandsAddedAfterShowReachTheNativeMenu() {
        // Only the side-menu paths used to republish; a command added to the left bar, the right
        // bar or the overflow after the form was shown had nowhere to appear while the Toolbar
        // was hidden.
        desktopMode("native");
        Form f = new Form("My App");
        f.show();
        DisplayTest.flushEdt();

        Command left = Command.create("Left", null, null);
        Command right = Command.create("Right", null, null);
        Command overflow = Command.create("Overflow", null, null);
        f.getToolbar().addCommandToLeftBar(left);
        f.getToolbar().addCommandToRightBar(right);
        f.getToolbar().addCommandToOverflowMenu(overflow);
        Vector bridged = implementation.getLastNativeCommands();
        assertTrue(bridged.contains(left), "left bar command published");
        assertTrue(bridged.contains(right), "right bar command published");
        assertTrue(bridged.contains(overflow), "overflow command published");

        f.getToolbar().removeOverflowCommand(overflow);
        f.getToolbar().removeCommand(right);
        bridged = implementation.getLastNativeCommands();
        assertFalse(bridged.contains(overflow), "a removed overflow command leaves the native menu");
        assertFalse(bridged.contains(right), "a removed bar command leaves the native menu");
    }

    @FormTest
    void nativeModeLeavesTheSearchCommandOutOfTheNativeMenu() {
        // The search command opens a search bar inside the Toolbar, which native chrome hides;
        // from the menu it threw instead of searching.
        desktopMode("native");
        Form f = new Form("My App");
        f.getToolbar().addSearchCommand(null);
        Command other = Command.create("Other", null, null);
        f.getToolbar().addCommandToOverflowMenu(other);
        f.show();
        DisplayTest.flushEdt();

        Vector bridged = implementation.getLastNativeCommands();
        assertTrue(bridged.contains(other), "ordinary commands are still published");
        for (int i = 0; i < bridged.size(); i++) {
            Command c = (Command) bridged.elementAt(i);
            assertFalse(c.getMaterialIcon() == FontImage.MATERIAL_SEARCH && "".equals(c.getCommandName()),
                    "the search command is not published");
        }
        f.getToolbar().showSearchBar(null);
        DisplayTest.flushEdt();
    }

    @FormTest
    void nativeCommandBehaviourStillDrawsSoftButtonsWithNoNativeMenuBar() {
        implementation.setDesktop(true);
        implementation.setNativeCommandsSupported(false);
        Toolbar.setGlobalToolbar(false);
        Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_NATIVE);
        try {
            Form f = new Form("Soft buttons");
            Command save = new Command("Save");
            f.addCommand(save);
            f.show();
            DisplayTest.flushEdt();

            assertTrue(hasButtonLabelled(f, "Save"),
                    "with nowhere native to put it, the command has to be drawn");
        } finally {
            Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_DEFAULT);
            Toolbar.setGlobalToolbar(true);
        }
    }

    /**
     * The Initializr's barebones template does exactly this in {@code runApp()}. In native mode
     * the side menu used to be built on {@code getComponentForm()}, which is null while the
     * toolbar is detached, so the app died with a NullPointerException before its first form
     * was shown -- a blank desktop window for every project generated with
     * {@code desktop.titleBar=native}.
     */
    @FormTest
    void nativeModeBridgesSideMenuCommandsInsteadOfBuildingASideMenu() {
        desktopMode("native");

        Form hi = new Form("Hi World", BoxLayout.y());
        hi.add(new Button("Hello World"));
        Command hello = hi.getToolbar().addMaterialCommandToSideMenu("Hello Command",
                FontImage.MATERIAL_CHECK, 4, e -> { });
        hi.show();
        DisplayTest.flushEdt();

        assertNull(hi.getToolbar().getParent(), "the toolbar stays detached in native mode");
        Vector bridged = implementation.getLastNativeCommands();
        assertNotNull(bridged, "commands must be bridged to the native menu bar");
        assertTrue(bridged.contains(hello), "the side-menu command must reach the native menu bar");
    }

    /** The rest of the Toolbar API that assumed an attached form. */
    @FormTest
    void nativeModeToolbarCallsDoNotNeedAnAttachedForm() {
        desktopMode("native");

        Form f = new Form("Detached");
        Toolbar tb = f.getToolbar();
        Command item = new Command("Item");
        tb.addComponentToSideMenu(new Label("Header"));
        tb.addComponentToSideMenu(new Button("Row"), item);
        tb.addComponentToRightSideMenu(new Label("Right header"));
        Command back = new Command("Back");
        tb.setBackCommand(back);
        assertEquals(back, f.getBackCommand(), "the back command still reaches the form");
        tb.hideToolbar();
        tb.showToolbar();
        tb.openSideMenu();
        tb.openRightSideMenu();
        f.show();
        DisplayTest.flushEdt();

        assertTrue(implementation.getLastNativeCommands().contains(item),
                "a component row's command reaches the native menu bar");
    }

    @FormTest
    void nativeModeRemovedSideMenuCommandLeavesTheNativeMenu() {
        desktopMode("native");

        Form f = new Form("Remove");
        Command gone = new Command("Gone");
        f.getToolbar().addCommandToSideMenu(gone);
        f.getToolbar().removeCommand(gone);

        assertFalse(f.getToolbar().getAllNativeMenuCommands().contains(gone),
                "a removed command must not be bridged");
    }

    @FormTest
    void nativeModeSideMenuCommandsChangedAfterShowReachTheNativeMenu() {
        desktopMode("native");

        Form f = new Form("Later");
        f.show();
        DisplayTest.flushEdt();

        Command later = new Command("Later");
        f.getToolbar().addCommandToSideMenu(later);
        assertTrue(implementation.getLastNativeCommands().contains(later),
                "a command added while the form is showing must be published");

        f.getToolbar().removeCommand(later);
        assertFalse(implementation.getLastNativeCommands().contains(later),
                "and removing it must unpublish it");
    }

    private boolean hasButtonLabelled(Container root, String text) {
        for (int iter = 0; iter < root.getComponentCount(); iter++) {
            Component c = root.getComponentAt(iter);
            if (c instanceof Button && text.equals(((Button) c).getText())) {
                return true;
            }
            if (c instanceof Container && hasButtonLabelled((Container) c, text)) {
                return true;
            }
        }
        return false;
    }

    @FormTest
    void desktopShortcutHintRoundTrips() {
        Command save = new Command("Save");
        assertEquals(0, save.getDesktopShortcutKeyChar(), "no shortcut by default");
        assertEquals(0, save.getDesktopShortcutModifiers(), "no modifiers by default");

        // single-arg form uses the platform-primary modifier and upper-cases the key
        save.setDesktopShortcut('s');
        assertEquals('S', save.getDesktopShortcutKeyChar());
        assertEquals(Command.DESKTOP_SHORTCUT_MODIFIER_PRIMARY, save.getDesktopShortcutModifiers());

        Command saveAs = new Command("Save As");
        saveAs.setDesktopShortcut('s',
                Command.DESKTOP_SHORTCUT_MODIFIER_PRIMARY | Command.DESKTOP_SHORTCUT_MODIFIER_SHIFT);
        assertEquals('S', saveAs.getDesktopShortcutKeyChar());
        assertEquals(Command.DESKTOP_SHORTCUT_MODIFIER_PRIMARY | Command.DESKTOP_SHORTCUT_MODIFIER_SHIFT,
                saveAs.getDesktopShortcutModifiers());
    }

    @FormTest
    void toolbarModeKeepsToolbarAttached() {
        // default desktop mode is "toolbar": behavior must be unchanged from today
        implementation.setDesktop(true);
        Toolbar.setGlobalToolbar(true);
        Form f = new Form("Legacy");
        f.show();
        DisplayTest.flushEdt();

        assertNotNull(f.getToolbar().getParent(), "in toolbar mode the toolbar is shown as today");
    }
}
