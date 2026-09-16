/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.events.PointerEvent;
import com.codename1.ui.plaf.UIManager;

import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Hover DELIVERY: what actually happens when a port reports a pointer position.
///
/// `ComponentHoverStyleTest` covers the style resolution with the flag set by hand. The two
/// cases here are the ones that looked right in that test and still rendered nothing on a
/// real desktop, because the flag was being put somewhere `getStyle()` does not read, or
/// nowhere at all.
class HoverDeliveryTest extends UITestBase {

    /// setDesktop is global to the implementation, so a test that turns it on has to put it
    /// back or every later test in the run inherits a desktop it did not ask for.
    @org.junit.jupiter.api.AfterEach
    void restoreDesktopFlag() {
        implementation.setDesktop(false);
        implementation.resetPointerEventMetadata();
    }

    /// Theme with a hover colour on the row UIID and on a plain button.
    ///
    /// Installed AFTER the surface is shown and followed by a refresh: showing loads the
    /// default theme, and a component caches the style it already built, so props set before
    /// show are overwritten and props set after show are not picked up without the refresh.
    private static void installHoverTheme(Container refresh) {
        Hashtable theme = new Hashtable();
        theme.put("LeadRow.bgColor", "112233");
        theme.put("LeadRow.hover#bgColor", "44ff88");
        theme.put("Button.bgColor", "112233");
        theme.put("Button.hover#bgColor", "44ff88");
        // The selected colours are pinned to the normal ones because showing a surface
        // focuses its first focusable component, and getStyle() answers the SELECTED style
        // for a focused component. Without these the baseline reads as the blank default and
        // says nothing about hover. It also makes the hover assertion sharper: hover outranks
        // focus, which is the desktop behaviour, so 44ff88 can only come from the hover style.
        theme.put("LeadRow.sel#bgColor", "112233");
        theme.put("Button.sel#bgColor", "112233");
        // addThemeProps, and the full refresh sequence a live theme change uses. setThemeProps
        // REPLACES the table, which drops the defaults the surface was built against, and a
        // component keeps the style it already built until the surface is refreshed.
        UIManager.getInstance().addThemeProps(theme);
        UIManager.getInstance().refreshTheme();
        refresh.refreshTheme(true);
        refresh.revalidate();
        DisplayTest.flushEdt();
    }

    /// Hovers the centre of a component the way a port does.
    private static void hoverForm(Form f, Component cmp) {
        f.pointerHover(new int[]{cmp.getAbsoluteX() + cmp.getWidth() / 2},
                new int[]{cmp.getAbsoluteY() + cmp.getHeight() / 2});
    }

    /// A container with a lead component paints its own hover style when the pointer is over
    /// any of its children.
    ///
    /// This is the case a MultiButton, a SpanButton or a toolbar command container is: the
    /// pointer lands on an inner label, `Form.pointerHover` resolves it to the lead PARENT,
    /// and `Component.getStyle()` returns out of its lead branch after consulting the lead
    /// COMPONENT. Marking the parent therefore satisfied nothing that paints, and the row
    /// stayed at its normal colour with the pointer sitting on it.
    @FormTest
    void aLeadContainerShowsItsHoverStyleWhenAChildIsHovered() {
        Form f = new Form("lead", new BorderLayout());
        Container row = new Container(new BorderLayout());
        row.setUIID("LeadRow");
        Button lead = new Button("lead");
        Label child = new Label("child");
        row.add(BorderLayout.WEST, lead);
        row.add(BorderLayout.CENTER, child);
        f.add(BorderLayout.NORTH, row);
        f.show();
        DisplayTest.flushEdt();
        // After showing: setLeadComponent only builds the lead hierarchy on an initialized
        // container, so doing this before show leaves hasLead false and the test proves
        // nothing about lead components at all.
        row.setLeadComponent(lead);
        f.revalidate();
        DisplayTest.flushEdt();
        installHoverTheme(f);

        assertEquals(0x112233, row.getStyle().getBgColor(), "before any hover");

        hoverForm(f, child);
        DisplayTest.flushEdt();
        assertEquals(0x44ff88, row.getStyle().getBgColor(),
                "hovering a child of a lead container must paint the container's hover style");
        // The child does NOT take the row's hover colour. Hover is opt-in per UIID and the
        // theme declares none for Label, so the lead gives the label the ability to resolve
        // hover from the row's pointer -- and resolving it yields nothing, which is the
        // property that keeps every pre-hover application looking the way it always did.
        assertNotEquals(0x44ff88, child.getStyle().getBgColor(),
                "a UIID with no hover entry must not inherit the row's hover colour");

        // And away again: the ports report leaving the window as a hover at (-1,-1).
        f.pointerHover(new int[]{-1}, new int[]{-1});
        DisplayTest.flushEdt();
        assertEquals(0x112233, row.getStyle().getBgColor(), "leaving must clear it");
    }

