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

import com.codename1.ui.EditorFont;
import com.codename1.ui.util.EditableResources;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * UI to edit a specific font entry, a font entry can be a bitmap, native font or 
 * system font
 *
 * @author  Shai Almog
 */
public class FontEditor extends BaseForm {
    //private EditFontAction editFontAction = new EditFontAction();
    private EditorFont font;
    private EditableResources resources;
    private String fontName;
    public static final int[] FONT_FACE_VALUES = {EditorFont.FACE_SYSTEM, EditorFont.FACE_MONOSPACE, EditorFont.FACE_PROPORTIONAL};
    public static final int[] FONT_STYLE_VALUES = {EditorFont.STYLE_PLAIN, EditorFont.STYLE_BOLD, EditorFont.STYLE_ITALIC, EditorFont.STYLE_BOLD | EditorFont.STYLE_ITALIC};
    public static final int[] FONT_SIZE_VALUES = {EditorFont.SIZE_MEDIUM, EditorFont.SIZE_SMALL, EditorFont.SIZE_LARGE};
    private static final String[] ANTI_ALIASING_STRINGS;
    private boolean completedConstruction;
    private static final Object[] ANTI_ALIASING_VALUES;
    private boolean factoryCreation;

    static {
        // the LCD/GASP values for AA are only supported under Java 6 and higher
        // this allows us to fail gracefully
        Object[] vals = null;
        String[] strs = null;
        try {
            vals = new Object[] {
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON,
                RenderingHints.VALUE_TEXT_ANTIALIAS_GASP,
                RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HBGR,
                RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB,
                RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VBGR,
                RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VRGB
            };
            strs = new String[]{
                "Off", "Simple", "GASP", "LCD HBGR", "LCD HRGB",
                "LCD VBGR", "LCD VRGB"
            };
        } catch(Throwable t) {
            vals = new Object[] {
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
            };
            strs = new String[]{
                "Off", "Simple"
            };
        }
        ANTI_ALIASING_STRINGS = strs;
        ANTI_ALIASING_VALUES = vals;
    }
    
