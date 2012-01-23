/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */

package com.codename1.designer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;

/**
 * Represents the color icon on a button that pops up the color chooser
 * 
 * @author Shai Almog
 */
class ColorIcon implements Icon {
    private static JColorChooser colorChooser;
    private JComponent cmp;
    public ColorIcon(JComponent cmp) {
        this.cmp = cmp;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        try {
            Color col = new Color(Integer.decode("0x" + getColorString(cmp)));
            g.setColor(col);
            g.fillRect(x, y, getIconWidth(), getIconHeight());
            g.setColor(Color.WHITE);
            g.drawRect(x, y, getIconWidth(), getIconHeight());
        } catch (Exception ignor) {
        }
    }

    protected String getColorString(Component c) {
        if(c instanceof JTextComponent) {
            return ((JTextComponent)c).getText();
        }
        return ((JLabel) c).getText();
    }
    
    public int getIconWidth() {
        return 15;
    }

    public int getIconHeight() {
        return 15;
    }

    public static void installWithColorPicker(final JButton button, final JTextComponent colorText) {
        install(button, colorText);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int color = Integer.decode("0x" + colorText.getText());
                if(colorChooser == null) {
                    colorChooser = new JColorChooser();
                }
                colorChooser.setColor(color);

                JDialog dlg = JColorChooser.createDialog(button, "Pick color", true, colorChooser, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        int i = colorChooser.getColor().getRGB() & 0xffffff;
                        colorText.setText(Integer.toHexString(i));
                    }
                }, null);
                dlg.setLocationByPlatform(true);
                dlg.pack();
                dlg.setVisible(true);
            }
        });
    }

    public static void install(final JButton button, JTextComponent text) {
        button.setIcon(new ColorIcon(text));
        ((AbstractDocument)text.getDocument()).setDocumentFilter(new DocumentFilter() {
            public void remove(DocumentFilter.FilterBypass fb, int offset, int length) throws
                               BadLocationException {
                if(fb.getDocument().getLength() > length) {
                    fb.remove(offset, length);
                    button.repaint();
                }
            }

            public void insertString(DocumentFilter.FilterBypass fb, int offset, String string,
                                     AttributeSet attr) throws BadLocationException {
                if(fb.getDocument().getLength() + string.length() > 6) {
                    return;
                }
                for(int iter = 0 ; iter < string.length() ; iter++) {
                    char c = string.charAt(iter);
                    if(!(Character.isDigit(c) || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f'))) {
                        return;
                    }
                }
                fb.insertString(offset, string, attr);
                button.repaint();
            }

            public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text,
                                AttributeSet attrs) throws BadLocationException {
                if(fb.getDocument().getLength() - length + text.length() > 6) {
                    return;
                }
                for(int iter = 0 ; iter < text.length() ; iter++) {
                    char c = text.charAt(iter);
                    if(!(Character.isDigit(c) || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f'))) {
                        return;
                    }
                }
                fb.replace(offset, length, text, attrs);
                button.repaint();
            }
        });
    }
}
