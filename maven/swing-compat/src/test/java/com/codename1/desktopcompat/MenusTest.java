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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.java.awt.event.InputEvent;
import com.codename1.desktopcompat.java.awt.event.KeyEvent;
import com.codename1.desktopcompat.java.awt.event.MouseAdapter;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.desktopcompat.javax.swing.AbstractAction;
import com.codename1.desktopcompat.javax.swing.Action;
import com.codename1.desktopcompat.javax.swing.ButtonGroup;
import com.codename1.desktopcompat.javax.swing.JButton;
import com.codename1.desktopcompat.javax.swing.JCheckBoxMenuItem;
import com.codename1.desktopcompat.javax.swing.JDialog;
import com.codename1.desktopcompat.javax.swing.JFrame;
import com.codename1.desktopcompat.javax.swing.JMenu;
import com.codename1.desktopcompat.javax.swing.JMenuBar;
import com.codename1.desktopcompat.javax.swing.JMenuItem;
import com.codename1.desktopcompat.javax.swing.JPanel;
import com.codename1.desktopcompat.javax.swing.JPopupMenu;
import com.codename1.desktopcompat.javax.swing.JRadioButtonMenuItem;
import com.codename1.desktopcompat.javax.swing.JToolBar;
import com.codename1.desktopcompat.javax.swing.KeyStroke;
import com.codename1.desktopcompat.javax.swing.MenuSelectionManager;
import com.codename1.desktopcompat.javax.swing.event.PopupMenuEvent;
import com.codename1.desktopcompat.javax.swing.event.PopupMenuListener;
import com.codename1.desktopcompat.rt.WindowHosts;
import com.codename1.ui.Command;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/// Menu bars as commands, menus drawn in a window, popup menus, tool
/// bars, and actions that keep their items in step.
public class MenusTest extends WindowsTestBase {

    private static final class Counter extends AbstractAction {
        int count;

        Counter(String name) {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            count++;
        }
    }

    private static final class Clicks implements ActionListener {
        int count;

        @Override
        public void actionPerformed(ActionEvent e) {
            count++;
        }
    }

    private static List<String> names(List<Command> cmds) {
        List<String> out = new ArrayList<String>();
        for (int i = 0; i < cmds.size(); i++) {
            out.add(cmds.get(i).getCommandName());
        }
        return out;
    }

    private static void run(Command c) {
        c.actionPerformed(new com.codename1.ui.events.ActionEvent(c));
    }

    private static final String CHECK = String.valueOf((char) 0x2713) + " ";

    @Test
    public void everyItemOfTheMenuBarOfAFrameIsOneCommandUnderItsMenu() {
        JFrame f = new JFrame("menus");
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenu edit = new JMenu("Edit");
        JMenuItem open = new JMenuItem("Open");
        JMenuItem quit = new JMenuItem("Quit");
        quit.setEnabled(false);
        JMenu recent = new JMenu("Recent");
        JMenuItem one = new JMenuItem("one.txt");
        recent.add(one);
        JCheckBoxMenuItem wrap = new JCheckBoxMenuItem("Wrap", true);
        file.add(open);
        file.add(recent);
        file.addSeparator();
        file.add(quit);
        edit.add(wrap);
        bar.add(file);
        bar.add(edit);
        Clicks opened = new Clicks();
        Clicks oneClicked = new Clicks();
        open.addActionListener(opened);
        one.addActionListener(oneClicked);
        f.setJMenuBar(bar);
        show(f);

        List<Command> cmds = f.cn1Form().cn1Commands();
        assertEquals("[Open, Recent > one.txt, Quit, " + CHECK + "Wrap]", names(cmds).toString());
        assertEquals("File", cmds.get(0).getDesktopMenu());
        assertEquals("File", cmds.get(1).getDesktopMenu());
        assertEquals("Edit", cmds.get(3).getDesktopMenu());
        assertTrue(cmds.get(0).isEnabled());
        assertFalse(cmds.get(2).isEnabled());
        assertTrue("a bridged menu bar takes no room", bar.cn1Bridged());
        assertEquals(0, bar.getHeight());
        assertEquals(f.getRootPane().getHeight(), f.getContentPane().getHeight());

        run(cmds.get(0));
        assertEquals(1, opened.count);
        run(cmds.get(1));
        assertEquals(1, oneClicked.count);

        run(cmds.get(3));
        assertFalse(wrap.isSelected());
        assertEquals("Wrap", f.cn1Form().cn1Commands().get(3).getCommandName());
    }

