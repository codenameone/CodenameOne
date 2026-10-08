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
package com.codenameone.examples.hellocodenameone.desktop;

import com.codename1.desktopcompat.SwingInterop;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Calendar;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.jdesktop.swingx.JXBusyLabel;
import org.jdesktop.swingx.JXDatePicker;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;
import org.jdesktop.swingx.decorator.HighlighterFactory;

/// The Swing screens of the desktop compatibility screenshot tests.
///
/// This class is ordinary Swing, compiled against the JDK's own classes; the
/// build relocates it onto the Swing layer. Each scene is built here and
/// handed to the application as a Codename One component, which is all the
/// tests in `tests/desktopcompat` ever see of Swing.
///
/// Everything a scene shows is fixed: no clock, no animation, no focused
/// text field.
public final class SwingScenes {

    private static final String[] NAMES = {"Mercury", "Venus", "Earth", "Mars", "Jupiter", "Saturn"};
    private static final int[] MOONS = {0, 0, 1, 2, 95, 146};
    private static final double[] MASS = {0.055, 0.815, 1.0, 0.107, 317.8, 95.2};

    private SwingScenes() {
    }

    private static com.codename1.ui.Component host(JComponent content) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        root.setBackground(Color.WHITE);
        root.add(content, BorderLayout.CENTER);
        return SwingInterop.asComponent(root);
    }

    private static GridBagConstraints at(int x, int y, int width, double weight) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.gridwidth = width;
        c.weightx = weight;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(4, 4, 4, 4);
        return c;
    }

    /// Scene (a): the basic controls in a `GridBagLayout`.
    public static com.codename1.ui.Component controls() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        int row = 0;
        form.add(new JLabel("Name:"), at(0, row, 1, 0));
        form.add(new JTextField("Ada Lovelace", 12), at(1, row++, 2, 1));
        form.add(new JLabel("Password:"), at(0, row, 1, 0));
        form.add(new JPasswordField("secret", 12), at(1, row++, 2, 1));

        JCheckBox remember = new JCheckBox("Remember me", true);
        remember.setOpaque(false);
        JCheckBox offline = new JCheckBox("Work offline");
        offline.setOpaque(false);
        form.add(remember, at(0, row, 2, 0));
        form.add(offline, at(2, row++, 1, 1));

        JRadioButton small = new JRadioButton("Small");
        JRadioButton medium = new JRadioButton("Medium", true);
        JRadioButton large = new JRadioButton("Large");
        ButtonGroup sizes = new ButtonGroup();
        JPanel radios = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        radios.setOpaque(false);
        for (JRadioButton b : new JRadioButton[] {small, medium, large}) {
            b.setOpaque(false);
            sizes.add(b);
            radios.add(b);
        }
        form.add(new JLabel("Size:"), at(0, row, 1, 0));
        form.add(radios, at(1, row++, 2, 1));

        JComboBox<String> planet = new JComboBox<String>(NAMES);
        planet.setSelectedIndex(2);
        form.add(new JLabel("Planet:"), at(0, row, 1, 0));
        form.add(planet, at(1, row++, 2, 1));

        JSlider volume = new JSlider(0, 100, 35);
        volume.setOpaque(false);
        form.add(new JLabel("Volume:"), at(0, row, 1, 0));
        form.add(volume, at(1, row++, 2, 1));

        JProgressBar progress = new JProgressBar(0, 100);
        progress.setValue(60);
        progress.setStringPainted(true);
        form.add(new JLabel("Progress:"), at(0, row, 1, 0));
        form.add(progress, at(1, row++, 2, 1));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.setOpaque(false);
        JButton disabled = new JButton("Disabled");
        disabled.setEnabled(false);
        buttons.add(disabled);
        buttons.add(new JButton("Cancel"));
        buttons.add(new JButton("OK"));
        form.add(buttons, at(0, row++, 3, 1));

        GridBagConstraints rest = at(0, row, 3, 1);
        rest.weighty = 1;
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        form.add(filler, rest);
        return host(form);
    }

    /// The planets as a table model: a name, a moon count and a mass.
    private static final class Planets extends AbstractTableModel {
        private static final String[] COLUMNS = {"Planet", "Moons", "Mass"};

        @Override
        public int getRowCount() {
            return NAMES.length;
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return column == 0 ? String.class : column == 1 ? Integer.class : Double.class;
        }

        @Override
        public Object getValueAt(int row, int column) {
            if (column == 0) {
                return NAMES[row];
            }
            if (column == 1) {
                return Integer.valueOf(MOONS[row]);
            }
            return Double.valueOf(MASS[row]);
        }
    }

    /// Right aligned, the giants in bold on a tinted cell.
    private static final class MassRenderer extends DefaultTableCellRenderer {
        MassRenderer() {
            setHorizontalAlignment(SwingConstants.RIGHT);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            double mass = value instanceof Number ? ((Number) value).doubleValue() : 0;
            setText(mass + " M");
            boolean giant = mass > 10;
            setFont(getFont().deriveFont(giant ? Font.BOLD : Font.PLAIN));
            setForeground(giant ? new Color(0x8a3b00) : Color.BLACK);
            setBackground(giant ? new Color(0xfff1e0) : Color.WHITE);
            return this;
        }
    }

    private static JPanel titled(String title, Component content) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.add(content, BorderLayout.CENTER);
        return p;
    }

    /// Scene (b): a `JTable` with a custom renderer over a striped
    /// `JXTable`.
    public static com.codename1.ui.Component tables() {
        JTable plain = new JTable(new Planets());
        plain.setRowHeight(22);
        plain.getColumnModel().getColumn(2).setCellRenderer(new MassRenderer());

        JXTable striped = new JXTable(new Planets());
        striped.setHighlighters(HighlighterFactory.createSimpleStriping());
        striped.setRowHeight(22);

        JPanel both = new JPanel(new GridLayout(2, 1, 0, 8));
        both.setOpaque(false);
        both.add(titled("JTable, custom renderer", new JScrollPane(plain)));
        both.add(titled("JXTable, striped", new JScrollPane(striped)));
        return host(both);
    }

    /// A planet with its moons, the giants in bold, odd rows tinted.
    private static final class PlanetRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            int moons = index >= 0 && index < MOONS.length ? MOONS[index] : 0;
            setText(value + "  -  " + moons + (moons == 1 ? " moon" : " moons"));
            setFont(getFont().deriveFont(moons > 10 ? Font.BOLD : Font.PLAIN));
            if (!isSelected) {
                setBackground(index % 2 == 1 ? new Color(0xf2f6fa) : Color.WHITE);
            }
            return this;
        }
    }

    /// Scene (c): an expanded `JTree` over a `JList` with a custom
    /// renderer.
    public static com.codename1.ui.Component trees() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Solar system");
        DefaultMutableTreeNode rocky = new DefaultMutableTreeNode("Rocky planets");
        DefaultMutableTreeNode giants = new DefaultMutableTreeNode("Giants");
        root.add(rocky);
        root.add(giants);
        for (int i = 0; i < NAMES.length; i++) {
            (MASS[i] < 10 ? rocky : giants).add(new DefaultMutableTreeNode(NAMES[i]));
        }
        JTree tree = new JTree(new DefaultTreeModel(root));
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }

        DefaultListModel<String> model = new DefaultListModel<String>();
        for (String name : NAMES) {
            model.addElement(name);
        }
        JList<String> list = new JList<String>(model);
        list.setCellRenderer(new PlanetRenderer());
        list.setSelectedIndex(4);

        JPanel both = new JPanel(new GridLayout(2, 1, 0, 8));
        both.setOpaque(false);
        both.add(titled("JTree", new JScrollPane(tree)));
        both.add(titled("JList, custom renderer", new JScrollPane(list)));
        return host(both);
    }

    /// Scene (d): everything painted by hand.
    private static final class Painting extends JComponent {
        private final BufferedImage checker = checker();

        private static BufferedImage checker() {
            BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    g.setColor((x + y) % 2 == 0 ? new Color(0x355c7d) : new Color(0xf8b195));
                    g.fillRect(x * 8, y * 8, 8, 8);
                }
            }
            g.dispose();
            return image;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g.setPaint(new GradientPaint(0, 0, new Color(0xfdfbfb), 0, h, new Color(0xc9d6e6)));
            g.fillRect(0, 0, w, h);

            g.setColor(new Color(0x6c5b7b));
            g.fill(new Ellipse2D.Double(16, 16, 90, 60));
            g.setColor(new Color(0xc06c84));
            g.fill(new RoundRectangle2D.Double(122, 16, 90, 60, 18, 18));
            Path2D triangle = new Path2D.Double();
            triangle.moveTo(273, 16);
            triangle.lineTo(318, 76);
            triangle.lineTo(228, 76);
            triangle.closePath();
            g.setColor(new Color(0xf67280));
            g.fill(triangle);

            g.setColor(Color.DARK_GRAY);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f,
                    new float[] {8f, 6f}, 0f));
            g.draw(new Line2D.Double(16, 96, 318, 96));
            g.setStroke(new BasicStroke(3f));
            g.draw(new Ellipse2D.Double(16, 16, 90, 60));

            g.setPaint(new GradientPaint(16, 116, new Color(0x11998e), 166, 116, new Color(0x38ef7d)));
            g.fill(new RoundRectangle2D.Double(16, 116, 150, 44, 12, 12));

            AffineTransform saved = g.getTransform();
            g.rotate(Math.toRadians(-20), 250, 150);
            g.setColor(new Color(0x355c7d));
            g.fillRect(200, 126, 100, 44);
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
            g.drawString("Rotated", 216, 154);
            g.setTransform(saved);

            g.drawImage(checker, 16, 190, null);
            g.drawImage(checker, 96, 190, 32, 32, null);

            String text = "Graphics2D";
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
            FontMetrics metrics = g.getFontMetrics();
            int baseline = 200 + metrics.getAscent();
            g.setColor(new Color(0x355c7d));
            g.drawString(text, 150, baseline);
            g.setStroke(new BasicStroke(2f));
            g.drawLine(150, baseline + 3, 150 + metrics.stringWidth(text), baseline + 3);
            g.dispose();
        }
    }

    /// Scene (d): shapes, a gradient, a dashed stroke, rotated text and a
    /// `BufferedImage`, painted by a component of the application's own.
    public static com.codename1.ui.Component painting() {
        Painting p = new Painting();
        p.setPreferredSize(new Dimension(334, 280));
        return host(p);
    }

    /// Scene (e): a `JTabbedPane` whose tab holds a `JSplitPane` of
    /// panels with titled borders.
    public static com.codename1.ui.Component containers() {
        JPanel top = new JPanel(new GridLayout(3, 1, 0, 2));
        top.setBorder(BorderFactory.createTitledBorder("Account"));
        top.add(new JLabel("Owner: Ada Lovelace"));
        top.add(new JLabel("Plan: Analytical"));
        top.add(new JLabel("Seats: 12"));

        JPanel bottom = new JPanel(new BorderLayout(0, 4));
        bottom.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(0x355c7d), 2),
                "Notes"));
        bottom.add(new JLabel("The split pane divides this tab."), BorderLayout.NORTH);
        bottom.add(new JButton("Add note"), BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
        split.setDividerLocation(120);
        split.setContinuousLayout(false);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Overview", split);
        tabs.addTab("History", new JLabel("Nothing yet", SwingConstants.CENTER));
        tabs.addTab("Settings", new JLabel("No settings", SwingConstants.CENTER));
        tabs.setSelectedIndex(0);
        return host(tabs);
    }

    /// Scene (g): SwingX task panes, a date picker on a fixed date and a
    /// busy label that is not busy.
    public static com.codename1.ui.Component swingx() {
        JXTaskPaneContainer tasks = new JXTaskPaneContainer();
        JXTaskPane files = new JXTaskPane();
        files.setTitle("File tasks");
        files.add(new JLabel("Rename this file"));
        files.add(new JLabel("Move this file"));
        tasks.add(files);
        JXTaskPane details = new JXTaskPane();
        details.setTitle("Details");
        details.add(new JLabel("6 planets"));
        tasks.add(details);
        JXTaskPane closed = new JXTaskPane();
        closed.setTitle("Collapsed");
        closed.setAnimated(false);
        closed.setCollapsed(true);
        closed.add(new JLabel("Not shown"));
        tasks.add(closed);

        // Noon, so the day is the same in every time zone.
        Calendar day = Calendar.getInstance();
        day.set(Calendar.YEAR, 2024);
        day.set(Calendar.MONTH, Calendar.MARCH);
        day.set(Calendar.DAY_OF_MONTH, 15);
        day.set(Calendar.HOUR_OF_DAY, 12);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.set(Calendar.MILLISECOND, 0);
        JXDatePicker picker = new JXDatePicker(day.getTime());
        picker.setFormats("yyyy-MM-dd");

        JXBusyLabel busy = new JXBusyLabel();
        busy.setText("Idle");
        busy.setBusy(false);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        south.setOpaque(false);
        south.add(new JLabel("Due:"));
        south.add(picker);
        south.add(busy);

        JPanel all = new JPanel(new BorderLayout(0, 8));
        all.setOpaque(false);
        all.add(tasks, BorderLayout.CENTER);
        all.add(south, BorderLayout.SOUTH);
        return host(all);
    }

    /// Scene (f): asks a yes/no question in a modal `JOptionPane` and
    /// answers what was chosen, as [#answerName(int)] names it. The call
    /// blocks, as on a desktop, until [#answerConfirm()] or the user
    /// answered.
    public static int confirm() {
        SwingInterop.setNativeWindows(false);
        try {
            return JOptionPane.showConfirmDialog(null, "Delete the selected planet?", "Confirm",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        } finally {
            SwingInterop.setNativeWindows(true);
        }
    }

    /// The name of an answer of [#confirm()].
    public static String answerName(int answer) {
        if (answer == JOptionPane.YES_OPTION) {
            return "yes";
        }
        if (answer == JOptionPane.NO_OPTION) {
            return "no";
        }
        return "closed(" + answer + ")";
    }

    private static AbstractButton button(Container in, String text) {
        for (int i = 0; i < in.getComponentCount(); i++) {
            Component c = in.getComponent(i);
            if (c instanceof AbstractButton && text.equals(((AbstractButton) c).getText())) {
                return (AbstractButton) c;
            }
            if (c instanceof Container) {
                AbstractButton found = button((Container) c, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static AbstractButton yesButton() {
        Window[] windows = Window.getWindows();
        for (int i = windows.length - 1; i >= 0; i--) {
            if (windows[i].isShowing()) {
                AbstractButton yes = button(windows[i], "Yes");
                if (yes != null) {
                    return yes;
                }
            }
        }
        return null;
    }

    /// Whether the dialog of [#confirm()] is up.
    public static boolean confirmShowing() {
        return yesButton() != null;
    }

    /// Presses "Yes" in the dialog of [#confirm()]; false if it is not up.
    public static boolean answerConfirm() {
        AbstractButton yes = yesButton();
        if (yes == null) {
            return false;
        }
        yes.doClick();
        return true;
    }
}
