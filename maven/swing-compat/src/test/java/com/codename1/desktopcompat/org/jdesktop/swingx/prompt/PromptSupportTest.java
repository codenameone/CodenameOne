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
package com.codename1.desktopcompat.org.jdesktop.swingx.prompt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.codename1.desktopcompat.KernelTestBase;
import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.javax.swing.JFrame;
import com.codename1.desktopcompat.javax.swing.JTextField;
import com.codename1.desktopcompat.javax.swing.text.JTextComponent;
import com.codename1.desktopcompat.org.jdesktop.swingx.JXFormattedTextField;
import com.codename1.desktopcompat.org.jdesktop.swingx.JXSearchField;
import com.codename1.desktopcompat.org.jdesktop.swingx.JXSearchField.SearchMode;
import com.codename1.desktopcompat.org.jdesktop.swingx.JXTextArea;
import com.codename1.desktopcompat.org.jdesktop.swingx.JXTextField;
import com.codename1.desktopcompat.org.jdesktop.swingx.prompt.PromptSupport.FocusBehavior;
import com.codename1.ui.TextArea;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/// Prompts: the text handed to [PromptSupport] becomes the hint of the
/// Codename One text field behind the component.
public class PromptSupportTest extends KernelTestBase {

    private static String hint(JTextComponent c) {
        return ((TextArea) c.cn1Peer()).getHint();
    }

    private JFrame open(JTextComponent c) {
        JFrame f = new JFrame();
        f.add(c, BorderLayout.NORTH);
        return show(f);
    }

    @Test
    public void aPromptBecomesTheHintOfAPlainTextField() {
        JTextField field = new JTextField(10);
        assertNull(PromptSupport.getPrompt(field));
        PromptSupport.setPrompt("Name", field);
        assertEquals("Name", PromptSupport.getPrompt(field));
        assertEquals("Name", field.getClientProperty(PromptSupport.PROMPT));
        open(field);
        assertEquals("Name", hint(field));
        assertEquals("the prompt is not the text", "", field.getText());

        PromptSupport.setPrompt("Surname", field);
        assertEquals("Surname", hint(field));
        PromptSupport.setPrompt(null, field);
        assertNull(PromptSupport.getPrompt(field));
        assertEquals("", hint(field));
    }

    @Test
    public void theColorAndTheStyleReachTheHint() {
        JTextField field = new JTextField(10);
        open(field);
        PromptSupport.init("Find", new Color(0x336699), new Color(0xfff0f0), field);
        assertEquals("Find", hint(field));
        assertEquals(new Color(0x336699), PromptSupport.getForeground(field));
        assertEquals(new Color(0xfff0f0), PromptSupport.getBackground(field));
        TextArea peer = (TextArea) field.cn1Peer();
        assertNotNull(peer.getHintLabel());
        assertEquals(0x336699, peer.getHintLabel().getUnselectedStyle().getFgColor());

        assertNull(PromptSupport.getFontStyle(field));
        PromptSupport.setFontStyle(Integer.valueOf(Font.ITALIC), field);
        assertEquals(Integer.valueOf(Font.ITALIC), PromptSupport.getFontStyle(field));
        assertEquals("Find", hint(field));

        assertSame(FocusBehavior.HIDE_PROMPT, PromptSupport.getFocusBehavior(field));
        PromptSupport.setFocusBehavior(FocusBehavior.SHOW_PROMPT, field);
        assertSame(FocusBehavior.SHOW_PROMPT, PromptSupport.getFocusBehavior(field));
    }

    @Test
    public void thePromptFieldsCarryTheirPrompt() {
        JXTextField field = new JXTextField("City", new Color(0x808080));
        assertEquals("City", field.getPrompt());
        assertEquals(new Color(0x808080), field.getPromptForeground());
        open(field);
        assertEquals("City", hint(field));
        field.setPrompt("Town");
        assertEquals("Town", PromptSupport.getPrompt(field));
        assertEquals("Town", hint(field));
        field.setPromptFontStyle(Integer.valueOf(Font.BOLD));
        assertEquals(Integer.valueOf(Font.BOLD), field.getPromptFontStyle());
        field.setFocusBehavior(FocusBehavior.HIGHLIGHT_PROMPT);
        assertSame(FocusBehavior.HIGHLIGHT_PROMPT, field.getFocusBehavior());

        JXTextArea area = new JXTextArea("Notes");
        open(area);
        assertEquals("Notes", hint(area));
        area.setPrompt("Remarks");
        assertEquals("Remarks", hint(area));

        JXFormattedTextField formatted = new JXFormattedTextField("Amount");
        open(formatted);
        assertEquals("Amount", hint(formatted));
    }

    @Test
    public void theSearchFieldTellsItsListenersWhenToSearch() {
        JXSearchField search = new JXSearchField();
        assertEquals("Search", search.getPrompt());
        assertSame(SearchMode.INSTANT, search.getSearchMode());
        assertTrue(search.isInstantSearchMode());
        final List<String> searched = new ArrayList<String>();
        final JXSearchField field = search;
        search.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searched.add(field.getText());
            }
        });
        open(search);
        assertEquals("Search", hint(search));

        // Without a delay every change searches at once.
        search.setInstantSearchDelay(0);
        assertEquals(0, search.getInstantSearchDelay());
        search.setText("ab");
        assertFalse(searched.isEmpty());
        assertEquals("ab", searched.get(searched.size() - 1));

        // With one the timer is started instead.
        searched.clear();
        search.setInstantSearchDelay(60000);
        search.setText("abc");
        assertTrue(searched.isEmpty());
        assertTrue(search.getInstantSearchTimer().isRunning());
        assertFalse(search.getInstantSearchTimer().isRepeats());
        // Searching now stops the wait.
        search.postActionEvent();
        assertEquals(1, searched.size());
        assertEquals("abc", searched.get(0));
        assertFalse(search.getInstantSearchTimer().isRunning());

        // The regular mode waits for the edit to finish.
        searched.clear();
        search.setSearchMode(SearchMode.REGULAR);
        assertTrue(search.isRegularSearchMode());
        search.setText("abcd");
        assertTrue(searched.isEmpty());
        assertFalse(search.getInstantSearchTimer().isRunning());

        // The cancel action a field starts with clears the text.
        search.getCancelAction().actionPerformed(new ActionEvent(search, ActionEvent.ACTION_PERFORMED, "cancel"));
        assertEquals("", search.getText());
        final int[] cancelled = {0};
        ActionListener own = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancelled[0]++;
            }
        };
        search.setCancelAction(own);
        assertSame(own, search.getCancelAction());
    }
}