    @Test
    public void commandsFollowTheMenuBarAsItChanges() {
        JFrame f = new JFrame("menus");
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem open = new JMenuItem("Open");
        file.add(open);
        bar.add(file);
        f.setJMenuBar(bar);
        show(f);
        assertEquals("[Open]", names(f.cn1Form().cn1Commands()).toString());
        open.setText("Open...");
        file.add(new JMenuItem("Close"));
        assertEquals("[Open..., Close]", names(f.cn1Form().cn1Commands()).toString());
        open.setEnabled(false);
        assertFalse(f.cn1Form().cn1Commands().get(0).isEnabled());
        JMenu help = new JMenu("Help");
        help.add(new JMenuItem("About"));
        bar.add(help);
        assertEquals("Help", f.cn1Form().cn1Commands().get(2).getDesktopMenu());
        file.remove(open);
        assertEquals("[Close, About]", names(f.cn1Form().cn1Commands()).toString());
        f.setJMenuBar(null);
        assertTrue(f.cn1Form().cn1Commands().isEmpty());
    }

    @Test
    public void anActionKeepsItsItemAndItsCommandInStep() {
        JFrame f = new JFrame("menus");
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        Counter save = new Counter("Save");
        save.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        JMenuItem item = file.add(save);
        bar.add(file);
        f.setJMenuBar(bar);
        show(f);
        assertEquals("Save", item.getText());
        assertEquals(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), item.getAccelerator());
        Command cmd = f.cn1Form().cn1Commands().get(0);
        assertEquals('S', cmd.getDesktopShortcutKeyChar());
        assertEquals(Command.DESKTOP_SHORTCUT_MODIFIER_PRIMARY, cmd.getDesktopShortcutModifiers());
        run(cmd);
        assertEquals(1, save.count);