    /** Creates new form FontEditor */
    public FontEditor(EditableResources resources, com.codename1.ui.Font font, String fontName) {
        this.font = (EditorFont)font;
        this.resources = resources;
        this.fontName = fontName;
        initComponents();
        if(this.font.getBitmapCharset() != null && this.font.getBitmapCharset().length() > 0) {
            charset.setText(this.font.getBitmapCharset());
        }
        boolean hasBitmap = this.font.isIncludesBitmap();
        if(hasBitmap) {
            if(this.font.getLookupFont() != null && this.font.getLookupFont().split(";").length > 1) {
                fontMainType.setSelectedIndex(2);
            } else {
                fontMainType.setSelectedIndex(1);
            }
        } else {
            if(this.font.getLookupFont() != null && this.font.getLookupFont().split(";").length > 1) {
                fontMainType.setSelectedIndex(3);
            } else {
                fontMainType.setSelectedIndex(0);
            }
        }
        createFont();
        fontFamily.setModel(new DefaultComboBoxModel(
            GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        fontFamily.setSelectedIndex(0);
        fontSize.setModel(new SpinnerNumberModel(13.0, 3.0, 120.0, 1.0));
        fontSize.setValue(new Double(13));
        if(this.font.getLookupFont() == null) {
            this.font.setLookupFont("");
        } else {
            String[] fonts = this.font.getLookupFont().split(";");
            for(String lookupValue : fonts) {
                java.awt.Font awtFont = java.awt.Font.decode(lookupValue);
                if(awtFont != null) {
                    String family = awtFont.getFamily();
                    fontFamily.setSelectedItem(family);
                    fontSize.setValue(new Double(awtFont.getSize()));
                    bold.setSelected(awtFont.isBold());
                    italic.setSelected(awtFont.isItalic());
                    break;
                }
            }

            if(fonts.length > 1) {
                lookupString.setText(fonts[1]);
            }
        }
        systemFontFace.setSelectedIndex(getSystemFontOffset(font.getFace(), FONT_FACE_VALUES));
        systemFontSize.setSelectedIndex(getSystemFontOffset(font.getSize(), FONT_SIZE_VALUES));
        systemFontStyle.setSelectedIndex(getSystemFontOffset(font.getStyle(), FONT_STYLE_VALUES));

        antiAliasing.setModel(new DefaultComboBoxModel(ANTI_ALIASING_STRINGS));
        antiAliasing.setSelectedIndex(1);
        if(this.font.getBitmapAntiAliasing() != null) {
            for(int iter = 0 ; iter < ANTI_ALIASING_VALUES.length ; iter++) {
                if(ANTI_ALIASING_VALUES[iter] == this.font.getBitmapAntiAliasing()) {
                    antiAliasing.setSelectedIndex(iter);
                    break;
                }
            }
        }
        updatePreview();
        if (charset.getDocument() instanceof AbstractDocument) {
            final AbstractDocument document = (AbstractDocument) charset.getDocument();
            document.setDocumentFilter(new DocumentFilter() {
                @Override
                public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                    // get the string of the document
                    String docstr = document.getText(0, document.getLength());

                    // remove the characters that already exists in the
                    // document from the entered string
                    int index = 0;
                    while (index < string.length()) {
                        CharSequence substr = string.substring(index, index + 1);
                        if (docstr.indexOf(string.charAt(index)) >= 0) {
                            string = string.replace(substr, "");
                        } else if (string.substring(0, index).indexOf(string.charAt(index)) >= 0) {
                            string = string.substring(0, index) + string.substring(index).replace(substr, "");
                        } else {
                            index++;
                        }
                    }

                    super.insertString(fb, offset, string, attr);
                }

                @Override
                public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                    // get the string of the document
                    String docstr = document.getText(0, document.getLength());

                    // remove the part being replaced
                    docstr = docstr.substring(0, offset) + docstr.substring(offset + length);

                    // remove the characters that already exists in the
                    // document from the entered string
                    int index = 0;
                    while (index < text.length()) {
                        CharSequence substr = text.substring(index, index + 1);
                        if (docstr.indexOf(text.charAt(index)) >= 0) {
                            text = text.replace(substr, "");
                        } else if (text.substring(0, index).indexOf(text.charAt(index)) >= 0) {
                            text = text.substring(0, index) + text.substring(index).replace(substr, "");
                        } else {
                            index++;
                        }
                    }

                    super.replace(fb, offset, length, text, attrs);
                }
            });
            fontMainTypeActionPerformed(null);
        }
        lookupString.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                createFont();
            }

            public void removeUpdate(DocumentEvent e) {
                createFont();
            }