    /// Releasing a drag over a different component moves the hover there.
    ///
    /// Hover is not tracked during a drag -- pointerHover returns early while a component is
    /// being dragged -- and a pointer that stops moving after the release produces no further
    /// motion event, so without a catch-up on release the component the drag STARTED on stays
    /// hover-styled and the one under the pointer never lights up.
    @FormTest
    void releasingADragOverAnotherComponentMovesTheHover() {
        // The catch-up is desktop-only, because nothing else generates hover, so the test
        // has to be one -- otherwise it passes for the wrong reason on any implementation.
        implementation.setDesktop(true);
        Form f = new Form("drag", new com.codename1.ui.layouts.BoxLayout(
                com.codename1.ui.layouts.BoxLayout.Y_AXIS));
        Button a = new Button("A");
        Button b = new Button("B");
        f.add(a);
        f.add(b);
        f.show();
        DisplayTest.flushEdt();
        installHoverTheme(f);

        hoverForm(f, a);
        DisplayTest.flushEdt();
        assertEquals(0x44ff88, a.getStyle().getBgColor(), "A is hovered to begin with");

        // Press on A, then release over B without any motion event in between -- which is
        // what a drag that ends on a stationary pointer looks like to the form.
        f.pointerPressed(new int[]{a.getAbsoluteX() + a.getWidth() / 2},
                new int[]{a.getAbsoluteY() + a.getHeight() / 2});
        DisplayTest.flushEdt();
        implementation.setPointerType(PointerEvent.TYPE_MOUSE);
        f.pointerReleased(b.getAbsoluteX() + b.getWidth() / 2,
                b.getAbsoluteY() + b.getHeight() / 2);
        DisplayTest.flushEdt();

        assertNotEquals(0x44ff88, a.getStyle().getBgColor(),
                "the component the drag started on must not stay hovered");
        assertEquals(0x44ff88, b.getStyle().getBgColor(),
                "the component under the pointer at release must be hovered");
    }

    @FormTest
    void releaseNavigationDoesNotRestoreHoverOnTheHiddenForm() {
        implementation.setDesktop(true);
        final Form destination = new Form("destination");
        Form source = new Form("source", new BorderLayout());
        source.setTransitionOutAnimator(com.codename1.ui.animations.CommonTransitions.createEmpty());
        destination.setTransitionInAnimator(com.codename1.ui.animations.CommonTransitions.createEmpty());
        Button button = new Button("navigate");
        source.add(BorderLayout.CENTER, button);
        button.addActionListener(event -> destination.show());
        source.show();
        DisplayTest.flushEdt();
        installHoverTheme(source);
        hoverForm(source, button);
        assertTrue(button.isHovered());
        int x = button.getAbsoluteX() + button.getWidth() / 2;
        int y = button.getAbsoluteY() + button.getHeight() / 2;
        implementation.setPointerType(PointerEvent.TYPE_MOUSE);
        source.pointerPressed(x, y);
        source.pointerReleased(x, y);
        DisplayTest.flushEdt();
        assertEquals(destination, Display.getInstance().getCurrent());
        assertFalse(button.isHovered(), "release must not restore hover after navigation deinitializes the source");
        source.show();
        DisplayTest.flushEdt();
        assertFalse(button.isHovered(), "showing the source again must not revive stale hover");
    }