        save.setEnabled(false);
        assertFalse(item.isEnabled());
        assertFalse(f.cn1Form().cn1Commands().get(0).isEnabled());
        save.putValue(Action.NAME, "Save All");
        assertEquals("Save All", item.getText());
        assertEquals("Save All", f.cn1Form().cn1Commands().get(0).getCommandName());
        save.setEnabled(true);
        assertTrue(f.cn1Form().cn1Commands().get(0).isEnabled());
    }

    @Test
    public void anAcceleratorRunsItsItemWhileTheWindowHasTheFocus() {
        JFrame f = new JFrame("menus");
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem open = new JMenuItem("Open");
        open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        Clicks clicks = new Clicks();
        open.addActionListener(clicks);
        file.add(open);
        bar.add(file);
        f.setJMenuBar(bar);
        JButton b = new JButton("focus");
        f.add(b, BorderLayout.CENTER);
        show(f);
        b.requestFocusInWindow();
        type(f, 'o');
        assertEquals(0, clicks.count);
        input.control();
        type(f, 'o');
        assertEquals(1, clicks.count);
        open.setEnabled(false);
        type(f, 'o');
        assertEquals(1, clicks.count);
    }

    @Test
    public void onADesktopTheCommandsGoToTheWindowOfTheFrame() {
        FakeDesktop desktop = new FakeDesktop();
        WindowHosts.setSecondaryWindows(desktop);
        show(new JFrame("first"));
        JFrame second = new JFrame("second");
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        Clicks clicks = new Clicks();
        JMenuItem open = new JMenuItem("Open");
        open.addActionListener(clicks);
        file.add(open);
        bar.add(file);
        second.setJMenuBar(bar);
        second.setSize(300, 200);
        second.setVisible(true);
        FakeHost host = desktop.hosts.get(0);
        assertEquals("[Open]", names(host.commands).toString());
        assertEquals("File", host.commands.get(0).getDesktopMenu());
        run(host.commands.get(0));
        assertEquals(1, clicks.count);
        file.add(new JMenuItem("Close"));
        assertEquals("[Open, Close]", names(host.commands).toString());
    }

    @Test
    public void inADialogTheMenuBarIsDrawnAndItsMenusOpenPopups() {
        JFrame f = show(new JFrame("owner"));
        JDialog d = new JDialog(f, "dialog");
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem open = new JMenuItem("Open");
        Clicks clicks = new Clicks();
        open.addActionListener(clicks);
        file.add(open);
        JMenu more = new JMenu("More");
        JMenuItem deep = new JMenuItem("Deep");
        Clicks deepClicks = new Clicks();
        deep.addActionListener(deepClicks);
        more.add(deep);
        file.add(more);
        bar.add(file);
        d.setJMenuBar(bar);
        d.add(new JPanel(), BorderLayout.CENTER);
        d.setSize(300, 300);
        d.setVisible(true);
        assertFalse(bar.cn1Bridged());
        assertTrue(bar.getHeight() > 0);
        assertEquals(bar.getHeight(), d.getContentPane().getY());

        assertFalse(file.isPopupMenuVisible());
        click(d, file, 3, 3);
        assertTrue(file.isPopupMenuVisible());
        assertTrue(file.isSelected());
        assertSame(d.getLayeredPane(), file.getPopupMenu().getParent());
        assertEquals(0, d.getLayeredPane().getIndexOf(file.getPopupMenu()));
        assertTrue(file.getPopupMenu().getHeight() > 0);

        click(d, more, 3, 3);
        assertTrue(more.isPopupMenuVisible());
        assertTrue("the parent menu stays open", file.isPopupMenuVisible());
        assertEquals(4, MenuSelectionManager.defaultManager().getSelectedPath().length);

        click(d, deep, 3, 3);
        assertEquals(1, deepClicks.count);
        assertFalse(more.isPopupMenuVisible());
        assertFalse(file.isPopupMenuVisible());
        assertFalse(file.isSelected());
        assertNull(file.getPopupMenu().getParent());

        click(d, file, 3, 3);
        click(d, open, 3, 3);
        assertEquals(1, clicks.count);
        click(d, file, 3, 3);
        assertTrue(file.isPopupMenuVisible());
        click(d, file, 3, 3);
        assertFalse("a second click closes the menu", file.isPopupMenuVisible());
    }

    @Test
    public void aPopupMenuOpensOnThePopupTriggerAndAPressOutsideClosesIt() {
        JFrame f = new JFrame("popup");
        JPanel p = new JPanel();
        final int[] presses = new int[1];
        p.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                presses[0]++;
            }
        });
        f.add(p, BorderLayout.CENTER);
        JPopupMenu menu = new JPopupMenu();
        Clicks copy = new Clicks();
        JMenuItem copyItem = menu.add("Copy");
        copyItem.addActionListener(copy);
        menu.addSeparator();
        menu.add(new JMenuItem("Paste"));
        final List<String> log = new ArrayList<String>();
        menu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                log.add("visible");
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                log.add("invisible");
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                log.add("canceled");
            }
        });
        p.setComponentPopupMenu(menu);
        show(f);

        click(f, p, 40, 50);
        assertFalse(menu.isVisible());
        input.button = 3;
        click(f, p, 40, 50);
        assertTrue(menu.isVisible());
        assertSame(p, menu.getInvoker());
        assertEquals("[visible]", log.toString());
        assertEquals(40, menu.getX() - p.getX());
        Dimension pref = menu.getPreferredSize();
        assertEquals(pref.height, menu.getHeight());
        assertTrue(MenuSelectionManager.defaultManager().isComponentPartOfCurrentMenu(copyItem));

        input.button = 1;
        int before = presses[0];
        click(f, p, 5, 5);
        assertFalse(menu.isVisible());
        assertEquals("the press that closes a popup is not delivered", before, presses[0]);
        assertEquals("[visible, invisible]", log.toString());

        input.button = 3;
        click(f, p, 40, 50);
        input.button = 1;
        click(f, copyItem, 3, 3);
        assertEquals(1, copy.count);
        assertFalse(menu.isVisible());

        menu.show(p, 10, 10);
        assertTrue(menu.isVisible());
        type(f, 27);
        assertFalse("escape closes the popup", menu.isVisible());
    }

    @Test
    public void aLongPressOfAFingerIsAPopupTrigger() {
        JFrame f = new JFrame("popup");
        JPanel p = new JPanel();
        final int[] clicked = new int[1];
        p.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                clicked[0]++;
            }
        });
        f.add(p, BorderLayout.CENTER);
        JPopupMenu menu = new JPopupMenu();
        menu.add("Copy");
        p.setComponentPopupMenu(menu);
        show(f);
        input.touch = true;
        int[] at = at(p, 30, 30);
        f.cn1Form().pointerPressed(at[0], at[1]);
        assertFalse(menu.isVisible());
        f.cn1Form().longPointerPress(at[0], at[1]);
        assertTrue(menu.isVisible());
        f.cn1Form().pointerReleased(at[0], at[1]);
        assertTrue("the release keeps the menu open", menu.isVisible());
        assertEquals("a long press is not a click", 0, clicked[0]);
    }

    @Test
    public void thePopupMenuOfAParentIsInheritedOnlyWhenAsked() {
        JPanel parent = new JPanel();
        JPanel child = new JPanel();
        parent.add(child);
        JPopupMenu menu = new JPopupMenu();
        parent.setComponentPopupMenu(menu);
        assertNull(child.getComponentPopupMenu());
        child.setInheritsPopupMenu(true);
        assertSame(menu, child.getComponentPopupMenu());
    }

    @Test
    public void radioButtonItemsOfAGroupSelectOneAndMarkItsCommand() {
        JFrame f = new JFrame("menus");
        JMenuBar bar = new JMenuBar();
        JMenu view = new JMenu("View");
        JRadioButtonMenuItem small = new JRadioButtonMenuItem("Small", true);
        JRadioButtonMenuItem large = new JRadioButtonMenuItem("Large");
        ButtonGroup g = new ButtonGroup();
        g.add(small);
        g.add(large);
        view.add(small);
        view.add(large);
        bar.add(view);
        f.setJMenuBar(bar);
        show(f);
        assertEquals("[" + CHECK + "Small, Large]", names(f.cn1Form().cn1Commands()).toString());
        run(f.cn1Form().cn1Commands().get(1));
        assertTrue(large.isSelected());
        assertFalse(small.isSelected());
        assertEquals("[Small, " + CHECK + "Large]", names(f.cn1Form().cn1Commands()).toString());
    }

    @Test
    public void aToolBarButtonRunsItsActionAndFollowsIt() {
        JFrame f = new JFrame("tools");
        JToolBar bar = new JToolBar();
        Counter run = new Counter("Run");
        run.putValue(Action.SHORT_DESCRIPTION, "Runs it");
        JButton b = bar.add(run);
        bar.addSeparator();
        bar.add(new JButton("Stop"));
        f.add(bar, BorderLayout.NORTH);
        show(f);
        assertEquals("Run", b.getText());
        assertEquals("Runs it", b.getToolTipText());
        assertEquals(3, bar.getComponentCount());
        assertTrue(bar.getHeight() > 0);
        assertTrue(bar.getComponent(2).getX() > b.getX() + b.getWidth());
        click(f, b, 3, 3);
        assertEquals(1, run.count);
        run.setEnabled(false);
        assertFalse(b.isEnabled());
        run.putValue(Action.NAME, "Go");
        assertEquals("Go", b.getText());
        bar.setOrientation(JToolBar.VERTICAL);
        f.validate();
        assertTrue(bar.getComponent(2).getY() > b.getY());
    }

    /// A menu is not part of the keyboard focus. The widget that draws
    /// one in the window took the first focus of the form, and the last
    /// menu of the bar was drawn as chosen from the moment the window
    /// opened; the first focus is the content's, as in Swing.
    @Test
    public void noMenuOfABarInTheWindowHasTheFocusWhenTheWindowOpens() {
        com.codename1.compat.testing.HeadlessImplementation.setDesktop(true);
        try {
            JFrame f = new JFrame("menus");
            JMenuBar bar = new JMenuBar();
            String[] names = {"File", "View", "Help"};
            JMenu[] menus = new JMenu[3];
            for (int i = 0; i < 3; i++) {
                menus[i] = new JMenu(names[i]);
                menus[i].add(new JMenuItem("Item"));
                bar.add(menus[i]);
            }
            f.setJMenuBar(bar);
            JButton go = new JButton("Go");
            f.add(go, BorderLayout.CENTER);
            f.setSize(300, 200);
            show(f);
            paint(f);
            assertFalse("the bar is drawn in the window", bar.cn1Bridged());
            for (int i = 0; i < 3; i++) {
                com.codename1.ui.Component p = menus[i].cn1Peer();
                assertFalse(names[i], menus[i].isFocusable());
                assertFalse(names[i], p.isFocusable());
                assertFalse(names[i], p.hasFocus());
                assertSame(names[i], p.getUnselectedStyle(), p.getStyle());
            }
            assertSame(go.cn1Peer(), f.cn1Form().getFocused());
            // A menu still opens, and its items are still chosen.
            click(f, menus[2], 3, 3);
            assertTrue(menus[2].isPopupMenuVisible());
            assertFalse(menus[2].cn1Peer().hasFocus());
            MenuSelectionManager.defaultManager().clearSelectedPath();
        } finally {
            com.codename1.compat.testing.HeadlessImplementation.setDesktop(false);
        }
    }

    /// What is set on a component after its widget was made reaches the
    /// widget, and so does what is taken back.
    @Test
    public void aComponentMadeNotFocusableIsNotOfferedTheFocus() {
        JFrame f = new JFrame("focus");
        JButton b = new JButton("Go");
        f.add(b, BorderLayout.CENTER);
        show(f);
        assertTrue(b.cn1Peer().isFocusable());
        b.setFocusable(false);
        assertFalse(b.cn1Peer().isFocusable());
        b.setFocusable(true);
        assertTrue(b.cn1Peer().isFocusable());
        JButton never = new JButton("Never");
        never.setFocusable(false);
        assertFalse(never.cn1Peer().isFocusable());
    }
}
