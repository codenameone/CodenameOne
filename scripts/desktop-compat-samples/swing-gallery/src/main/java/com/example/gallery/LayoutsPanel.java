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
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

/**
 * One small form or arrangement per layout manager.
 */
public class LayoutsPanel extends JPanel {

    public LayoutsPanel() {
        super(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane(SwingConstants.LEFT);
        tabs.addTab("Border", border());
        tabs.addTab("GridBag", gridBag());
        tabs.addTab("Group", group());
        tabs.addTab("Box", box());
        tabs.addTab("Card", card());
        tabs.addTab("Mig", mig());
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel border() {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.add(colored("North", new Color(0xcfe8ff)), BorderLayout.NORTH);
        p.add(colored("South", new Color(0xcfe8ff)), BorderLayout.SOUTH);
        p.add(colored("West", new Color(0xffe9c4)), BorderLayout.WEST);
        p.add(colored("East", new Color(0xffe9c4)), BorderLayout.EAST);
        p.add(colored("Center", Color.WHITE), BorderLayout.CENTER);
        return p;
    }

    private JPanel gridBag() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.LINE_END;
        c.gridx = 0;
        c.gridy = 0;
        p.add(new JLabel("First name:"), c);
        c.gridy = 1;
        p.add(new JLabel("Last name:"), c);
        c.gridy = 2;
        p.add(new JLabel("Email:"), c);

        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        p.add(new JTextField(16), c);
        c.gridy = 1;
        p.add(new JTextField(16), c);
        c.gridy = 2;
        p.add(new JTextField(16), c);

        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        c.weighty = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.FIRST_LINE_END;
        p.add(new JButton("Save"), c);
        return p;
    }

    private JPanel group() {
        JPanel p = new JPanel();
        GroupLayout layout = new GroupLayout(p);
        p.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        JLabel hostLabel = new JLabel("Host:");
        JLabel portLabel = new JLabel("Port:");
        JTextField host = new JTextField("localhost", 16);
        JTextField port = new JTextField("8080", 6);
        JButton connect = new JButton("Connect");

        layout.setHorizontalGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                        .addComponent(hostLabel)
                        .addComponent(portLabel))
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(host)
                        .addComponent(port, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
                                GroupLayout.PREFERRED_SIZE)
                        .addComponent(connect)));
        layout.setVerticalGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(hostLabel)
                        .addComponent(host))
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(portLabel)
                        .addComponent(port))
                .addComponent(connect));
        return p;
    }

    private JPanel box() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.add(new JButton("Left"));
        p.add(Box.createHorizontalStrut(12));
        p.add(new JButton("Next to it"));
        p.add(Box.createHorizontalGlue());
        p.add(new JButton("Far right"));
        return p;
    }

    private JPanel card() {
        CardLayout cards = new CardLayout();
        JPanel deck = new JPanel(cards);
        deck.add(colored("The first card", new Color(0xe3f6e0)), "first");
        deck.add(colored("The second card", new Color(0xf6e0ee)), "second");
        deck.add(colored("The third card", new Color(0xe0e6f6)), "third");

        JComboBox<String> chooser = new JComboBox<>(new String[] {"first", "second", "third"});
        chooser.addActionListener(e -> cards.show(deck, (String) chooser.getSelectedItem()));
        JButton next = new JButton("Next");
        next.addActionListener(e -> cards.next(deck));

        JPanel top = new JPanel();
        top.add(chooser);
        top.add(next);
        JPanel p = new JPanel(new BorderLayout());
        p.add(top, BorderLayout.NORTH);
        p.add(deck, BorderLayout.CENTER);
        return p;
    }

    private JPanel mig() {
        JPanel p = new JPanel(new MigLayout("wrap 2, insets 10", "[right][grow, fill]"));
        p.add(new JLabel("Street:"));
        p.add(new JTextField());
        p.add(new JLabel("City:"));
        p.add(new JTextField());
        p.add(new JLabel("Zip:"));
        p.add(new JTextField(6), "width 80!, growx 0");
        p.add(new JButton("Cancel"), "span 2, split 2, right");
        p.add(new JButton("OK"));
        return p;
    }

    private static JLabel colored(String text, Color background) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setOpaque(true);
        label.setBackground(background);
        label.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        return label;
    }
}