    @FormTest
    void hoverCallbacksSeeCurrentStateAndCanDetachOrHide() {
        implementation.setDesktop(true);
        implementation.setMultiWindowSupported(true);
        for (boolean secondary : new boolean[]{false, true}) {
            for (final boolean detach : new boolean[]{false, true}) {
                final Form main = new Form("hover callback", new com.codename1.ui.layouts.BoxLayout(
                        com.codename1.ui.layouts.BoxLayout.Y_AXIS));
                main.setTransitionOutAnimator(com.codename1.ui.animations.CommonTransitions.createEmpty());
                main.show();
                DisplayTest.flushEdt();
                final Window window = secondary ? new Window("hover callback", new com.codename1.ui.layouts.BoxLayout(
                        com.codename1.ui.layouts.BoxLayout.Y_AXIS)) : null;
                final Container surface = window == null ? main : window;
                final Button previous = new Button("previous");
                Button target = new Button("target") {
                    @Override
                    public void pointerHover(int[] x, int[] y) {
                        assertTrue(isHovered(), "enter callback sees its new hover state");
                        assertFalse(previous.isHovered(), "the previous target is already cleared");
                        assertEquals(0x44ff88, getStyle().getBgColor());
                        if (detach) {
                            getParent().removeComponent(this);
                        } else if (window != null) {
                            window.hide();
                        } else {
                            new Form("navigated").show();
                        }
                    }
                };
                surface.add(previous);
                surface.add(target);
                if (window != null) {
                    window.setWindowSize(500, 400);
                    window.show();
                }
                installHoverTheme(surface);
                try {
                    surface.pointerHover(new int[]{previous.getAbsoluteX() + previous.getWidth() / 2},
                            new int[]{previous.getAbsoluteY() + previous.getHeight() / 2});
                    assertTrue(previous.isHovered());
                    surface.pointerHover(new int[]{target.getAbsoluteX() + target.getWidth() / 2},
                            new int[]{target.getAbsoluteY() + target.getHeight() / 2});
                    DisplayTest.flushEdt();
                    assertFalse(target.isHovered(), "a callback must not leave a detached or hidden target hovered");
                } finally {
                    if (window != null) window.dispose();
                }
            }
        }
    }

    @FormTest
    void hidingAReusableWindowClearsHoverIncludingDuringRelease() {
        implementation.setDesktop(true);
        implementation.setMultiWindowSupported(true);
        new Form("main").show();
        DisplayTest.flushEdt();
        final Window window = new Window("reusable", new BorderLayout());
        Button button = new Button("hide");
        window.add(BorderLayout.CENTER, button);
        window.setWindowSize(500, 400);
        window.show();
        installHoverTheme(window);
        try {
            int x = button.getAbsoluteX() + button.getWidth() / 2;
            int y = button.getAbsoluteY() + button.getHeight() / 2;
            window.pointerHover(new int[]{x}, new int[]{y});
            assertTrue(button.isHovered());
            window.hide();
            assertFalse(button.isHovered());
            window.show();
            assertFalse(button.isHovered());
            button.addActionListener(event -> window.hide());
            implementation.setPointerType(PointerEvent.TYPE_MOUSE);
            window.pointerPressed(x, y);
            window.pointerReleased(x, y);
            assertFalse(window.isTopLevelShowing());
            assertFalse(button.isHovered(), "release catch-up must not revive hover after hide");
            window.show();
            assertFalse(button.isHovered());
        } finally {
            window.dispose();
        }
    }

    /// A control in a secondary window responds to hover.
    ///
    /// `Window` is not a `Form` -- it extends `Container` -- and its `pointerHover` only
    /// forwarded the event. Nothing recorded which component the pointer was over, so with
    /// the native ports now delivering hover per window, a control there still could not
    /// paint a hover style its theme declared.
    @FormTest
    void aComponentInAWindowShowsItsHoverStyle() {
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        main.show();
        DisplayTest.flushEdt();

        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(500, 400);
        Button b = new Button("hover me");
        w.add(BorderLayout.CENTER, b);
        w.show();
        DisplayTest.flushEdt();
        installHoverTheme(w);

        assertEquals(0x112233, b.getStyle().getBgColor(), "before any hover");

        w.pointerHover(new int[]{b.getAbsoluteX() + b.getWidth() / 2},
                new int[]{b.getAbsoluteY() + b.getHeight() / 2});
        DisplayTest.flushEdt();
        assertTrue(b.isHovered(), "the window has to record what its pointer is over");
        assertEquals(0x44ff88, b.getStyle().getBgColor(),
                "a component in a window must paint its hover style");

        w.pointerHover(new int[]{-1}, new int[]{-1});
        DisplayTest.flushEdt();
        assertFalse(b.isHovered(), "leaving the window has to clear it");
        assertEquals(0x112233, b.getStyle().getBgColor(), "and it must paint normally again");

        w.dispose();
        DisplayTest.flushEdt();
    }
    @FormTest
    void releasesOnlyCreateHoverForMouseAndPenOnForms() {
        checkReleaseSources(false);
    }

    @FormTest
    void releasesOnlyCreateHoverForMouseAndPenOnWindows() {
        checkReleaseSources(true);
    }

