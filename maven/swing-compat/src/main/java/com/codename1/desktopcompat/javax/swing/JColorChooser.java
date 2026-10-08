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
package com.codename1.desktopcompat.javax.swing;

import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.desktopcompat.java.awt.Dialog;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Window;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.javax.swing.event.ChangeEvent;
import com.codename1.desktopcompat.javax.swing.event.ChangeListener;

/// `javax.swing.JColorChooser`: picks a color.
///
/// The pane is a preview above three sliders, one each for red, green and
/// blue. The swatch, HSV, HSL and CMYK panels of the desktop, the color
/// selection model and custom chooser panels are not provided, and the
/// alpha of a color is not editable: the chosen color is opaque.
public class JColorChooser extends JComponent {

    public static final String SELECTION_MODEL_PROPERTY = "selectionModel";
    public static final String PREVIEW_PANEL_PROPERTY = "previewPanel";
    public static final String CHOOSER_PANELS_PROPERTY = "chooserPanels";

    private final JSlider[] sliders = new JSlider[3];
    private final JPanel preview = new JPanel();
    private Color color;
    private boolean adjusting;

    public JColorChooser() {
        this(new Color(255, 255, 255));
    }

    public JColorChooser(Color initialColor) {
        color = initialColor == null ? new Color(255, 255, 255) : initialColor;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        preview.setOpaque(true);
        preview.setPreferredSize(new Dimension(160, 40));
        preview.setBackground(color);
        add(preview, BorderLayout.NORTH);
        JPanel rows = new JPanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        String[] names = {"Red", "Green", "Blue"};
        int[] parts = {color.getRed(), color.getGreen(), color.getBlue()};
        ChangeListener moved = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (!adjusting) {
                    setColor(sliders[0].getValue(), sliders[1].getValue(), sliders[2].getValue());
                }
            }
        };
        for (int i = 0; i < 3; i++) {
            JPanel row = new JPanel(new BorderLayout(6, 0));
            row.add(new JLabel(names[i]), BorderLayout.WEST);
            sliders[i] = new JSlider(0, 255, parts[i]);
            sliders[i].addChangeListener(moved);
            row.add(sliders[i], BorderLayout.CENTER);
            rows.add(row);
        }
        add(rows, BorderLayout.CENTER);
    }

    /// Shows a modal color dialog and waits for it: the color chosen, or
    /// `null` when the user cancelled or closed the dialog.
    public static Color showDialog(Component component, String title, Color initialColor) {
        final JColorChooser pane = new JColorChooser(initialColor);
        final Color[] chosen = new Color[1];
        JDialog dialog = createDialog(component, title, true, pane, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                chosen[0] = pane.getColor();
            }
        }, null);
        dialog.setVisible(true);
        dialog.dispose();
        return chosen[0];
    }

    public static JDialog createDialog(Component c, String title, boolean modal, JColorChooser chooserPane,
            final ActionListener okListener, final ActionListener cancelListener) {
        Window owner = null;
        if (c instanceof Window) {
            owner = (Window) c;
        } else if (c != null) {
            owner = SwingUtilities.getWindowAncestor(c);
        }
        final JDialog dialog = new JDialog(owner, title,
                modal ? Dialog.ModalityType.APPLICATION_MODAL : Dialog.ModalityType.MODELESS);
        Container content = dialog.getContentPane();
        content.setLayout(new BorderLayout());
        if (chooserPane != null) {
            content.add(chooserPane, BorderLayout.CENTER);
        }
        JPanel buttons = new JPanel();
        JButton ok = new JButton(label("ColorChooser.okText", "OK"));
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(false);
                if (okListener != null) {
                    okListener.actionPerformed(e);
                }
            }
        });
        JButton cancel = new JButton(label("ColorChooser.cancelText", "Cancel"));
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(false);
                if (cancelListener != null) {
                    cancelListener.actionPerformed(e);
                }
            }
        });
        buttons.add(ok);
        buttons.add(cancel);
        content.add(buttons, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(ok);
        dialog.pack();
        dialog.setLocationRelativeTo(c);
        return dialog;
    }

    private static String label(String key, String fallback) {
        String s = UIManager.getString(key);
        return s == null ? fallback : s;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        if (color == null) {
            return;
        }
        Color old = this.color;
        this.color = color;
        adjusting = true;
        sliders[0].setValue(color.getRed());
        sliders[1].setValue(color.getGreen());
        sliders[2].setValue(color.getBlue());
        adjusting = false;
        preview.setBackground(color);
        preview.repaint();
        firePropertyChange("color", old, color);
    }

    public void setColor(int r, int g, int b) {
        setColor(new Color(r, g, b));
    }

    public void setColor(int c) {
        setColor((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF);
    }
}
