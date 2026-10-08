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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.desktopcompat.java.awt.Window;
import com.codename1.desktopcompat.javax.swing.AbstractButton;
import com.codename1.desktopcompat.javax.swing.JButton;
import com.codename1.desktopcompat.javax.swing.JColorChooser;
import com.codename1.desktopcompat.javax.swing.JComboBox;
import com.codename1.desktopcompat.javax.swing.JDialog;
import com.codename1.desktopcompat.javax.swing.JFileChooser;
import com.codename1.desktopcompat.javax.swing.JFrame;
import com.codename1.desktopcompat.javax.swing.JLabel;
import com.codename1.desktopcompat.javax.swing.JList;
import com.codename1.desktopcompat.javax.swing.JOptionPane;
import com.codename1.desktopcompat.javax.swing.JSlider;
import com.codename1.desktopcompat.javax.swing.JTextField;
import com.codename1.desktopcompat.javax.swing.ProgressMonitor;
import com.codename1.desktopcompat.javax.swing.SwingUtilities;
import com.codename1.desktopcompat.javax.swing.filechooser.FileNameExtensionFilter;
import com.codename1.desktopcompat.rt.FilePicker;
import com.codename1.desktopcompat.rt.WindowHosts;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Test;

/// The standard dialogs: each blocks its caller until it is answered, and
/// answers with what the user did.
public class DialogsTest extends WindowsTestBase {

    private final List<String> order = new ArrayList<String>();

    @After
    public void restorePicker() {
        FilePicker.setSource(null);
    }