            public void changedUpdate(DocumentEvent e) {
                createFont();
            }
        });
        charset.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                updatePreview();
            }

            public void removeUpdate(DocumentEvent e) {
                updatePreview();
            }

            public void changedUpdate(DocumentEvent e) {
                updatePreview();
            }
        });
        completedConstruction = true;
    }

    /**
     * Creates a font instance based on the current state to place into the 
     * undoable edit within the Editor. 
     */
    public EditorFont createFont() {
        if(!completedConstruction) {
            return null;
        }
        com.codename1.ui.Font systemFont = com.codename1.ui.Font.createSystemFont(FONT_FACE_VALUES[systemFontFace.getSelectedIndex()],
                    FONT_STYLE_VALUES[systemFontStyle.getSelectedIndex()], 
                    FONT_SIZE_VALUES[systemFontSize.getSelectedIndex()]);

        java.awt.Font aFont = preview.getFont();
        String s = aFont.getFamily() + "-";
        if (aFont.isBold()) {
            s += aFont.isItalic() ? "bolditalic" : "bold";
        } else {
            s += aFont.isItalic() ? "italic" : "plain";
        }
        s += "-" + aFont.getSize();
        int selIndex = fontMainType.getSelectedIndex();
        EditorFont newFont = new EditorFont(systemFont, null, s + ";" + lookupString.getText(),
                selIndex == 1 || selIndex == 2, ANTI_ALIASING_VALUES[antiAliasing.getSelectedIndex()], charset.getText());
        if(!factoryCreation) {
            resources.setFont(fontName, newFont);
        }
        return newFont;
    }

    public static int getSystemFontOffset(int value, int[] array) {
        for(int iter = 0 ; iter < array.length ; iter++) {
            if(array[iter] == value) {
                return iter;
            }
        }
        return 0;
    }

    
    private void updatePreview() {
        if(!completedConstruction) {
            return;
        }

        String s = ((String)fontFamily.getSelectedItem()) + "-";
        if (bold.isSelected()) {
            s += italic.isSelected() ? "bolditalic" : "bold";
        } else {
            s += italic.isSelected() ? "italic" : "plain";
        }
        s += "-" + ((Number)fontSize.getValue()).intValue();
        java.awt.Font f = java.awt.Font.decode(s);
        preview.setFont(f);
        preview.setText(charset.getText());
        createFont();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane3 = new javax.swing.JScrollPane();
        help = new javax.swing.JTextPane();
        tabs = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        systemFontFace = new javax.swing.JComboBox();
        jLabel8 = new javax.swing.JLabel();
        systemFontSize = new javax.swing.JComboBox();
        systemFontStyle = new javax.swing.JComboBox();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        bold = new javax.swing.JCheckBox();
        jLabel5 = new javax.swing.JLabel();
        fontFamily = new javax.swing.JComboBox();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        preview = new AATextArea();
        jScrollPane1 = new javax.swing.JScrollPane();
        charset = new javax.swing.JTextArea();
        fontSize = new javax.swing.JSpinner();
        jLabel1 = new javax.swing.JLabel();
        antiAliasing = new javax.swing.JComboBox();
        jLabel6 = new javax.swing.JLabel();
        italic = new javax.swing.JCheckBox();
        jLabel4 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        lookupString = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        fontMainType = new javax.swing.JComboBox();

        FormListener formListener = new FormListener();

        setName("Form"); // NOI18N

        jScrollPane3.setName("jScrollPane3"); // NOI18N

        help.setContentType("text/html");
        help.setEditable(false);
        help.setText("<html>\r\n  <head>\r\n\r\n  </head>\r\n  <body>\r\n    <p style=\"margin-top: 0\">\r\n      Devices contain 4 types of fonts:</p>\n    <ol>\n\t<li>System - a limited set over which the designer has very limited control, however these \nwould always look good on the device and perform reasonably well. There is no way to know in advance \nexactly how the font will look.</li>\n                    <li>Bitmap - fonts are created in the Codename One designer on the desktop as images which are \nthen drawn on the device. The bitmap font will look exactly the same wherever it is run which can be a\n big disadvantage in hi-dpi (dots per inch) devices. A further disadvantage is in localization since the \nbitmap font supports a limited charset defined here (otherwise the size of the images will be huge).</li>\n                   <li>Lookup - on the very few platforms that support it Codename One allows looking up installed fonts.</li>\n                   <li>Loadable fonts - Currently unsupported. This allows embedding a TTF file into the application for loading/kerning at runtime.</li>\n    </ol>\n     Codename One allows 4 combinations of said fonts based on device support and programming logic, e.g. a programmer might choose to disable bitmap\nfonts and most devices don't support lookup font. \n  </body>\r\n</html>\r\n"); // NOI18N
        help.setName("help"); // NOI18N
        jScrollPane3.setViewportView(help);

        tabs.setName("tabs"); // NOI18N

        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setOpaque(false);

        systemFontFace.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "SYSTEM", "MONOSPACE", "PROPORTIONAL" }));
        systemFontFace.setName("systemFontFace"); // NOI18N
        systemFontFace.addActionListener(formListener);

        jLabel8.setText("Face");
        jLabel8.setName("jLabel8"); // NOI18N

        systemFontSize.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "MEDIUM", "SMALL", "LARGE" }));
        systemFontSize.setName("systemFontSize"); // NOI18N
        systemFontSize.addActionListener(formListener);

        systemFontStyle.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "PLAIN", "BOLD", "ITALIC", "BOLD ITALIC" }));
        systemFontStyle.setName("systemFontStyle"); // NOI18N
        systemFontStyle.addActionListener(formListener);

        jLabel9.setText("Style");
        jLabel9.setName("jLabel9"); // NOI18N

        jLabel10.setText("Size");
        jLabel10.setName("jLabel10"); // NOI18N

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel9)
                    .add(jLabel8)
                    .add(jLabel10))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(systemFontSize, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 84, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(systemFontStyle, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 97, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(systemFontFace, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 119, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(267, Short.MAX_VALUE))
        );

        jPanel1Layout.linkSize(new java.awt.Component[] {systemFontFace, systemFontSize, systemFontStyle}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(14, 14, 14)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel8)
                    .add(systemFontFace, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel9)
                    .add(systemFontStyle, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel10)
                    .add(systemFontSize, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(212, Short.MAX_VALUE))
        );

        tabs.addTab("System", jPanel1);

        jPanel2.setName("jPanel2"); // NOI18N
        jPanel2.setOpaque(false);

        bold.setText("Bold");
        bold.setName("bold"); // NOI18N
        bold.addActionListener(formListener);

        jLabel5.setText("Preview");
        jLabel5.setName("jLabel5"); // NOI18N

        fontFamily.setName("fontFamily"); // NOI18N
        fontFamily.addActionListener(formListener);

        jLabel3.setText("Style");
        jLabel3.setName("jLabel3"); // NOI18N

        jScrollPane2.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane2.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        jScrollPane2.setName("jScrollPane2"); // NOI18N
        jScrollPane2.setOpaque(false);

        preview.setColumns(20);
        preview.setEditable(false);
        preview.setLineWrap(true);
        preview.setRows(5);
        preview.setName("preview"); // NOI18N
        preview.setOpaque(false);
        jScrollPane2.setViewportView(preview);

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        charset.setColumns(20);
        charset.setLineWrap(true);
        charset.setRows(5);
        charset.setText("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.,;:!@/\\*()[]{}|#$%^&<>?'\"+- ");
        charset.setName("charset"); // NOI18N
        jScrollPane1.setViewportView(charset);

        fontSize.setName("fontSize"); // NOI18N
        fontSize.addChangeListener(formListener);

        jLabel1.setText("Family");
        jLabel1.setName("jLabel1"); // NOI18N

        antiAliasing.setName("antiAliasing"); // NOI18N
        antiAliasing.addActionListener(formListener);

        jLabel6.setText("Anti-Aliasing");
        jLabel6.setName("jLabel6"); // NOI18N

        italic.setText("Italic");
        italic.setName("italic"); // NOI18N
        italic.addActionListener(formListener);

        jLabel4.setText("Charset");
        jLabel4.setName("jLabel4"); // NOI18N

        jLabel2.setText("Size");
        jLabel2.setName("jLabel2"); // NOI18N

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel2)
                    .add(jLabel3)
                    .add(jLabel5)
                    .add(jLabel4)
                    .add(jLabel6)
                    .add(jLabel1))
                .add(38, 38, 38)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(bold)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(italic))
                    .add(fontFamily, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 312, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(antiAliasing, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 312, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 312, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(fontSize, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 312, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jScrollPane2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 312, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(fontFamily, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel2)
                    .add(fontSize, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel3)
                    .add(italic)
                    .add(bold))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel6)
                    .add(antiAliasing, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel4)
                    .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 23, Short.MAX_VALUE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel5)
                    .add(jScrollPane2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 183, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        tabs.addTab("Bitmap", jPanel2);

        jPanel3.setName("jPanel3"); // NOI18N
        jPanel3.setOpaque(false);

        jLabel11.setText("Lookup String");
        jLabel11.setName("jLabel11"); // NOI18N

        lookupString.setColumns(40);
        lookupString.setName("lookupString"); // NOI18N

        org.jdesktop.layout.GroupLayout jPanel3Layout = new org.jdesktop.layout.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel11)
                .add(7, 7, 7)
                .add(lookupString, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel11)
                    .add(lookupString, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(267, Short.MAX_VALUE))
        );

        tabs.addTab("Lookup", jPanel3);

        jLabel15.setText("Leading Font Type");
        jLabel15.setName("jLabel15"); // NOI18N

        fontMainType.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "System Only", "Bitmap", "Lookup + Bitmap", "Lookup" }));
        fontMainType.setName("fontMainType"); // NOI18N
        fontMainType.addActionListener(formListener);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .addContainerGap()
                        .add(jLabel15)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(fontMainType, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(tabs, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 462, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 317, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jScrollPane3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 405, Short.MAX_VALUE)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel15)
                    .add(fontMainType, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(tabs, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 361, Short.MAX_VALUE))
        );
    }

    // Code for dispatching events from components to event handlers.

    private class FormListener implements java.awt.event.ActionListener, javax.swing.event.ChangeListener {
        FormListener() {}
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            if (evt.getSource() == systemFontFace) {
                FontEditor.this.systemFontFaceActionPerformed(evt);
            }
            else if (evt.getSource() == systemFontSize) {
                FontEditor.this.systemFontSizeActionPerformed(evt);
            }
            else if (evt.getSource() == systemFontStyle) {
                FontEditor.this.systemFontStyleactionInEditableComponent(evt);
            }
            else if (evt.getSource() == bold) {
                FontEditor.this.boldActionPerformed(evt);
            }
            else if (evt.getSource() == fontFamily) {
                FontEditor.this.fontFamilyActionPerformed(evt);
            }
            else if (evt.getSource() == antiAliasing) {
                FontEditor.this.antiAliasingActionPerformed(evt);
            }
            else if (evt.getSource() == italic) {
                FontEditor.this.italicActionPerformed(evt);
            }
            else if (evt.getSource() == fontMainType) {
                FontEditor.this.fontMainTypeActionPerformed(evt);
            }
        }

        public void stateChanged(javax.swing.event.ChangeEvent evt) {
            if (evt.getSource() == fontSize) {
                FontEditor.this.fontSizeStateChanged(evt);
            }
        }
    }// </editor-fold>//GEN-END:initComponents

    private void systemFontFaceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_systemFontFaceActionPerformed
        updatePreview();
}//GEN-LAST:event_systemFontFaceActionPerformed

    private void systemFontStyleactionInEditableComponent(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_systemFontStyleactionInEditableComponent
        updatePreview();
}//GEN-LAST:event_systemFontStyleactionInEditableComponent

    private void systemFontSizeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_systemFontSizeActionPerformed
        updatePreview();
}//GEN-LAST:event_systemFontSizeActionPerformed

    private void fontFamilyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fontFamilyActionPerformed
        updatePreview();
    }//GEN-LAST:event_fontFamilyActionPerformed

    private void fontSizeStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_fontSizeStateChanged
        updatePreview();
    }//GEN-LAST:event_fontSizeStateChanged

    private void boldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_boldActionPerformed
        updatePreview();
    }//GEN-LAST:event_boldActionPerformed

    private void italicActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_italicActionPerformed
        updatePreview();
    }//GEN-LAST:event_italicActionPerformed

    private void antiAliasingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_antiAliasingActionPerformed
        updatePreview();
    }//GEN-LAST:event_antiAliasingActionPerformed

    private void fontMainTypeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fontMainTypeActionPerformed
        switch(fontMainType.getSelectedIndex()) {
            case 0:
                tabs.setSelectedIndex(0);
                tabs.setEnabledAt(1, false);
                tabs.setEnabledAt(2, false);
                lookupString.setText("");
                break;
            case 1:
                if(tabs.getSelectedIndex() == 2) {
                    tabs.setSelectedIndex(0);
                }
                tabs.setEnabledAt(1, true);
                tabs.setEnabledAt(2, false);
                lookupString.setText("");
                break;
            case 2:
                tabs.setEnabledAt(1, true);
                tabs.setEnabledAt(2, true);
                break;
            case 3:
                if(tabs.getSelectedIndex() == 1) {
                    tabs.setSelectedIndex(0);
                }
                tabs.setEnabledAt(1, false);
                tabs.setEnabledAt(2, true);
                break;
        }
        createFont();
    }//GEN-LAST:event_fontMainTypeActionPerformed

    /**
     * @return the factoryCreation
     */
    public boolean isFactoryCreation() {
        return factoryCreation;
    }

    /**
     * @param factoryCreation the factoryCreation to set
     */
    public void setFactoryCreation(boolean factoryCreation) {
        this.factoryCreation = factoryCreation;
    }
    
    
    /**
     * A label that draws the text with the font from codename one
     */
    class FontLabel extends JLabel {
        public void paintComponent(Graphics g) {
            com.codename1.ui.Image i = com.codename1.ui.Image.createImage(getWidth(), getHeight());
            com.codename1.ui.Graphics jwtG = i.getGraphics();
            jwtG.setColor(0xffffff);
            jwtG.fillRect(0, 0, getWidth(), getHeight());
            jwtG.setColor(0);
            jwtG.setFont(font);
            jwtG.setClip(0, 0, getWidth(), getHeight());
            jwtG.drawString(getText(), 0, 0);
            BufferedImage buf = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
            buf.setRGB(0, 0, getWidth(), getHeight(), i.getRGB(), 0, getWidth());
            g.drawImage(buf, 0, 0, null);
        }
        
        public Dimension getMinimumSize() {
            Dimension d = super.getMinimumSize();
            d.width = Math.max(d.width, font.stringWidth(getText()));
            d.height = Math.max(d.height, font.getHeight());
            return d;
        }
        
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            d.width = Math.max(d.width, font.stringWidth(getText()));
            d.height = Math.max(d.height, font.getHeight());
            return d;
        }
    }

    /**
     * Creates a bitmap font object
     */
    public com.codename1.ui.Font createBitmapFont() {
        BufferedImage image = new BufferedImage(5000, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = (Graphics2D)image.getGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, ANTI_ALIASING_VALUES[antiAliasing.getSelectedIndex()]);
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
        g2d.setColor(new Color(0xff0000));
        g2d.setFont(preview.getFont());
        FontMetrics metrics = g2d.getFontMetrics();
        FontRenderContext context = g2d.getFontRenderContext();
        int height = (int)Math.ceil(metrics.getMaxDescent() + metrics.getMaxAscent());
        int baseline = (int)Math.ceil(metrics.getMaxAscent());
        String charsetStr = charset.getText();
        int[] offsets = new int[charsetStr.length()];
        int[] widths = new int[offsets.length];
        int currentOffset = 0;
        for(int iter = 0 ; iter < charsetStr.length() ; iter++) {
            offsets[iter] = currentOffset;
            String currentChar = charsetStr.substring(iter, iter + 1);
            g2d.drawString(currentChar, currentOffset, baseline);
            Rectangle2D rect = preview.getFont().getStringBounds(currentChar, context);
            widths[iter] = (int)Math.ceil(rect.getWidth());

            // max advance works but it makes a HUGE image in terms of width which
            // occupies more ram
            if(preview.getFont().isItalic()) {
                currentOffset += metrics.getMaxAdvance();
            } else {
                currentOffset += widths[iter] + 1;
            }
        }

        g2d.dispose();
        BufferedImage shrunk = new BufferedImage(currentOffset, height, BufferedImage.TYPE_INT_RGB);
        g2d = (Graphics2D)shrunk.getGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();

        int[] rgb = new int[shrunk.getWidth() * shrunk.getHeight()];
        shrunk.getRGB(0, 0, shrunk.getWidth(), shrunk.getHeight(), rgb, 0, shrunk.getWidth());
        com.codename1.ui.Image bitmap = com.codename1.ui.Image.createImage(rgb, shrunk.getWidth(), shrunk.getHeight());

        return com.codename1.ui.Font.createBitmapFont(bitmap, offsets, widths, charsetStr);
    }

    /**
     * Supports disabling/enabling anti-aliasing for the text area
     */
    private class AATextArea extends JTextArea {
        @Override
        public void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D)g.create();
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, ANTI_ALIASING_VALUES[antiAliasing.getSelectedIndex()]);
            super.paintComponent(g2d);
        }
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox antiAliasing;
    private javax.swing.JCheckBox bold;
    private javax.swing.JTextArea charset;
    private javax.swing.JComboBox fontFamily;
    private javax.swing.JComboBox fontMainType;
    private javax.swing.JSpinner fontSize;
    private javax.swing.JTextPane help;
    private javax.swing.JCheckBox italic;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTextField lookupString;
    private javax.swing.JTextArea preview;
    private javax.swing.JComboBox systemFontFace;
    private javax.swing.JComboBox systemFontSize;
    private javax.swing.JComboBox systemFontStyle;
    private javax.swing.JTabbedPane tabs;
    // End of variables declaration//GEN-END:variables
    
}
