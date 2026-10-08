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
package com.codename1.desktopcompat.org.jdesktop.swingx.hyperlink;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.codename1.desktopcompat.KernelTestBase;
import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.javax.swing.AbstractAction;
import com.codename1.desktopcompat.javax.swing.JFrame;
import com.codename1.desktopcompat.org.jdesktop.swingx.JXHyperlink;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

/// The hyperlink and its actions: what a click fires and opens, and the
/// color the link takes afterwards. Addresses go to a recording opener,
/// never to a browser.
public class HyperlinkTest extends KernelTestBase {

    private final List<String> opened = new ArrayList<String>();

    /// The kernel counts clicks by time alone and keeps the count between
    /// tests; wait the count of the taps made here out.
    @AfterClass
    public static void letTheClickCountRunOut() throws InterruptedException {
        Thread.sleep(600);
    }

    @Before
    public void record() {
        HyperlinkAction.setOpener(new UrlOpener() {
            @Override
            public void open(String url) {
                opened.add(url);
            }
        });
    }

    @After
    public void restore() {
        HyperlinkAction.setOpener(null);
    }

    @Test
    public void aClickOpensTheAddressAndMarksTheLinkVisited() {
        JXHyperlink link = new JXHyperlink();
        link.setURI(URI.create("http://example.com/a"));
        assertEquals("http://example.com/a", link.getText());
        assertTrue(link.isEnabled());
        assertFalse(link.isClicked());
        assertEquals(link.getUnclickedColor(), link.getForeground());
        JFrame f = new JFrame();
        f.add(link, BorderLayout.NORTH);
        show(f);
        assertNotNull(find(paint(f), "http://example.com/a"));
        press(f, link, 5, 5);
        release(f, link, 5, 5);
        assertEquals("[http://example.com/a]", opened.toString());
        assertTrue(link.isClicked());
        assertEquals(link.getClickedColor(), link.getForeground());
        assertTrue(link.getAction() instanceof HyperlinkAction);
        assertTrue(((HyperlinkAction) link.getAction()).isVisited());
    }

    @Test
    public void theLinkFollowsItsActionsVisitedFlagAndTarget() {
        HyperlinkAction action = HyperlinkAction.createHyperlinkAction(URI.create("http://example.com/b"));
        JXHyperlink link = new JXHyperlink(action);
        assertFalse(link.isClicked());
        action.setVisited(true);
        assertTrue(link.isClicked());
        action.setTarget(URI.create("mailto:someone@example.com"));
        assertEquals("mailto:someone@example.com", link.getText());
        assertFalse(action.isVisited());
        assertFalse(link.isClicked());
        action.actionPerformed(new ActionEvent(link, ActionEvent.ACTION_PERFORMED, null));
        assertEquals("[mailto:someone@example.com]", opened.toString());
        assertTrue(link.isClicked());
    }

    @Test
    public void anActionWithoutATargetIsDisabledAndOpensNothing() {
        HyperlinkAction action = new HyperlinkAction();
        assertFalse(action.isEnabled());
        action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
        assertTrue(opened.isEmpty());
        assertFalse(action.isVisited());
        JXHyperlink link = new JXHyperlink();
        link.setURI(null);
        assertFalse(link.isEnabled());
    }

    @Test
    public void aPlainActionIsFiredAndTheLinkMarksItselfClicked() {
        final int[] fired = new int[1];
        JXHyperlink link = new JXHyperlink(new AbstractAction("Go") {
            @Override
            public void actionPerformed(ActionEvent e) {
                fired[0]++;
            }
        });
        assertEquals("Go", link.getText());
        JFrame f = new JFrame();
        f.add(link, BorderLayout.NORTH);
        show(f);
        press(f, link, 5, 5);
        release(f, link, 5, 5);
        assertEquals(1, fired[0]);
        assertTrue(link.isClicked());
        assertTrue(opened.isEmpty());
    }

    @Test
    public void theColorsFollowTheClickedState() {
        JXHyperlink link = new JXHyperlink();
        link.setUnclickedColor(Color.GREEN);
        assertSame(Color.GREEN, link.getForeground());
        link.setClickedColor(Color.RED);
        assertSame(Color.GREEN, link.getForeground());
        link.setClicked(true);
        assertSame(Color.RED, link.getForeground());
        link.setClicked(false);
        assertSame(Color.GREEN, link.getForeground());
    }

    @Test
    public void theExtendedActionKeepsItsValues() {
        HyperlinkAction action = new HyperlinkAction();
        action.setShortDescription("short");
        assertEquals("short", action.getShortDescription());
        assertEquals("short", action.getLongDescription());
        action.setActionCommand("cmd");
        assertEquals("cmd", action.getActionCommand());
        action.setMnemonic("Open");
        assertEquals('O', action.getMnemonic());
        action.setGroup("g");
        assertEquals("g", action.getGroup());
        action.setStateAction(true);
        assertFalse(action.isStateAction());
        action.setSelected(true);
        assertTrue(action.isSelected());
    }
}
