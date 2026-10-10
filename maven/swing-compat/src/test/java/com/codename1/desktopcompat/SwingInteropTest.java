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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Window;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.javax.swing.JButton;
import com.codename1.desktopcompat.javax.swing.JFrame;
import com.codename1.desktopcompat.javax.swing.JLabel;
import com.codename1.desktopcompat.javax.swing.JMenu;
import com.codename1.desktopcompat.javax.swing.JMenuBar;
import com.codename1.desktopcompat.javax.swing.JMenuItem;
import com.codename1.desktopcompat.javax.swing.JOptionPane;
import com.codename1.desktopcompat.javax.swing.JPanel;
import com.codename1.desktopcompat.javax.swing.SwingUtilities;
import com.codename1.desktopcompat.javax.swing.WindowConstants;
import com.codename1.desktopcompat.rt.DialogForm;
import com.codename1.desktopcompat.rt.FrameForm;
import com.codename1.desktopcompat.rt.Units;
import com.codename1.desktopcompat.rt.WindowHosts;
import com.codename1.ui.Command;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Toolbar;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/// Swing inside a Codename One application that has a main class of its
/// own (library mode): a component tree embedded in a form of the
/// application, windows shown over its forms and closed back to them, and
/// the menu bar of a frame reachable from the form's toolbar. No lifecycle
/// of the layer runs in any of these, and none installs an exit hook: the
/// application must survive every one of them.
public class SwingInteropTest extends WindowsTestBase {

    private static final class Clicks implements ActionListener {
        int count;

        @Override
        public void actionPerformed(ActionEvent e) {
            count++;
        }
    }

    private static Form home(String title) {
        Form f = new Form(title, new com.codename1.ui.layouts.BorderLayout());
        f.show();
        return f;
    }

    private static List<Object[]> paint(Form f) {
        HeadlessImplementation.drawnText.clear();
        HeadlessImplementation.recordText = true;
        try {
            Image target = Image.createImage(HeadlessImplementation.WIDTH, HeadlessImplementation.HEIGHT);
            f.paintComponent(target.getGraphics());
            return new ArrayList<Object[]>(HeadlessImplementation.drawnText);
        } finally {
            HeadlessImplementation.recordText = false;
            HeadlessImplementation.drawnText.clear();
        }
    }

    private static void click(Form f, com.codename1.desktopcompat.java.awt.Component c, int x, int y) {
        int[] p = at(c, x, y);
        f.pointerPressed(p[0], p[1]);
        f.pointerReleased(p[0], p[1]);
    }

    @Test
    public void aSwingTreeIsAComponentOfACodenameOneForm() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("Embedded label");
        JButton button = new JButton("Press");
        Clicks clicks = new Clicks();
        button.addActionListener(clicks);
        panel.add(label, BorderLayout.NORTH);
        panel.add(button, BorderLayout.CENTER);

        com.codename1.ui.Component hosted = SwingInterop.asComponent(panel);
        Dimension wanted = panel.getPreferredSize();
        assertEquals("the preferred size is the tree's, in device pixels", Units.toDevice(wanted.width),
                hosted.getPreferredW());
        assertEquals(Units.toDevice(wanted.height), hosted.getPreferredH());

        Form form = home("Host");
        form.add(com.codename1.ui.layouts.BorderLayout.CENTER, hosted);
        form.revalidate();
        assertSame(form, Display.getInstance().getCurrent());
        assertTrue(hosted.getWidth() > 0 && hosted.getHeight() > 0);
        assertEquals("laid out to the size Codename One gave the component", Units.toLogical(hosted.getWidth()),
                panel.getWidth());
        assertEquals(Units.toLogical(hosted.getHeight()), panel.getHeight());
        assertTrue(button.getWidth() > 0 && button.getHeight() > 0);
        assertTrue(panel.isShowing());

        List<Object[]> text = paint(form);
        assertNotNull("the tree paints with the form", find(text, "Embedded label"));
        assertNotNull(find(text, "Press"));

        click(form, button, 5, 5);
        assertEquals("pointer input on the form reaches the tree", 1, clicks.count);

        // The tree has a window to answer for it, which is not one of the
        // application's windows and cannot be closed from inside.
        Window w = SwingUtilities.getWindowAncestor(button);
        assertNotNull(w);
        assertTrue(w.cn1Embedded());
        assertSame(form, w.cn1HostForm());
        assertEquals(0, Window.getWindows().length);
        assertEquals(0, WindowHosts.showing().length);
        assertTrue(w.isActive());
        w.dispose();
        w.setVisible(false);
        assertTrue(panel.isShowing());
        click(form, button, 5, 5);
        assertEquals(2, clicks.count);

