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

import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

/**
 * The basic controls: labels, buttons, text, choices and a progress bar.
 */
public class ControlsPanel extends JPanel {

    private final JLabel echo = new JLabel(" ");
    private int clicks;

    public ControlsPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(labels());
        add(buttons());
        add(text());
        add(choices());
        add(progress());
        add(echo);
    }

    private JPanel labels() {
        JPanel p = row("Labels");
        p.add(new JLabel("Plain label"));
        p.add(new JLabel("<html><b>Bold</b> and <i>italic</i> and <font color='red'>red</font></html>"));
        JLabel right = new JLabel("Right aligned", SwingConstants.RIGHT);
        right.setToolTipText("A label with a tool tip");
        p.add(right);
        return p;
    }

    private JPanel buttons() {
        JPanel p = row("Buttons");
        JButton count = new JButton("Click me");
        count.addActionListener(e -> {
            clicks++;
            echo.setText("Clicked " + clicks + (clicks == 1 ? " time" : " times"));
        });
        p.add(count);

        JCheckBox enabled = new JCheckBox("Enabled", true);
        enabled.addItemListener(e -> count.setEnabled(enabled.isSelected()));
        p.add(enabled);

        ButtonGroup sizes = new ButtonGroup();
        for (String size : new String[] {"Small", "Medium", "Large"}) {
            JRadioButton radio = new JRadioButton(size, "Medium".equals(size));
            radio.setActionCommand(size);
            radio.addActionListener(e -> echo.setText("Size: " + sizes.getSelection().getActionCommand()));
            sizes.add(radio);
            p.add(radio);
        }
        return p;
    }

    private JPanel text() {
        JPanel p = new JPanel(new GridLayout(1, 2, 8, 0));
        p.setBorder(BorderFactory.createTitledBorder("Text"));

        JPanel fields = new JPanel(new GridLayout(2, 2, 4, 4));
        JTextField name = new JTextField("Ada", 12);
        name.addActionListener(e -> echo.setText("Hello, " + name.getText()));
        JPasswordField password = new JPasswordField(12);
        password.addActionListener(e -> echo.setText("Password of " + password.getPassword().length + " characters"));
        fields.add(new JLabel("Name:"));
        fields.add(name);
        fields.add(new JLabel("Password:"));
        fields.add(password);
        p.add(fields);

        JTextArea notes = new JTextArea(4, 20);
        notes.setLineWrap(true);
        notes.setWrapStyleWord(true);
        notes.setText("A text area inside a scroll pane.\nIt wraps long lines at word boundaries.");
        p.add(new JScrollPane(notes));
        return p;
    }

    private JPanel choices() {
        JPanel p = row("Choices");
        JComboBox<String> colors = new JComboBox<>(new String[] {"Red", "Green", "Blue"});
        colors.setSelectedItem("Green");
        colors.addActionListener(e -> echo.setText("Color: " + colors.getSelectedItem()));
        p.add(colors);

        JSpinner quantity = new JSpinner(new SpinnerNumberModel(3, 0, 10, 1));
        quantity.addChangeListener(e -> echo.setText("Quantity: " + quantity.getValue()));
        p.add(quantity);

        JSlider volume = new JSlider(0, 100, 40);
        volume.setMajorTickSpacing(25);
        volume.setPaintTicks(true);
        volume.setPaintLabels(true);
        volume.addChangeListener(e -> {
            if (!volume.getValueIsAdjusting()) {
                echo.setText("Volume: " + volume.getValue());
            }
        });
        p.add(volume);
        return p;
    }

    private JPanel progress() {
        JPanel p = row("Progress");
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setStringPainted(true);
        Timer timer = new Timer(100, e -> bar.setValue((bar.getValue() + 2) % 101));
        JButton toggle = new JButton("Start");
        toggle.addActionListener(e -> {
            if (timer.isRunning()) {
                timer.stop();
                toggle.setText("Start");
            } else {
                timer.start();
                toggle.setText("Stop");
            }
        });
        p.add(bar);
        p.add(toggle);
        return p;
    }

    private static JPanel row(String title) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        p.setBorder(BorderFactory.createTitledBorder(title));
        return p;
    }
}
