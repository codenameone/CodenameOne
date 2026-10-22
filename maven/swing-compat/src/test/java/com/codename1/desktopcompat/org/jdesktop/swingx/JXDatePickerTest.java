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
package com.codename1.desktopcompat.org.jdesktop.swingx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.codename1.desktopcompat.KernelTestBase;
import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.java.beans.PropertyChangeEvent;
import com.codename1.desktopcompat.java.beans.PropertyChangeListener;
import com.codename1.desktopcompat.javax.swing.JFrame;
import com.codename1.ui.spinner.Picker;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.junit.Test;

/// The date picker: the date it holds, the text its formats show it
/// with, what a choice in the chooser tells the listeners, and the month
/// view that follows along. Every date is fixed.
public class JXDatePickerTest extends KernelTestBase {

    private static Date date(int month, int day, int hour) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, 2024);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private JFrame open(JXDatePicker picker) {
        JFrame f = new JFrame();
        f.add(picker, BorderLayout.NORTH);
        return show(f);
    }

    private static Picker peer(JXDatePicker picker) {
        return (Picker) picker.cn1Peer();
    }

    private static List<String> listen(JXDatePicker picker) {
        final List<String> commands = new ArrayList<String>();
        picker.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                commands.add(e.getActionCommand());
            }
        });
        return commands;
    }

    @Test
    public void theDateIsKeptAsItsDay() {
        JXDatePicker picker = new JXDatePicker();
        assertNull(picker.getDate());
        final List<Object> changes = new ArrayList<Object>();
        picker.addPropertyChangeListener("date", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent e) {
                changes.add(e.getNewValue());
            }
        });
        picker.setDate(date(Calendar.MARCH, 15, 17));
        assertEquals(date(Calendar.MARCH, 15, 0), picker.getDate());
        assertEquals(1, changes.size());
        assertEquals(date(Calendar.MARCH, 15, 0), changes.get(0));
        // Another hour of the same day is the same date.
        picker.setDate(date(Calendar.MARCH, 15, 3));
        assertEquals(1, changes.size());
        picker.setDate(null);
        assertNull(picker.getDate());
        assertEquals(2, changes.size());
        assertEquals(date(Calendar.JUNE, 2, 0), new JXDatePicker(date(Calendar.JUNE, 2, 9)).getDate());
    }

    @Test
    public void theFirstFormatShowsTheDateAndReadsItBack() throws Exception {
        JXDatePicker picker = new JXDatePicker(date(Calendar.MARCH, 15, 17));
        picker.setFormats("yyyy-MM-dd", "dd/MM/yyyy");
        DateFormat[] formats = picker.getFormats();
        assertEquals(2, formats.length);
        open(picker);
        assertEquals("2024-03-15", peer(picker).getText());
        assertEquals(picker.getDate(), formats[0].parse(peer(picker).getText()));

        picker.setDate(date(Calendar.NOVEMBER, 3, 0));
        assertEquals("2024-11-03", peer(picker).getText());
        assertEquals(picker.getDate(), formats[0].parse(peer(picker).getText()));

        // Formats handed over as objects, and replaced while showing.
        DateFormat other = new SimpleDateFormat("dd.MM.yyyy");
        picker.setFormats(other);
        assertEquals(1, picker.getFormats().length);
        assertSame(other, picker.getFormats()[0]);
        assertEquals("03.11.2024", peer(picker).getText());
        assertEquals(picker.getDate(), other.parse(peer(picker).getText()));

        // The array handed out is a copy.
        picker.getFormats()[0] = null;
        assertSame(other, picker.getFormats()[0]);
        try {
            picker.setFormats("yyyy", null);
            throw new AssertionError("a null pattern must be refused");
        } catch (NullPointerException expected) {
            assertSame(other, picker.getFormats()[0]);
        }
    }

    @Test
    public void aChoiceInTheChooserCommits() throws Exception {
        JXDatePicker picker = new JXDatePicker(date(Calendar.MARCH, 15, 0));
        picker.setFormats("yyyy-MM-dd");
        List<String> commands = listen(picker);
        open(picker);
        assertEquals("setting up tells nobody", 0, commands.size());
        picker.setDate(date(Calendar.MARCH, 16, 0));
        assertEquals("nor does a date set by the program", 0, commands.size());

        // What the chooser does when the user picks a date.
        peer(picker).setDate(date(Calendar.MARCH, 20, 11));
        assertEquals(date(Calendar.MARCH, 20, 0), picker.getDate());
        assertEquals(1, commands.size());
        assertEquals(JXDatePicker.COMMIT_KEY, commands.get(0));
        assertEquals("2024-03-20", peer(picker).getText());
        assertEquals(date(Calendar.MARCH, 20, 0), picker.getMonthView().getSelectionDate());

        assertTrue(picker.isEditValid());
        picker.commitEdit();
        assertEquals(date(Calendar.MARCH, 20, 0), picker.getDate());
        assertEquals(2, commands.size());
        assertEquals(JXDatePicker.COMMIT_KEY, commands.get(1));
        picker.cancelEdit();
        assertEquals(JXDatePicker.CANCEL_KEY, commands.get(2));
        assertEquals(date(Calendar.MARCH, 20, 0), picker.getDate());
    }

    @Test
    public void theMonthViewFollowsAndLeads() {
        JXDatePicker picker = new JXDatePicker();
        JXMonthView view = picker.getMonthView();
        assertTrue(view.isSelectionEmpty());
        picker.setDate(date(Calendar.AUGUST, 21, 6));
        assertEquals(date(Calendar.AUGUST, 21, 0), view.getSelectionDate());
        assertEquals(date(Calendar.AUGUST, 1, 0), view.getFirstDisplayedDay());

        // A day committed in the month view becomes the picker's date.
        List<String> commands = listen(picker);
        view.setSelectionDate(date(Calendar.AUGUST, 9, 0));
        assertEquals("not before it is committed", date(Calendar.AUGUST, 21, 0), picker.getDate());
        view.commitSelection();
        assertEquals(date(Calendar.AUGUST, 9, 0), picker.getDate());
        assertEquals(1, commands.size());
        assertEquals(JXDatePicker.COMMIT_KEY, commands.get(0));

        // A cancelled choice is put back.
        view.setSelectionDate(date(Calendar.AUGUST, 30, 0));
        view.cancelSelection();
        assertEquals(date(Calendar.AUGUST, 9, 0), view.getSelectionDate());
        assertEquals(JXDatePicker.CANCEL_KEY, commands.get(1));

        // A new month view takes the date over, and the old one is let go.
        JXMonthView other = new JXMonthView(date(Calendar.JANUARY, 1, 0));
        picker.setMonthView(other);
        assertSame(other, picker.getMonthView());
        assertEquals(date(Calendar.AUGUST, 9, 0), other.getSelectionDate());
        view.setSelectionDate(date(Calendar.AUGUST, 2, 0));
        view.commitSelection();
        assertEquals(date(Calendar.AUGUST, 9, 0), picker.getDate());
        assertEquals(2, commands.size());
    }

    @Test
    public void theChooserOpensOnlyWhenEditableAndEnabled() {
        JXDatePicker picker = new JXDatePicker(date(Calendar.MARCH, 15, 0));
        open(picker);
        assertTrue(picker.isEditable());
        assertTrue(peer(picker).isEnabled());
        picker.setEditable(false);
        assertFalse(picker.isEditable());
        assertFalse(peer(picker).isEnabled());
        picker.setEditable(true);
        assertTrue(peer(picker).isEnabled());
        picker.setEnabled(false);
        assertFalse(peer(picker).isEnabled());
        picker.setEnabled(true);
        assertTrue(peer(picker).isEnabled());
    }
}