        // Off the screen it takes no more input, and takes it again later.
        Form other = home("Other");
        assertFalse(w.isActive());
        form.show();
        click(form, button, 5, 5);
        assertEquals(3, clicks.count);
        assertSame(other, other.getComponentForm());
    }

    @Test
    public void onlyAComponentThatIsNotAWindowIsEmbedded() {
        try {
            SwingInterop.asComponent(new JFrame("no"));
            fail("a window is shown, not embedded");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("setVisible(true)"));
        }
        try {
            SwingInterop.asComponent("text");
            fail();
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("java.lang.String"));
        }
        try {
            SwingInterop.asComponent(null);
            fail();
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("null"));
        }
    }

    @Test
    public void aMenuBarInAnEmbeddedTreeIsDrawnAndItsMenusOpen() {
        JPanel panel = new JPanel(new BorderLayout());
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem open = new JMenuItem("Open");
        Clicks clicks = new Clicks();
        open.addActionListener(clicks);
        file.add(open);
        bar.add(file);
        panel.add(bar, BorderLayout.NORTH);
        panel.add(new JLabel("body"), BorderLayout.CENTER);
        Form form = home("Host");
        form.add(com.codename1.ui.layouts.BorderLayout.CENTER, SwingInterop.asComponent(panel));
        form.revalidate();

        assertFalse(bar.cn1Bridged());
        assertTrue(bar.getHeight() > 0);
        assertNotNull(find(paint(form), "File"));
        click(form, file, 3, 3);
        assertTrue(file.isPopupMenuVisible());
        click(form, open, 3, 3);
        assertEquals(1, clicks.count);
        assertFalse(file.isPopupMenuVisible());
    }

    @Test
    public void aFrameShownFromACodenameOneFormClosesBackToIt() {
        Form home = home("Home");
        JFrame f = new JFrame("Swing screen");
        f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        f.add(new JLabel("content"));
        f.setVisible(true);
        FrameForm shown = f.cn1Form();
        assertSame(shown, Display.getInstance().getCurrent());
        Command back = shown.getBackCommand();
        assertNotNull("a frame shown over a form of the application has a way back", back);

        back.actionPerformed(new com.codename1.ui.events.ActionEvent(back));
        MainThreadRule.drain();
        assertSame("closing the last Swing window shows the form it was shown over", home,
                Display.getInstance().getCurrent());
        assertEquals(0, Window.getWindows().length);
        assertFalse(f.isDisplayable());
    }

    @Test
    public void exitOnCloseClosesTheSwingWindowsAndLeavesTheApplicationRunning() {
        Form home = home("Home");
        JFrame first = new JFrame("first");
        first.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        first.setVisible(true);
        JFrame second = new JFrame("second");
        second.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        second.setVisible(true);
        assertSame(second.cn1Form(), Display.getInstance().getCurrent());
        second.cn1Closing();
        assertSame(first.cn1Form(), Display.getInstance().getCurrent());
        second.setVisible(true);

        // No lifecycle of the layer runs, so no exit hook is installed.
        first.cn1Closing();
        MainThreadRule.drain();
        assertSame(home, Display.getInstance().getCurrent());
        assertEquals(0, Window.getWindows().length);
        assertEquals(0, WindowHosts.showing().length);
        assertFalse(first.isVisible());
        assertFalse(second.isVisible());
        assertTrue("the display is still up", Display.isInitialized());
    }

    @Test
    public void aLabelEndsInAnEllipsisWhateverTheThemeSays() {
        com.codename1.ui.plaf.LookAndFeel laf = com.codename1.ui.plaf.UIManager.getInstance().getLookAndFeel();
        boolean before = laf.isDefaultEndsWith3Points();
        laf.setDefaultEndsWith3Points(false);
        try {
            com.codename1.ui.Component peer = new JLabel("Text with no room").cn1Peer();
            assertTrue(peer instanceof com.codename1.ui.Label);
            assertTrue(((com.codename1.ui.Label) peer).isEndsWith3Points());
        } finally {
            laf.setDefaultEndsWith3Points(before);
        }
    }

    @Test
    public void aModalDialogFloatsOverTheFormOfTheApplicationAndReturnsToIt() {
        final Form home = home("Home");
        final JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("asks"));
        home.add(com.codename1.ui.layouts.BorderLayout.CENTER, SwingInterop.asComponent(panel));
        home.revalidate();
        final boolean[] floated = new boolean[1];
        final boolean[] ownWindow = new boolean[1];
        // A timer, not callSerially: showing a dialog first runs what is
        // waiting on the event dispatch thread, before the dialog is up.
        Display.getInstance().setTimeout(150, new Runnable() {
            @Override
            public void run() {
                Window top = WindowHosts.active();
                JOptionPane pane = top == null ? null : find(top);
                if (pane == null) {
                    Display.getInstance().setTimeout(50, this);
                    return;
                }
                Form current = Display.getInstance().getCurrent();
                floated[0] = current instanceof DialogForm;
                ownWindow[0] = current instanceof DialogForm && ((DialogForm) current).isNativeWindowMode();
                pane.setValue(Integer.valueOf(JOptionPane.YES_OPTION));
            }
        });
        // A theme may ask for every Codename One dialog to open in a
        // window of its own, as the Windows native theme does. A dialog
        // the layer keeps in the application's form is not one of them.
        boolean before = com.codename1.ui.Dialog.isDefaultNativeWindowMode();
        com.codename1.ui.Dialog.setDefaultNativeWindowMode(true);
        int answer;
        try {
            answer = JOptionPane.showConfirmDialog(panel, "Proceed?", "Confirm", JOptionPane.YES_NO_OPTION);
        } finally {
            com.codename1.ui.Dialog.setDefaultNativeWindowMode(before);
        }
        assertTrue("the dialog floated over the application's form", floated[0]);
        assertFalse("the dialog asked for a window of its own", ownWindow[0]);
        assertEquals(JOptionPane.YES_OPTION, answer);
        MainThreadRule.drain();
        assertSame(home, Display.getInstance().getCurrent());
        assertEquals(0, WindowHosts.showing().length);
    }

    private static com.codename1.ui.Component named(com.codename1.ui.Container root, String name) {
        for (int i = 0; i < root.getComponentCount(); i++) {
            com.codename1.ui.Component c = root.getComponentAt(i);
            if (name.equals(c.getName())) {
                return c;
            }
            if (c instanceof com.codename1.ui.Container) {
                com.codename1.ui.Component found = named((com.codename1.ui.Container) c, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static JOptionPane find(com.codename1.desktopcompat.java.awt.Component c) {
        if (c instanceof JOptionPane) {
            return (JOptionPane) c;
        }
        if (c instanceof com.codename1.desktopcompat.java.awt.Container) {
            com.codename1.desktopcompat.java.awt.Container k = (com.codename1.desktopcompat.java.awt.Container) c;
            for (int i = 0; i < k.getComponentCount(); i++) {
                JOptionPane found = find(k.getComponent(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /// The menus of a frame are reachable on a host without a menu bar of
    /// its own: every item is a command in the overflow menu of the form's
    /// toolbar, and choosing the command there fires the item's listeners.
    @Test
    public void theMenuBarOfAFrameIsInTheOverflowMenuOfItsFormsToolbar() {
        JFrame f = new JFrame("menus");
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenu help = new JMenu("Help");
        JMenuItem open = new JMenuItem("Open");
        JMenuItem about = new JMenuItem("About");
        Clicks opened = new Clicks();
        Clicks abouts = new Clicks();
        open.addActionListener(opened);
        about.addActionListener(abouts);
        file.add(open);
        help.add(about);
        bar.add(file);
        bar.add(help);
        f.setJMenuBar(bar);
        show(f);

        FrameForm form = f.cn1Form();
        Toolbar tb = form.getToolbar();
        assertNotNull("the form of a frame with a menu bar has a toolbar", tb);
        List<Command> overflow = new ArrayList<Command>();
        Iterable<Command> listed = tb.getOverflowCommands();
        assertNotNull(listed);
        for (Command c : listed) {
            overflow.add(c);
        }
        assertEquals(2, overflow.size());
        assertEquals("Open", overflow.get(0).getCommandName());
        assertEquals("About", overflow.get(1).getCommandName());
        assertEquals("File", overflow.get(0).getDesktopMenu());
        assertEquals("Help", overflow.get(1).getDesktopMenu());
        // Looked up in the form, not through Toolbar.getOverflowButton():
        // the toolbar makes a button per command added and shows the first.
        com.codename1.ui.Component button = named(form, "OverflowButton");
        assertNotNull("the button that opens the overflow menu is on the toolbar", button);
        assertSame(form, button.getComponentForm());
        assertTrue(button.isVisible());

        // As the overflow menu does when its entry is chosen.
        form.dispatchCommand(overflow.get(0), new com.codename1.ui.events.ActionEvent(overflow.get(0)));
        assertEquals(1, opened.count);
        assertEquals(0, abouts.count);
        form.dispatchCommand(overflow.get(1), new com.codename1.ui.events.ActionEvent(overflow.get(1)));
        assertEquals(1, abouts.count);

        // A menu added later is there too, and a removed bar leaves nothing.
        JMenu view = new JMenu("View");
        view.add(new JMenuItem("Zoom"));
        bar.add(view);
        int n = 0;
        for (Command c : tb.getOverflowCommands()) {
            n++;
            assertNotNull(c);
        }
        assertEquals(3, n);
        f.setJMenuBar(null);
        n = 0;
        for (Command c : tb.getOverflowCommands()) {
            n++;
            assertNull(c);
        }
        assertEquals(0, n);
    }
}
