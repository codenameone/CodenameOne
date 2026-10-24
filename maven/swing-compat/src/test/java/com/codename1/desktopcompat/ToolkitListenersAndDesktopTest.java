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
package com.codename1.desktopcompat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.codename1.desktopcompat.java.awt.AWTEvent;
import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Desktop;
import com.codename1.desktopcompat.java.awt.Dialog;
import com.codename1.desktopcompat.java.awt.Taskbar;
import com.codename1.desktopcompat.java.awt.Toolkit;
import com.codename1.desktopcompat.java.awt.desktop.PreferencesEvent;
import com.codename1.desktopcompat.java.awt.desktop.PreferencesHandler;
import com.codename1.desktopcompat.java.awt.event.AWTEventListener;
import com.codename1.desktopcompat.java.awt.event.InputEvent;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.desktopcompat.javax.swing.JButton;
import com.codename1.desktopcompat.javax.swing.JFrame;
import com.codename1.desktopcompat.javax.swing.JPopupMenu;
import com.codename1.desktopcompat.rt.AwtListeners;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Test;

/// The toolkit's event listeners and shortcut key, and the parts of the
/// desktop a device does not have, which say so.
public class ToolkitListenersAndDesktopTest extends WindowsTestBase {

    @After
    public void forget() {
        AwtListeners.reset();
        AwtListeners.setCommandKey(null);
    }

    @Test
    public void aToolkitListenerSeesTheEventsOfItsMaskBeforeTheComponent() {
        JFrame f = new JFrame();
        f.setLayout(new BorderLayout());
        final JButton b = new JButton("go");
        f.add(b, BorderLayout.CENTER);
        show(f);
        final List<Integer> ids = new ArrayList<Integer>();
        AWTEventListener l = new AWTEventListener() {
            @Override
            public void eventDispatched(AWTEvent event) {
                if (event.getSource() == b) {
                    ids.add(Integer.valueOf(event.getID()));
                }
            }
        };
        Toolkit tk = Toolkit.getDefaultToolkit();
        tk.addAWTEventListener(l, AWTEvent.MOUSE_EVENT_MASK);
        assertEquals(1, tk.getAWTEventListeners().length);
        assertEquals(1, tk.getAWTEventListeners(AWTEvent.MOUSE_EVENT_MASK).length);
        assertEquals(0, tk.getAWTEventListeners(AWTEvent.KEY_EVENT_MASK).length);
        click(f, b, 5, 5);
        assertTrue(ids.toString(), ids.contains(Integer.valueOf(MouseEvent.MOUSE_PRESSED)));
        assertTrue(ids.toString(), ids.contains(Integer.valueOf(MouseEvent.MOUSE_RELEASED)));
        assertTrue(ids.toString(), ids.contains(Integer.valueOf(MouseEvent.MOUSE_CLICKED)));
        for (int i = 0; i < ids.size(); i++) {
            int id = ids.get(i).intValue();
            assertTrue("only mouse events: " + id, id >= MouseEvent.MOUSE_FIRST && id <= MouseEvent.MOUSE_LAST);
        }
        tk.removeAWTEventListener(l);
        ids.clear();
        click(f, b, 5, 5);
        assertTrue(ids.isEmpty());
        assertEquals(0, tk.getAWTEventListeners().length);
    }

    @Test
    public void theMenuShortcutKeyIsCommandOnApplePlatformsAndControlElsewhere() {
        Toolkit tk = Toolkit.getDefaultToolkit();
        AwtListeners.setCommandKey(Boolean.TRUE);
        assertEquals(InputEvent.META_DOWN_MASK, tk.getMenuShortcutKeyMaskEx());
        assertEquals(InputEvent.META_MASK, tk.getMenuShortcutKeyMask());
        AwtListeners.setCommandKey(Boolean.FALSE);
        assertEquals(InputEvent.CTRL_DOWN_MASK, tk.getMenuShortcutKeyMaskEx());
        assertEquals(InputEvent.CTRL_MASK, tk.getMenuShortcutKeyMask());
    }

    @Test
    public void whatADeviceLacksIsReportedUnsupported() {
        assertFalse(Taskbar.isTaskbarSupported());
        try {
            Taskbar.getTaskbar();
            fail("no taskbar");
        } catch (UnsupportedOperationException expected) {
            assertTrue(true);
        }
        Desktop d = Desktop.getDesktop();
        assertFalse(d.isSupported(Desktop.Action.APP_PREFERENCES));
        assertFalse(d.isSupported(Desktop.Action.MOVE_TO_TRASH));
        try {
            d.setPreferencesHandler(new PreferencesHandler() {
                @Override
                public void handlePreferences(PreferencesEvent e) {
                    fail("never called");
                }
            });
            fail("an action that is not supported throws");
        } catch (UnsupportedOperationException expected) {
            assertTrue(true);
        }
    }

    @Test
    public void aWindowIsNeverExcludedFromModality() {
        Toolkit tk = Toolkit.getDefaultToolkit();
        assertTrue(tk.isModalExclusionTypeSupported(Dialog.ModalExclusionType.NO_EXCLUDE));
        assertFalse(tk.isModalExclusionTypeSupported(Dialog.ModalExclusionType.APPLICATION_EXCLUDE));
        JFrame f = new JFrame();
        f.setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
        assertSame(Dialog.ModalExclusionType.NO_EXCLUDE, f.getModalExclusionType());
    }

    @Test
    public void thePopupWeightIsKeptAsAHint() {
        boolean was = JPopupMenu.getDefaultLightWeightPopupEnabled();
        try {
            JPopupMenu.setDefaultLightWeightPopupEnabled(false);
            JPopupMenu menu = new JPopupMenu();
            assertFalse(menu.isLightWeightPopupEnabled());
            menu.setLightWeightPopupEnabled(true);
            assertTrue(menu.isLightWeightPopupEnabled());
        } finally {
            JPopupMenu.setDefaultLightWeightPopupEnabled(was);
        }
    }
}