    private void checkReleaseSources(boolean secondaryWindow) {
        implementation.setDesktop(true);
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        main.show();
        DisplayTest.flushEdt();
        Window window = secondaryWindow ? new Window("release", new BorderLayout()) : null;
        Container surface = secondaryWindow ? window : main;
        Button button = new Button("release");
        surface.add(BorderLayout.CENTER, button);
        if (window != null) {
            window.setWindowSize(500, 400);
            window.show();
        }
        surface.revalidate();
        DisplayTest.flushEdt();
        installHoverTheme(surface);
        int x = button.getAbsoluteX() + button.getWidth() / 2;
        int y = button.getAbsoluteY() + button.getHeight() / 2;
        try {
            for (int type : new int[]{PointerEvent.TYPE_TOUCH, PointerEvent.TYPE_MOUSE,
                    PointerEvent.TYPE_STYLUS, PointerEvent.TYPE_ERASER, PointerEvent.TYPE_UNKNOWN}) {
                surface.pointerHover(new int[]{-1}, new int[]{-1});
                implementation.setPointerType(type);
                surface.pointerPressed(x, y);
                surface.pointerReleased(x, y);
                assertEquals(type == PointerEvent.TYPE_MOUSE || type == PointerEvent.TYPE_STYLUS
                        || type == PointerEvent.TYPE_ERASER, button.isHovered(),
                        "release hover for pointer type " + type + " in window=" + secondaryWindow);
            }
        } finally {
            if (window != null) {
                window.dispose();
            }
        }
    }

    @FormTest
    void leavingAWindowCancelsPendingAndVisibleTooltips() throws Exception {
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        Window window = new Window("tooltip", new BorderLayout());
        window.setWindowSize(500, 400);
        Button button = new Button("tip");
        button.setTooltip("Window tooltip");
        window.add(BorderLayout.CENTER, button);
        window.show();
        DisplayTest.flushEdt();
        TooltipManager previous = TooltipManager.getInstance();
        TooltipManager manager = new TooltipManager();
        manager.setTooltipShowDelay(60000);
        TooltipManager.enableTooltips(manager);
        java.lang.reflect.Field pending = TooltipManager.class.getDeclaredField("pendingTooltip");
        java.lang.reflect.Field visible = TooltipManager.class.getDeclaredField("currentTooltip");
        pending.setAccessible(true);
        visible.setAccessible(true);
        try {
            window.pointerHover(new int[]{button.getAbsoluteX() + button.getWidth() / 2},
                    new int[]{button.getAbsoluteY() + button.getHeight() / 2});
            assertNotNull(pending.get(manager), "hover schedules a tooltip");
            window.pointerHover(new int[]{-1}, new int[]{-1});
            assertNull(pending.get(manager), "leaving cancels the scheduled tooltip");
            manager.showTooltip(button.getTooltip(), button);
            assertNotNull(visible.get(manager), "tooltip is visible before leaving");
            window.pointerHover(new int[]{-1}, new int[]{-1});
            assertNull(visible.get(manager), "leaving dismisses a visible tooltip");
        } finally {
            manager.clearTooltip();
            TooltipManager.enableTooltips(previous);
            window.dispose();
        }
    }

    @FormTest
    void leavingAFormDoesNotHoverItsRootPane() {
        checkRootPaneLeave(false);
    }

    @FormTest
    void leavingAWindowDoesNotHoverItsRootPane() {
        checkRootPaneLeave(true);
    }

    private void checkRootPaneLeave(boolean secondaryWindow) {
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        main.show();
        Window window = secondaryWindow ? new Window("root", new BorderLayout()) : null;
        if (window != null) {
            window.setWindowSize(500, 400);
            window.show();
        }
        DisplayTest.flushEdt();
        Container surface = window == null ? main : window;
        Container root = window == null ? main.getContentPane() : window.getContentPane();
        int x = root.getAbsoluteX() + root.getWidth() / 2;
        int y = root.getAbsoluteY() + root.getHeight() / 2;
        try {
            for (int[] outside : new int[][]{{-1, -1}, {surface.getWidth(), y},
                    {x, surface.getHeight()}, {x, -1}}) {
                surface.pointerHover(new int[]{x}, new int[]{y});
                assertTrue(root.isHovered(), "empty root pane is hovered while inside");
                surface.pointerHover(new int[]{outside[0]}, new int[]{outside[1]});
                assertFalse(root.isHovered(), "outside coordinates must not hit the root pane");
            }
        } finally {
            if (window != null) {
                window.dispose();
            }
        }
    }

}