    /// Runs `r` on the event thread once the dialog the test is about to
    /// open is on the screen and the test is blocked in it.
    private void whileBlocked(final Runnable r) {
        Thread later = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                    return;
                }
                Display.getInstance().callSerially(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            r.run();
                            order.add("answered");
                        } catch (RuntimeException e) {
                            order.add("failed: " + e);
                            closeTop();
                        } catch (AssertionError e) {
                            order.add("failed: " + e);
                            closeTop();
                        }
                    }
                });
            }
        });
        later.start();
    }

    private static void closeTop() {
        Window w = WindowHosts.active();
        if (w instanceof JDialog) {
            w.setVisible(false);
        }
    }

    private static <T> T find(Component root, Class<T> type, String text) {
        if (type.isInstance(root)) {
            boolean match = text == null;
            if (!match && root instanceof AbstractButton) {
                match = text.equals(((AbstractButton) root).getText());
            }
            if (match) {
                return type.cast(root);
            }
        }
        if (root instanceof Container) {
            Component[] kids = ((Container) root).getComponents();
            for (int i = 0; i < kids.length; i++) {
                T t = find(kids[i], type, text);
                if (t != null) {
                    return t;
                }
            }
        }
        return null;
    }

    private static <T> T inDialog(Class<T> type, String text) {
        Window top = WindowHosts.active();
        assertTrue("a dialog is showing: " + top, top instanceof JDialog);
        T t = find(top, type, text);
        assertNotNull(type.getName() + " " + text, t);
        return t;
    }

    private void press(final String button) {
        whileBlocked(new Runnable() {
            @Override
            public void run() {
                inDialog(JButton.class, button).doClick();
            }
        });
    }

    private void answered() {
        order.add("returned");
        assertEquals("[answered, returned]", order.toString());
        order.clear();
    }

    @Test
    public void aConfirmDialogBlocksAndAnswersWithTheButton() {
        JFrame f = show(new JFrame("owner"));
        press("No");
        int r = JOptionPane.showConfirmDialog(f, "Sure?", "Confirm", JOptionPane.YES_NO_OPTION);
        answered();
        assertEquals(JOptionPane.NO_OPTION, r);
        assertTrue("the dialog is gone", f.isActive());

        press("Yes");
        assertEquals(JOptionPane.YES_OPTION, JOptionPane.showConfirmDialog(f, "Sure?"));
        answered();

        press("Cancel");
        assertEquals(JOptionPane.CANCEL_OPTION,
                JOptionPane.showConfirmDialog(f, "Sure?", "t", JOptionPane.YES_NO_CANCEL_OPTION));
        answered();

        press("OK");
        assertEquals(JOptionPane.OK_OPTION,
                JOptionPane.showConfirmDialog(f, "Sure?", "t", JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE));
        answered();
    }

    @Test
    public void closingAnOptionDialogAnswersClosed() {
        whileBlocked(new Runnable() {
            @Override
            public void run() {
                assertNull("a yes/no dialog has no cancel", find(WindowHosts.active(), JButton.class, "Cancel"));
                WindowHosts.active().cn1Closing();
            }
        });
        assertEquals(JOptionPane.CLOSED_OPTION,
                JOptionPane.showConfirmDialog(null, "Sure?", "t", JOptionPane.YES_NO_OPTION));
        answered();
    }

    @Test
    public void aMessageDialogBlocksUntilItIsDismissed() {
        whileBlocked(new Runnable() {
            @Override
            public void run() {
                assertEquals("Oops", ((JDialog) WindowHosts.active()).getTitle());
                JLabel icon = null;
                List<JLabel> labels = new ArrayList<JLabel>();
                collect(WindowHosts.active(), labels);
                StringBuilder text = new StringBuilder();
                for (JLabel l : labels) {
                    if (l.getIcon() != null) {
                        icon = l;
                    } else {
                        text.append(l.getText()).append('|');
                    }
                }
                assertNotNull("an error message has an icon", icon);
                assertEquals("one label a line", "It broke|badly|", text.toString());
                inDialog(JButton.class, "OK").doClick();
            }
        });
        JOptionPane.showMessageDialog(null, "It broke\nbadly", "Oops", JOptionPane.ERROR_MESSAGE);
        answered();
    }

    private static void collect(Component root, List<JLabel> into) {
        if (root instanceof JLabel) {
            into.add((JLabel) root);
        }
        if (root instanceof Container) {
            Component[] kids = ((Container) root).getComponents();
            for (int i = 0; i < kids.length; i++) {
                collect(kids[i], into);
            }
        }
    }

    @Test
    public void anInputDialogAnswersWithTheText() {
        whileBlocked(new Runnable() {
            @Override
            public void run() {
                JTextField t = inDialog(JTextField.class, null);
                assertEquals("the initial value", "before", t.getText());
                t.setText("hello");
                inDialog(JButton.class, "OK").doClick();
            }
        });
        assertEquals("hello", JOptionPane.showInputDialog(null, "Name?", "before"));
        answered();

        press("Cancel");
        assertNull(JOptionPane.showInputDialog("Name?"));
        answered();
    }

    @Test
    public void anInputDialogWithSelectionValuesAnswersWithTheChoice() {
        final Object[] values = {"a", "b", "c"};
        whileBlocked(new Runnable() {
            @Override
            public void run() {
                JComboBox<?> c = inDialog(JComboBox.class, null);
                assertEquals("c", c.getSelectedItem());
                c.setSelectedItem("b");
                inDialog(JButton.class, "OK").doClick();
            }
        });
        Object r = JOptionPane.showInputDialog(null, "Which?", "Pick", JOptionPane.QUESTION_MESSAGE, null, values,
                "c");
        answered();
        assertEquals("b", r);

        press("Cancel");
        assertNull(JOptionPane.showInputDialog(null, "Which?", "Pick", JOptionPane.PLAIN_MESSAGE, null, values,
                "a"));
        answered();
    }

    @Test
    public void anOptionDialogAnswersWithTheIndexOfTheOption() {
        Object[] options = {"Red", "Green", "Blue"};
        press("Green");
        int r = JOptionPane.showOptionDialog(null, "Color?", "Pick", JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, options, "Blue");
        answered();
        assertEquals(1, r);
    }

    @Test
    public void aPaneShowsComponentsAndArraysAndAnswersThroughItsValue() {
        JLabel part = new JLabel("a component");
        final JOptionPane pane = new JOptionPane(new Object[] {"a line", part}, JOptionPane.INFORMATION_MESSAGE);
        assertSame(JOptionPane.UNINITIALIZED_VALUE, pane.getValue());
        JDialog d = pane.createDialog(null, "instance");
        assertTrue("the component is in the dialog", SwingUtilities.isDescendingFrom(part, d));
        assertTrue(d.isModal());
        whileBlocked(new Runnable() {
            @Override
            public void run() {
                pane.setValue("chosen");
            }
        });
        d.setVisible(true);
        answered();
        assertFalse(d.isVisible());
        assertEquals("chosen", pane.getValue());
        d.dispose();
        pane.setMessageType(JOptionPane.WARNING_MESSAGE);
        assertEquals(JOptionPane.WARNING_MESSAGE, pane.getMessageType());
        pane.setOptions(new Object[] {"x"});
        assertArrayEquals(new Object[] {"x"}, pane.getOptions());
    }

    @Test
    public void anExtensionFilterAcceptsDirectoriesAndItsExtensions() throws IOException {
        File dir = scratch("filter");
        FileNameExtensionFilter f = new FileNameExtensionFilter("Text", "txt", "md");
        assertTrue(f.accept(dir));
        assertTrue(f.accept(new File(dir, "a.TXT")));
        assertTrue(f.accept(new File(dir, "a.md")));
        assertFalse(f.accept(new File(dir, "a.png")));
        assertFalse(f.accept(new File(dir, "txt")));
        assertEquals("Text", f.getDescription());
        assertArrayEquals(new String[] {"txt", "md"}, f.getExtensions());
    }

    private static File scratch(String name) throws IOException {
        File dir = new File("target/swing-windows-files/" + name).getAbsoluteFile();
        File sub = new File(dir, "sub");
        assertTrue(sub.isDirectory() || sub.mkdirs());
        new File(dir, "a.txt").createNewFile();
        new File(dir, "b.png").createNewFile();
        return dir;
    }

    @Test
    public void aSaveDialogListsTheDirectoryAndAnswersWithTheNamedFile() throws IOException {
        final File dir = scratch("save");
        final JFileChooser fc = new JFileChooser(dir);
        fc.setFileFilter(new FileNameExtensionFilter("Text", "txt"));
        final List<String> commands = new ArrayList<String>();
        fc.addActionListener(new com.codename1.desktopcompat.java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(com.codename1.desktopcompat.java.awt.event.ActionEvent e) {
                commands.add(e.getActionCommand());
            }
        });
        whileBlocked(new Runnable() {
            @Override
            public void run() {
                JList<?> list = inDialog(JList.class, null);
                StringBuilder names = new StringBuilder();
                for (int i = 0; i < list.getModel().getSize(); i++) {
                    names.append(list.getModel().getElementAt(i)).append(' ');
                }
                assertEquals("directories first, then the files the filter accepts", "sub/ a.txt ",
                        names.toString());
                list.setSelectedIndex(1);
                JTextField name = inDialog(JTextField.class, null);
                assertEquals("selecting a file names it", "a.txt", name.getText());
                name.setText("out.txt");
                inDialog(JButton.class, "Save").doClick();
            }
        });
        int r = fc.showSaveDialog(null);
        answered();
        assertEquals(JFileChooser.APPROVE_OPTION, r);
        assertEquals(new File(dir, "out.txt"), fc.getSelectedFile());
        assertEquals(1, fc.getSelectedFiles().length);
        assertEquals("[" + JFileChooser.APPROVE_SELECTION + "]", commands.toString());

        press("Cancel");
        assertEquals(JFileChooser.CANCEL_OPTION, fc.showSaveDialog(null));
        answered();
    }

    @Test
    public void namingADirectoryWhereOnlyFilesCanBeChosenEntersIt() throws IOException {
        final File dir = scratch("enter");
        final JFileChooser fc = new JFileChooser(dir.getPath());
        whileBlocked(new Runnable() {
            @Override
            public void run() {
                inDialog(JTextField.class, null).setText("sub");
                inDialog(JButton.class, "Save").doClick();
                assertTrue("still open", WindowHosts.active() instanceof JDialog);
                assertEquals(new File(dir, "sub"), fc.getCurrentDirectory());
                assertEquals(0, inDialog(JList.class, null).getModel().getSize());
                inDialog(JButton.class, "..").doClick();
                assertEquals(dir, fc.getCurrentDirectory());
                inDialog(JButton.class, "Cancel").doClick();
            }
        });
        assertEquals(JFileChooser.CANCEL_OPTION, fc.showSaveDialog(null));
        answered();
    }

    @Test
    public void choosingADirectoryAnswersWithTheCurrentOne() throws IOException {
        final File dir = scratch("dirs");
        JFileChooser fc = new JFileChooser(dir);
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        whileBlocked(new Runnable() {
            @Override
            public void run() {
                JList<?> list = inDialog(JList.class, null);
                assertEquals("only directories are listed", 1, list.getModel().getSize());
                inDialog(JButton.class, "Open").doClick();
            }
        });
        assertEquals(JFileChooser.APPROVE_OPTION, fc.showOpenDialog(null));
        answered();
        assertEquals(dir, fc.getSelectedFile());
    }

    @Test
    public void openingAFileAsksThePlatformPickerAndWaitsForIt() {
        final String[] asked = new String[1];
        final String[] answer = {"/docs/report.txt"};
        FilePicker.setSource(new FilePicker.Source() {
            @Override
            public void open(final ActionListener<ActionEvent> response, String accept) {
                asked[0] = accept;
                Display.getInstance().callSerially(new Runnable() {
                    @Override
                    public void run() {
                        order.add("picked");
                        response.actionPerformed(new ActionEvent(answer[0]));
                    }
                });
            }
        });
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Text", "txt", "md"));
        int r = fc.showOpenDialog(null);
        order.add("returned");
        assertEquals("[picked, returned]", order.toString());
        assertEquals(JFileChooser.APPROVE_OPTION, r);
        assertEquals("txt,md", asked[0]);
        assertEquals("report.txt", fc.getSelectedFile().getName());
        assertEquals(1, fc.getSelectedFiles().length);

        answer[0] = null;
        assertEquals(JFileChooser.CANCEL_OPTION, fc.showOpenDialog(null));
    }

    @Test
    public void theFiltersOfAChooser() {
        JFileChooser fc = new JFileChooser();
        assertSame(fc.getAcceptAllFileFilter(), fc.getFileFilter());
        FileNameExtensionFilter txt = new FileNameExtensionFilter("Text", "txt");
        fc.addChoosableFileFilter(txt);
        assertEquals(2, fc.getChoosableFileFilters().length);
        fc.setFileFilter(txt);
        assertFalse(fc.accept(new File("a.png")));
        assertTrue(fc.removeChoosableFileFilter(txt));
        assertSame(fc.getAcceptAllFileFilter(), fc.getFileFilter());
        fc.setAcceptAllFileFilterUsed(false);
        assertEquals(0, fc.getChoosableFileFilters().length);
        fc.setMultiSelectionEnabled(true);
        assertTrue(fc.isMultiSelectionEnabled());
        fc.setSelectedFiles(new File[] {new File("x"), new File("y")});
        assertEquals(new File("x"), fc.getSelectedFile());
        assertEquals(2, fc.getSelectedFiles().length);
    }

    @Test
    public void aColorDialogAnswersWithTheColor() {
        whileBlocked(new Runnable() {
            @Override
            public void run() {
                List<JSlider> sliders = new ArrayList<JSlider>();
                sliders(WindowHosts.active(), sliders);
                assertEquals(3, sliders.size());
                assertEquals("the initial color", 200, sliders.get(1).getValue());
                sliders.get(0).setValue(10);
                sliders.get(2).setValue(30);
                inDialog(JButton.class, "OK").doClick();
            }
        });
        Color c = JColorChooser.showDialog(null, "Pick", new Color(100, 200, 50));
        answered();
        assertEquals(new Color(10, 200, 30), c);

        press("Cancel");
        assertNull(JColorChooser.showDialog(null, "Pick", null));
        answered();
    }

    private static void sliders(Component root, List<JSlider> into) {
        if (root instanceof JSlider) {
            into.add((JSlider) root);
        }
        if (root instanceof Container) {
            Component[] kids = ((Container) root).getComponents();
            for (int i = 0; i < kids.length; i++) {
                sliders(kids[i], into);
            }
        }
    }

    @Test
    public void aProgressMonitorPopsUpAndCanBeCancelled() {
        JFrame f = show(new JFrame("owner"));
        ProgressMonitor m = new ProgressMonitor(f, "Working", "step 1", 0, 100);
        m.setMillisToDecideToPopup(0);
        m.setMillisToPopup(0);
        m.setProgress(10);
        assertTrue("it does not block", WindowHosts.active() instanceof JDialog);
        m.setNote("step 2");
        assertEquals("step 2", m.getNote());
        m.setProgress(50);
        assertFalse(m.isCanceled());
        inDialog(JButton.class, "Cancel").doClick();
        assertTrue(m.isCanceled());
        assertSame(f, WindowHosts.active());

        ProgressMonitor done = new ProgressMonitor(f, "Working", null, 0, 100);
        done.setMillisToDecideToPopup(0);
        done.setMillisToPopup(0);
        done.setProgress(10);
        assertTrue(WindowHosts.active() instanceof JDialog);
        done.setProgress(100);
        assertSame("reaching the maximum closes it", f, WindowHosts.active());
        assertFalse(done.isCanceled());
    }
}
