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
package com.example.gallery;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * The application: one frame, a menu bar, and a tab per area of Swing.
 */
public class GalleryApp {

    private static final Logger LOG = Logger.getLogger(GalleryApp.class.getName());

    private final ResourceBundle messages = ResourceBundle.getBundle("messages");
    private JFrame frame;
    private JLabel status;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new GalleryApp().show();
            }
        });
    }

    private void show() {
        frame = new JFrame(messages.getString("app.title"));
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setJMenuBar(createMenuBar());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab(messages.getString("tab.controls"), new ControlsPanel());
        tabs.addTab(messages.getString("tab.layouts"), new LayoutsPanel());
        tabs.addTab(messages.getString("tab.data"), new DataPanel());
        tabs.addTab(messages.getString("tab.painting"), new PaintPanel());
        tabs.addTab(messages.getString("tab.dialogs"), new DialogsPanel());
        tabs.addTab(messages.getString("tab.worker"), new WorkerPanel());
        tabs.addTab(messages.getString("tab.swingx"), new SwingXPanel());
        tabs.addTab(messages.getString("tab.resources"), new ResourcesPanel(messages));
        tabs.addChangeListener(e -> status.setText(tabs.getTitleAt(tabs.getSelectedIndex())));

        status = new JLabel(tabs.getTitleAt(0));
        status.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

        frame.getContentPane().add(tabs, BorderLayout.CENTER);
        frame.getContentPane().add(status, BorderLayout.SOUTH);
        frame.setPreferredSize(new Dimension(820, 600));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        LOG.info("Gallery started");
    }

    private JMenuBar createMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu(messages.getString("menu.file"));
        file.setMnemonic(KeyEvent.VK_F);
        JMenuItem exit = new JMenuItem(messages.getString("menu.exit"));
        exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
        exit.addActionListener(e -> System.exit(0));
        file.add(exit);
        bar.add(file);

        JMenu view = new JMenu(messages.getString("menu.view"));
        JCheckBoxMenuItem showStatus = new JCheckBoxMenuItem(messages.getString("menu.status"), true);
        showStatus.addActionListener(e -> status.setVisible(showStatus.isSelected()));
        view.add(showStatus);
        bar.add(view);

        JMenu help = new JMenu(messages.getString("menu.help"));
        JMenuItem about = new JMenuItem(messages.getString("menu.about"));
        about.addActionListener(e -> JOptionPane.showMessageDialog(frame,
                messages.getString("about.text"), messages.getString("menu.about"),
                JOptionPane.INFORMATION_MESSAGE));
        help.add(about);
        bar.add(help);
        return bar;
    }
}
