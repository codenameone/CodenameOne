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

import java.awt.GridLayout;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * What an application loads by name: images, translated text and its saved settings.
 */
public class ResourcesPanel extends JPanel {

    private static final Logger LOG = Logger.getLogger(ResourcesPanel.class.getName());
    private static final String VISITS = "visits";

    private final Preferences prefs = Preferences.userNodeForPackage(ResourcesPanel.class);

    public ResourcesPanel(ResourceBundle messages) {
        super(new GridLayout(0, 1, 6, 6));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        add(new JLabel(messages.getString("resources.icon"), icon("/icons/star.png"), SwingConstants.LEFT));

        JButton iconButton = new JButton(messages.getString("resources.button"), icon("/icons/dot.png"));
        JPanel buttonRow = new JPanel();
        buttonRow.add(iconButton);
        add(buttonRow);

        int visits = prefs.getInt(VISITS, 0) + 1;
        prefs.putInt(VISITS, visits);
        JLabel counter = new JLabel(MessageFormat.format(messages.getString("resources.visits"), visits));
        add(counter);

        JButton reset = new JButton(messages.getString("resources.reset"));
        reset.addActionListener(e -> {
            prefs.remove(VISITS);
            counter.setText(MessageFormat.format(messages.getString("resources.visits"), 0));
            LOG.log(Level.INFO, "Visit counter reset");
        });
        JPanel resetRow = new JPanel();
        resetRow.add(reset);
        add(resetRow);

        add(new JLabel(messages.getString("resources.locale") + " " + messages.getLocale()));
    }

    private ImageIcon icon(String path) {
        URL url = getClass().getResource(path);
        if (url == null) {
            LOG.warning("Missing resource " + path);
            return null;
        }
        return new ImageIcon(url);
    }
}
