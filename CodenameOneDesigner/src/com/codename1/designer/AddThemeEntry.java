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

import com.codename1.ui.resource.util.CodenameOneComponentWrapper;
import com.codename1.designer.AddResourceDialog;
import com.codename1.designer.ResourceEditorView;
import com.codename1.ui.Display;
import com.codename1.ui.EditorFont;
import com.codename1.ui.EditorTTFFont;
import com.codename1.ui.Font;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.Accessor;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.EditableResources;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;

/**
 * UI and logic for adding a new entry to the theme within the theme editor
 *
 * @author  Shai Almog
 */
public class AddThemeEntry extends javax.swing.JPanel {
    private boolean disableRefresh = true;
    private boolean brokenImage = false;
    public static final int[] FONT_FACE_VALUES = {Font.FACE_SYSTEM, Font.FACE_MONOSPACE, Font.FACE_PROPORTIONAL};
    public static final int[] FONT_STYLE_VALUES = {Font.STYLE_PLAIN, Font.STYLE_BOLD, Font.STYLE_ITALIC, Font.STYLE_BOLD | Font.STYLE_ITALIC};
    public static final int[] FONT_SIZE_VALUES = {Font.SIZE_MEDIUM, Font.SIZE_SMALL, Font.SIZE_LARGE};

    public static int BACKGROUND_VALUES_GRADIENT_ARRAY_OFFSET = 17;

    public static final byte[] BACKGROUND_VALUES = {
        Style.BACKGROUND_IMAGE_SCALED,
        Style.BACKGROUND_IMAGE_TILE_BOTH,
        Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_LEFT,
        Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_CENTER,
        Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_RIGHT,
        Style.BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_TOP,
        Style.BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_CENTER,
        Style.BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_BOTTOM,
        Style.BACKGROUND_IMAGE_ALIGNED_TOP,
        Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM,
        Style.BACKGROUND_IMAGE_ALIGNED_LEFT,
        Style.BACKGROUND_IMAGE_ALIGNED_RIGHT,
        Style.BACKGROUND_IMAGE_ALIGNED_TOP_LEFT,
        Style.BACKGROUND_IMAGE_ALIGNED_TOP_RIGHT,
        Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM_LEFT,
        Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM_RIGHT,
        Style.BACKGROUND_IMAGE_ALIGNED_CENTER,
        Style.BACKGROUND_GRADIENT_LINEAR_HORIZONTAL,
        Style.BACKGROUND_GRADIENT_LINEAR_VERTICAL,
        Style.BACKGROUND_GRADIENT_RADIAL,
        Style.BACKGROUND_NONE
    };

    public static final String[] BACKGROUND_STRINGS = {
        "IMAGE_SCALED",
        "IMAGE_TILE_BOTH",
        "IMAGE_TILE_VERTICAL_ALIGN_LEFT",
        "IMAGE_TILE_VERTICAL_ALIGN_CENTER",
        "IMAGE_TILE_VERTICAL_ALIGN_RIGHT",
        "IMAGE_TILE_HORIZONTAL_ALIGN_TOP",
        "IMAGE_TILE_HORIZONTAL_ALIGN_CENTER",
        "IMAGE_TILE_HORIZONTAL_ALIGN_BOTTOM",
        "IMAGE_ALIGNED_TOP",
        "IMAGE_ALIGNED_BOTTOM",
        "IMAGE_ALIGNED_LEFT",
        "IMAGE_ALIGNED_RIGHT",
        "IMAGE_ALIGNED_TOP_LEFT",
        "IMAGE_ALIGNED_TOP_RIGHT",
        "IMAGE_ALIGNED_BOTTOM_LEFT",
        "IMAGE_ALIGNED_BOTTOM_RIGHT",
        "IMAGE_ALIGNED_CENTER",
        "GRADIENT_LINEAR_HORIZONTAL",
        "GRADIENT_LINEAR_VERTICAL",
        "GRADIENT_RADIAL",
        "NONE"
    };

    public static final String[] IMAGE_ALIGNMENT_STRINGS = {
        "ALIGN_TOP",
        "ALIGN_BOTTOM",
        "ALIGN_LEFT",
        "ALIGN_RIGHT",
        "ALIGN_CENTER"
    };

    private EditableResources resources;
    private ResourceEditorView view;
    private Hashtable themeHash;
    private Hashtable originalTheme;
    private Border currentBorder;
    private String prefix;
    private com.codename1.ui.Container codenameOnePreview = new com.codename1.ui.Container(new com.codename1.ui.layouts.BorderLayout());
    private String themeName;
    
    /** Creates new form AddThemeEntry */
    public AddThemeEntry(boolean adding, EditableResources resources, ResourceEditorView view, Hashtable themeHash, String prefix, String themeName) {
        if(prefix == null) {
            prefix = "";
        }
        this.themeName = themeName;
        this.prefix = prefix;
        this.resources = resources;
        this.view = view;
        this.themeHash = themeHash;
        
        originalTheme = new Hashtable();
        originalTheme.putAll(themeHash);
        initComponents();
        
        trueTypeFontSizeValue.setModel(new SpinnerNumberModel(12.0, 5, 200, 0.5));
        if(ResourceEditorView.getLoadedFile() != null) {
            String[] fontFiles = ResourceEditorView.getLoadedFile().getParentFile().list(new FilenameFilter() {
                @Override
                public boolean accept(File file, String string) {
                    return string.endsWith(".ttf");
                }
            });
            if(fontFiles == null) {
                fontFiles = new String[0];
            } else {
                String[] arr = new String[fontFiles.length + 1];
                System.arraycopy(fontFiles, 0, arr, 1, fontFiles.length);
                fontFiles = arr;
            }
            trueTypeFont.setModel(new DefaultComboBoxModel(fontFiles));
        } else {
            trueTypeFont.setModel(new DefaultComboBoxModel(new String[0]));
        }
        
        try {
            help.setPage(getClass().getResource("/help/themePropertyHelp.html"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        initUIIDComboBox(componentName);
        if(prefix.length() == 0) {
            styleType.setText("Unselected");
        } else {
            if(prefix.indexOf("sel") > -1) {
                styleType.setText("Selected");
            } else {
                if(prefix.indexOf("dis") > -1) {
                    styleType.setText("Disabled");
                } else {
                    styleType.setText("Pressed");
                }
            }
        }
        if(!adding) {
            componentName.setEnabled(false);
        }

        Object[] componentNameArray = new Object[componentName.getModel().getSize()];
        for(int iter = 0 ; iter < componentNameArray.length ; iter++) {
            componentNameArray[iter] = componentName.getModel().getElementAt(iter);
        }
        baseStyle.setModel(new DefaultComboBoxModel(componentNameArray));

        codenameOnePreview.addComponent(com.codename1.ui.layouts.BorderLayout.CENTER, new com.codename1.ui.Label("Preview"));
        previewPane.add(java.awt.BorderLayout.CENTER, new CodenameOneComponentWrapper(codenameOnePreview));

        bindColorIconToButton(changeColorButtonFG, colorValueFG);
        bindColorIconToButton(changeColorButtonBG, colorValueBG);
        bindColorIconToButton(changeGradientStartColorButton, gradientStartColor);
        bindColorIconToButton(changeGradientEndColorButton, gradientEndColor);

        initImagesCombo();
        bitmapFontValue.setModel(new DefaultComboBoxModel(resources.getFontResourceNames()));
        
        paddingBottom.setModel(new SpinnerNumberModel(0, 0, 100, 1));
        paddingLeft.setModel(new SpinnerNumberModel(0, 0, 100, 1));
        paddingRight.setModel(new SpinnerNumberModel(0, 0, 100, 1));
        paddingTop.setModel(new SpinnerNumberModel(0, 0, 100, 1));
        marginBottom.setModel(new SpinnerNumberModel(0, 0, 100, 1));
        marginLeft.setModel(new SpinnerNumberModel(0, 0, 100, 1));
        marginRight.setModel(new SpinnerNumberModel(0, 0, 100, 1));
        marginTop.setModel(new SpinnerNumberModel(0, 0, 100, 1));
        transparencyValue.setModel(new SpinnerNumberModel(0, 0, 255, 1));
        gradientX.setModel(new SpinnerNumberModel(0.5, 0, 1, 0.01));
        gradientY.setModel(new SpinnerNumberModel(0.5, 0, 1, 0.01));
        gradientSize.setModel(new SpinnerNumberModel(1, 0, 2, 0.1));

        if(adding) {
            disableRefresh = false;
            updateThemePreview();
        }
    }

    private void help(String key) {
        try {
            help.setPage(getClass().getResource("/help/themePropertyHelp.html").toExternalForm() + "#" + key);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        addTabs.setSelectedIndex(8);
    }

    /**
     * Initializes a combo box for editing UIID's
     */
    public static void initUIIDComboBox(JComboBox jc) {
        jc.setEditable(true);
        Vector uiids = new Vector();
        uiids.add("");
        for(Object k : Accessor.getThemeProps().keySet()) {
            String key = (String)k;
            int dot = key.indexOf('.');
            if(dot > -1 && key.indexOf('@') < 0) {
                key = key.substring(0, dot);
                if(!uiids.contains(key)) {
                    uiids.add(key);
                }
            }
        }
        Collections.sort(uiids, String.CASE_INSENSITIVE_ORDER);
        jc.setModel(new DefaultComboBoxModel(uiids));
        com.codename1.ui.Form currentForm = com.codename1.ui.Display.getInstance().getCurrent();
        if(currentForm != null) {
            final List<String> currentFormUIIDs = new ArrayList<String>();
            findAllUIIDs(currentFormUIIDs, currentForm);
            Collections.sort(currentFormUIIDs, String.CASE_INSENSITIVE_ORDER);
            Collections.reverse(currentFormUIIDs);
            for(String cmp : currentFormUIIDs) {
                jc.insertItemAt(cmp, 1);
            }
            jc.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    String uiid = (String)value;
                    if(index > 0 && index < currentFormUIIDs.size() + 1) {
                        value = "<html><body><b>" + value + "</b></body></html>";
                    } else {
                        if(value == null || ((String)value).length() == 0) {
                            value = "[null]";
                        }
                    }
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    setIcon(ThemeEditor.getUIIDPreviewImage(uiid, false, false, false));
                    return this;
                }
            });
        }
    }

    private static void addUIID(List<String> currentFormUIIDs, com.codename1.ui.Component c) {
        if(c != null) {
            String uiid = c.getUIID();
            if(!currentFormUIIDs.contains(uiid)) {
                currentFormUIIDs.add(uiid);
            }
        }
    }

    private static void findAllUIIDs(List<String> currentFormUIIDs, com.codename1.ui.Container c) {
        addUIID(currentFormUIIDs, c);
        for(int iter = 0 ; iter < c.getComponentCount() ; iter++) {
            com.codename1.ui.Component currentComponent = c.getComponentAt(iter);
            if(currentComponent.getWidth() <= 0 && currentComponent.getHeight() <= 0 || !currentComponent.isVisible()) {
                continue;
            }
            if(currentComponent instanceof com.codename1.ui.Container) {
                findAllUIIDs(currentFormUIIDs, (com.codename1.ui.Container)currentComponent);
            } else {
                addUIID(currentFormUIIDs, currentComponent);
                if(currentComponent instanceof com.codename1.ui.List) {
                    // try to be REALLY smart about list UIID detection
                    com.codename1.ui.List lst = (com.codename1.ui.List)currentComponent;
                    Object value;
                    if(lst.getModel().getSize() > 0) {
                        value = lst.getModel().getItemAt(0);
                    } else {
                        if(lst.getRenderingPrototype() == null) {
                            continue;
                        }
                        value = lst.getRenderingPrototype();
                    }

                    // make sure to include both selected/unselected entries, both odd and
                    // the even styles as well as the focus component style
                    addUIID(currentFormUIIDs, lst.getRenderer().getListCellRendererComponent(lst, value, 0, false));
                    addUIID(currentFormUIIDs, lst.getRenderer().getListCellRendererComponent(lst, value, 0, true));
                    addUIID(currentFormUIIDs, lst.getRenderer().getListCellRendererComponent(lst, value, 1, false));
                    addUIID(currentFormUIIDs, lst.getRenderer().getListCellRendererComponent(lst, value, 1, true));
                    addUIID(currentFormUIIDs, lst.getRenderer().getListFocusComponent(lst));
                }
            }
        }
    }

    private void bindColorIconToButton(final JButton button, final JTextComponent text) {
        ColorIcon.install(button, text);
        text.getDocument().addDocumentListener(new DocumentListener() {
            private void update() {
                if(!disableRefresh && text.getText().length() > 0) {
                    updateThemePreview();
                }
            }
            public void insertUpdate(DocumentEvent e) {
                update();
            }

            public void removeUpdate(DocumentEvent e) {
                update();
            }

            public void changedUpdate(DocumentEvent e) {
                update();
            }
        });
    }
    
    private void initImagesCombo() {
        ResourceEditorView.initImagesComboBox(imagesCombo, resources, true, false);
        updateThemePreview();
    }

    public void setKeyValues(String uiid, String sel) {
        disableRefresh = true;
        // special case for default uiid!
        if(uiid == null) {
            for(Object currentKey : themeHash.keySet()) {
                String key = (String)currentKey;
                if(key.indexOf('.') < 0) {
                    if(sel == null || sel.length() == 0) {
                        if(key.indexOf('#') > -1) {
                            continue;
                        }
                    } else {
                        if(key.indexOf(sel) < 0) {
                            continue;
                        }
                    }
                    setKeyValue(key, themeHash.get(key));
                }
            }
            disableRefresh = false;
            return;
        }
        for(Object currentKey : themeHash.keySet()) {
            String key = (String)currentKey;
            if(key.startsWith(uiid + ".")) {
                if(sel == null || sel.length() == 0) {
                    if(key.indexOf('#') > -1) {
                        continue;
                    }
                } else {
                    if(key.indexOf(sel) < 0) {
                        continue;
                    }
                }
                Object v = themeHash.get(key);
                if(v != null) {
                    setKeyValue(key, v);
                }
            }
        }
        // we need to manually set the component name, this can happen when opening
        // from the UI builder
        if(componentName.getSelectedItem() == null || "".equals(componentName.getSelectedItem())) {
            componentName.setSelectedItem(uiid);
        }
        disableRefresh = false;
        updateThemePreview();
    }

    public String getUIID() {
        return (String)componentName.getSelectedItem();
    }

    public void pasteKeyValues(Hashtable h) {
        disableRefresh = true;
        for(Object currentKey : h.keySet()) {
            String key = (String)currentKey;
            setKeyValue(key, h.get(key));
        }
        disableRefresh = false;
        updateThemePreview();
    }

    private ImageIcon bullet;
    private void highlightTab(int index) {
        if(bullet == null) {
            bullet = new ImageIcon(getClass().getResource("/bullet_blue.png"));
        }
        addTabs.setIconAt(index, bullet);
        addTabs.revalidate();
        /*String t = addTabs.getTitleAt(index);
        if(t.startsWith("<html")) {
            return;
        }
        addTabs.setTitleAt(index, "<html><body><b>" + t + "</b></body></html>");*/
    }

    private void setKeyValue(String key, Object value) {
        int pos = key.indexOf(".");
        String attr;
        if(pos > -1) {
            componentName.setSelectedItem(key.substring(0, pos));
            attr = key.substring(pos + 1, key.length());
        } else {
            componentName.setSelectedIndex(0);
            attr = key;
        }
        pos = attr.indexOf('#');
        if(pos > -1) {
            attr = attr.substring(pos + 1);
        }
        if(attr.indexOf("fgColor") > -1) {
            deriveForegroundColor.setSelected(false);
            highlightTab(1);
            changeColorButtonFG.setEnabled(true);
            colorValueFG.setEnabled(true);
            if(value instanceof String) {
                colorValueFG.setText((String)value);
            } else {
                colorValueFG.setText(Integer.toHexString(((Number)value).intValue()));
            }
            return;
        }
        if(attr.indexOf("bgColor") > -1) {
            deriveBackgroundColor.setSelected(false);
            highlightTab(1);
            changeColorButtonBG.setEnabled(true);
            colorValueBG.setEnabled(true);
            if(value instanceof String) {
                colorValueBG.setText((String)value);
            } else {
                colorValueBG.setText(Integer.toHexString(((Number)value).intValue()));
            }
            return;
        }
        if(attr.indexOf("derive") > -1) {
            highlightTab(6);
            baseStyle.setEnabled(true);
            baseStyleType.setEnabled(true);
            defineAttribute.setSelected(false);
            String baseItemValue = (String)value;
            int keyPos = baseItemValue.indexOf('.');
            if(keyPos < 0) {
                baseStyle.setSelectedItem(baseItemValue);
            } else {
                String b = baseItemValue.substring(0, keyPos);
                String k = baseItemValue.substring(keyPos + 1);
                baseStyle.setSelectedItem(b);
                if(k.equals("sel")) {
                    baseStyleType.setSelectedIndex(1);
                    return;
                }
                if(k.equals("press")) {
                    baseStyleType.setSelectedIndex(2);
                    return;
                }
                if(k.equals("dis")) {
                    baseStyleType.setSelectedIndex(3);
                    return;
                }
            }
            return;
        }
        if(attr.indexOf("align") > -1) {
            highlightTab(2);
            deriveAlignment.setSelected(false);
            alignmentCombo.setEnabled(true);
            switch( ((Number)value).intValue() ) {
                case com.codename1.ui.Component.LEFT:
                    alignmentCombo.setSelectedIndex(0);
                    break;
                case com.codename1.ui.Component.RIGHT:
                    alignmentCombo.setSelectedIndex(1);
                    break;
                case com.codename1.ui.Component.CENTER:
                    alignmentCombo.setSelectedIndex(2);
                    break;
            }
            return;
        }
        if(attr.indexOf("textDecoration") > -1) {
            highlightTab(7);
            deriveTextDecoration.setSelected(false);
            textDecorationCombo.setEnabled(true);
            switch( ((Number)value).intValue() ) {
                case com.codename1.ui.plaf.Style.TEXT_DECORATION_UNDERLINE:
                    textDecorationCombo.setSelectedIndex(1);
                    break;
                case com.codename1.ui.plaf.Style.TEXT_DECORATION_STRIKETHRU:
                    textDecorationCombo.setSelectedIndex(2);
                    break;
                case com.codename1.ui.plaf.Style.TEXT_DECORATION_3D:
                    textDecorationCombo.setSelectedIndex(3);
                    break;
                case com.codename1.ui.plaf.Style.TEXT_DECORATION_3D_LOWERED:
                    textDecorationCombo.setSelectedIndex(4);
                    break;
                default:
                    textDecorationCombo.setSelectedIndex(0);
                    break;
            }
            return;
        }
        if(attr.indexOf("border") > -1) {
            highlightTab(5);
            customizeBorder.setEnabled(true);
            deriveBorder.setSelected(false);
            borderLabel.setText(Accessor.toString((Border)value));
            ((CodenameOneComponentWrapper)borderLabel).getCodenameOneComponent().getStyle().setBorder((Border)value);
            borderLabel.repaint();
            if(value != null && value instanceof Border) {
                currentBorder = (Border)value;
            } else {
                currentBorder = Border.getDefaultBorder();
            }
            return;
        }
        if(attr.indexOf("font") > -1) {
            highlightTab(7);
            Font font = (Font)value;
            deriveFont.setSelected(false);
            systemFont.setEnabled(true);
            bitmapFont.setEnabled(true);
            if(resources.getFontResourceNames() != null) {
                for(String fontName : resources.getFontResourceNames()) {
                    if(font == resources.getFont(fontName)) {
                        // this is a bitmap font
                        bitmapFont.setSelected(true);
                        bitmapFontValue.setEnabled(true);
                        addNewBitmapFont.setEnabled(true);
                        bitmapFontValue.setSelectedItem(fontName);
                        return;
                    }
                }
            }
            // this is a system font
            systemFont.setSelected(true);
            fontFace.setEnabled(true);
            fontSize.setEnabled(true);
            fontStyle.setEnabled(true);
            trueTypeFont.setEnabled(trueTypeFont.getModel().getSize() > 0);
            trueTypeFontSizeOption.setEnabled(trueTypeFont.getModel().getSize() > 0);
            trueTypeFontSizeValue.setEnabled(trueTypeFont.getModel().getSize() > 0);
            fontFace.setSelectedIndex(getSystemFontOffset(font.getFace(), FONT_FACE_VALUES));
            fontSize.setSelectedIndex(getSystemFontOffset(font.getSize(), FONT_SIZE_VALUES));
            fontStyle.setSelectedIndex(getSystemFontOffset(font.getStyle(), FONT_STYLE_VALUES));
            if(font instanceof EditorTTFFont) {
                EditorTTFFont ed = (EditorTTFFont)font;
                if(ed.getFontFile() != null) {
                    trueTypeFont.setSelectedItem(ed.getFontFile().getName());
                    trueTypeFontSizeOption.setSelectedIndex(ed.getSizeSetting());
                    trueTypeFontSizeValue.setValue(new Double(ed.getActualSize()));
                }
            }
            return;
        }
        if(attr.indexOf("bgImage") > -1) {
            highlightTab(0);
            updateBackgroundAttribute();
            for(int iter = 0 ; iter < imagesCombo.getModel().getSize() ; iter++) {
                String name = (String)imagesCombo.getModel().getElementAt(iter);
                if(value == resources.getImage(name)) {
                    imagesCombo.setSelectedItem(name);
                    return;
                }
            }
            return;
        }
        if(attr.indexOf("transparency") > -1) {
            highlightTab(1);
            deriveTransparency.setSelected(false);
            transparencyValue.setEnabled(true);
            transparencyValue.setValue(Integer.valueOf((String)value));
            return;
        }
        if(attr.indexOf("padding") > -1) {
            highlightTab(3);
            derivePadding.setSelected(false);
            paddingBottom.setEnabled(true);
            paddingTop.setEnabled(true);
            paddingLeft.setEnabled(true);
            paddingRight.setEnabled(true);
            paddingBottomUnit.setEnabled(true);
            paddingTopUnit.setEnabled(true);
            paddingLeftUnit.setEnabled(true);
            paddingRightUnit.setEnabled(true);
            StringTokenizer tokenizer = new StringTokenizer((String)value, ", ");
            paddingTop.setValue(Integer.parseInt(tokenizer.nextToken()));
            paddingBottom.setValue(Integer.parseInt(tokenizer.nextToken()));
            paddingLeft.setValue(Integer.parseInt(tokenizer.nextToken()));
            paddingRight.setValue(Integer.parseInt(tokenizer.nextToken()));
            return;
        }

        if(attr.indexOf("padUnit") > -1) {
            byte[] padUnit = (byte[])value;
            paddingBottomUnit.setSelectedIndex(padUnit[com.codename1.ui.Component.BOTTOM]);
            paddingTopUnit.setSelectedIndex(padUnit[com.codename1.ui.Component.TOP]);
            paddingLeftUnit.setSelectedIndex(padUnit[com.codename1.ui.Component.LEFT]);
            paddingRightUnit.setSelectedIndex(padUnit[com.codename1.ui.Component.RIGHT]);
            return;
        }

        if(attr.indexOf("margin") > -1) {
            highlightTab(4);
            deriveMargin.setSelected(false);
            marginBottom.setEnabled(true);
            marginTop.setEnabled(true);
            marginLeft.setEnabled(true);
            marginRight.setEnabled(true);
            marginBottomUnit.setEnabled(true);
            marginTopUnit.setEnabled(true);
            marginLeftUnit.setEnabled(true);
            marginRightUnit.setEnabled(true);
            StringTokenizer tokenizer = new StringTokenizer((String)value, ", ");
            marginTop.setValue(Integer.parseInt(tokenizer.nextToken()));
            marginBottom.setValue(Integer.parseInt(tokenizer.nextToken()));
            marginLeft.setValue(Integer.parseInt(tokenizer.nextToken()));
            marginRight.setValue(Integer.parseInt(tokenizer.nextToken()));
            return;
        }

        if(attr.indexOf("marUnit") > -1) {
            byte[] padUnit = (byte[])value;
            marginBottomUnit.setSelectedIndex(padUnit[com.codename1.ui.Component.BOTTOM]);
            marginTopUnit.setSelectedIndex(padUnit[com.codename1.ui.Component.TOP]);
            marginLeftUnit.setSelectedIndex(padUnit[com.codename1.ui.Component.LEFT]);
            marginRightUnit.setSelectedIndex(padUnit[com.codename1.ui.Component.RIGHT]);
            return;
        }

        if(attr.indexOf("bgType") > -1) {
            highlightTab(0);
            updateBackgroundAttribute();

            byte bgType = ((Byte)value).byteValue();

            for(int iter = 0 ; iter < BACKGROUND_VALUES.length ; iter++) {
                if(bgType == BACKGROUND_VALUES[iter]) {
                    backgroundType.setSelectedIndex(iter);
                    break;
                }
            }

            return;
        }
        if(attr.indexOf("bgGradient") > -1) {
            highlightTab(0);
            updateBackgroundAttribute();

            Object[] gradient = (Object[])value;

            gradientStartColor.setText(Integer.toHexString(((Number)gradient[0]).intValue()));
            gradientEndColor.setText(Integer.toHexString(((Number)gradient[1]).intValue()));
            if(gradient.length > 2) {
                gradientX.setValue(new Double(((Number)gradient[2]).doubleValue()));
                gradientY.setValue(new Double(((Number)gradient[3]).doubleValue()));
                if(gradient.length > 4) {
                    gradientSize.setValue(new Double(((Number)gradient[4]).doubleValue()));
                }
            }

            return;
        }
    }

    private void updateBackgroundAttribute() {
        deriveBackground.setSelected(false);
        imagesCombo.setEnabled(true);
        addNewImage.setEnabled(true);
        backgroundType.setEnabled(true);
        gradientEndColor.setEnabled(true);
        gradientStartColor.setEnabled(true);
        gradientSize.setEnabled(true);
        gradientX.setEnabled(true);
        gradientY.setEnabled(true);
        changeGradientEndColorButton.setEnabled(true);
        changeGradientStartColorButton.setEnabled(true);
    }

    public static int getSystemFontOffset(int value, int[] array) {
        for(int iter = 0 ; iter < array.length ; iter++) {
            if(array[iter] == value) {
                return iter;
            }
        }
        return 0;
    }

    /**
     * Method used to prevent the concurrent modification exception when changing the iterated keyset
     */
    private void removeKeys(Hashtable themeRes, String uiid) {
        // special case for globals
        if(uiid == null || uiid.length() == 0) {
            for(Object k : themeRes.keySet()) {
                String key = (String)k;
                if(key.indexOf('.') > -1 || key.indexOf('#') > -1 || key.indexOf('@') > -1) {
                    continue;
                }
                themeRes.remove(key);
                removeKeys(themeRes, uiid);
                return;
            }
            return;
        }
        for(Object k : themeRes.keySet()) {
            String key = (String)k;
            if(key.startsWith(uiid)) {
                if(prefix.length() == 0 && key.indexOf('#') > 0) {
                    continue;
                }
                themeRes.remove(key);
                removeKeys(themeRes, uiid);
                return;
            }
        }
    }

    private void updateThemeRes(byte[] padUnit, Hashtable themeRes, String type) {
            for(byte b : padUnit) {
                if(b != 0) {
                    themeRes.put(type, padUnit);
                    return;
                }
            }
            themeRes.remove(type);
    }

    /**
     * Updates the theme hash with the values from this editor
     */
    public void updateThemeHashtable(Hashtable themeRes) {
        if(disableRefresh) {
            return;
        }
        
        String uiid = prefix;
        String item = (String)componentName.getSelectedItem();
        if(item != null && item.length() > 0) {
            uiid = item + "." + prefix;
        }
        removeKeys(themeRes, uiid);
        if(!defineAttribute.isSelected()) {
            String val = (String)baseStyle.getSelectedItem();
            if(val != null && val.length() > 0) {
                switch(baseStyleType.getSelectedIndex()) {
                    case 0:
                        themeRes.put(uiid + "derive", val);
                        break;
                    case 1:
                        themeRes.put(uiid + "derive", val + ".sel");
                        break;
                    case 2:
                        themeRes.put(uiid + "derive", val + ".press");
                        break;
                    case 3:
                        themeRes.put(uiid + "derive", val + ".dis");
                        break;
                }
            }
        }
        if(!deriveAlignment.isSelected()) {
            switch(alignmentCombo.getSelectedIndex()) {
                case 0:
                    themeRes.put(uiid + "align", new Integer(com.codename1.ui.Component.LEFT));
                    break;
                case 1:
                    themeRes.put(uiid + "align", new Integer(com.codename1.ui.Component.RIGHT));
                    break;
                default:
                    themeRes.put(uiid + "align", new Integer(com.codename1.ui.Component.CENTER));
                    break;
            }
        }
        if(!deriveBackground.isSelected()) {
            int index = backgroundType.getSelectedIndex();
            themeRes.put(uiid + "bgType", new Byte(BACKGROUND_VALUES[index]));
            if(backgroundType.getSelectedIndex() >= BACKGROUND_VALUES_GRADIENT_ARRAY_OFFSET) {
                // this is a gradient related type
                themeRes.put(uiid + "bgGradient", new Object[] {
                        Integer.valueOf(gradientStartColor.getText(), 16),
                        Integer.valueOf(gradientEndColor.getText(), 16),
                        new Float(((Number)gradientX.getValue()).floatValue()),
                        new Float(((Number)gradientY.getValue()).floatValue()),
                        new Float(((Number)gradientSize.getValue()).floatValue())
                    });
            } else {
                // this is an image related type
                if(imagesCombo.getSelectedItem() != null && imagesCombo.getSelectedItem().toString().length() > 0) {
                    themeRes.put(uiid + "bgImage", resources.getImage((String)imagesCombo.getSelectedItem()));
                } else {
                    brokenImage = true;
                    themeRes.put(uiid + "bgImage", com.codename1.ui.Image.createImage(5, 5));
                }
            }
        }
        if(!deriveBackgroundColor.isSelected()) {
            themeRes.put(uiid + "bgColor", colorValueBG.getText());
        }
        if(!deriveBorder.isSelected()) {
            if(currentBorder == null) {
                themeRes.remove(uiid + "border");
            } else {
                themeRes.put(uiid + "border", currentBorder);
            }
        }
        if(!deriveFont.isSelected()) {
            Object v;
            if(bitmapFont.isSelected()) {
                String val = (String)bitmapFontValue.getSelectedItem();
                if(val != null) {
                    v = resources.getFont(val);
                } else {
                    v = Font.getDefaultFont();
                }
            } else {
                if(trueTypeFont.getSelectedIndex() > 0) {
                    Font sys = Font.createSystemFont(FONT_FACE_VALUES[fontFace.getSelectedIndex()],
                            FONT_STYLE_VALUES[fontStyle.getSelectedIndex()], FONT_SIZE_VALUES[fontSize.getSelectedIndex()]);
                    v = new EditorTTFFont(new File(ResourceEditorView.getLoadedFile().getParentFile(), (String)trueTypeFont.getSelectedItem()), 
                            trueTypeFontSizeOption.getSelectedIndex(), ((Number)trueTypeFontSizeValue.getValue()).floatValue(), sys);
                } else {
                    v = Font.createSystemFont(FONT_FACE_VALUES[fontFace.getSelectedIndex()],
                            FONT_STYLE_VALUES[fontStyle.getSelectedIndex()], FONT_SIZE_VALUES[fontSize.getSelectedIndex()]);
                }
            }
            themeRes.put(uiid + "font", v);
        }
        if(!deriveForegroundColor.isSelected())  {
            themeRes.put(uiid + "fgColor", colorValueFG.getText());
        }
        if(!deriveMargin.isSelected()) {
            themeRes.put(uiid + "margin", marginTop.getValue() + "," + marginBottom.getValue() + "," +
                    marginLeft.getValue() + "," + marginRight.getValue());
            byte[] padUnit = new byte[4];
            padUnit[com.codename1.ui.Component.BOTTOM] = (byte)marginBottomUnit.getSelectedIndex();
            padUnit[com.codename1.ui.Component.TOP] = (byte)marginTopUnit.getSelectedIndex();
            padUnit[com.codename1.ui.Component.LEFT] = (byte)marginLeftUnit.getSelectedIndex();
            padUnit[com.codename1.ui.Component.RIGHT] = (byte)marginRightUnit.getSelectedIndex();
            updateThemeRes(padUnit, themeRes, uiid + "marUnit");
        }
        if(!derivePadding.isSelected()) {
            themeRes.put(uiid + "padding", paddingTop.getValue() + "," + paddingBottom.getValue() + "," +
                    paddingLeft.getValue() + "," + paddingRight.getValue());
            byte[] padUnit = new byte[4];
            padUnit[com.codename1.ui.Component.BOTTOM] = (byte)paddingBottomUnit.getSelectedIndex();
            padUnit[com.codename1.ui.Component.TOP] = (byte)paddingTopUnit.getSelectedIndex();
            padUnit[com.codename1.ui.Component.LEFT] = (byte)paddingLeftUnit.getSelectedIndex();
            padUnit[com.codename1.ui.Component.RIGHT] = (byte)paddingRightUnit.getSelectedIndex();
            updateThemeRes(padUnit, themeRes, uiid + "padUnit");
        }
        if(!deriveTextDecoration.isSelected()) {
            Object v;
            switch(textDecorationCombo.getSelectedIndex()) {
                case 1:
                    v = new Integer(com.codename1.ui.plaf.Style.TEXT_DECORATION_UNDERLINE);
                    break;
                case 2:
                    v = new Integer(com.codename1.ui.plaf.Style.TEXT_DECORATION_STRIKETHRU);
                    break;
                case 3:
                    v = new Integer(com.codename1.ui.plaf.Style.TEXT_DECORATION_3D);
                    break;
                case 4:
                    v = new Integer(com.codename1.ui.plaf.Style.TEXT_DECORATION_3D_LOWERED);
                    break;
                default:
                    v = new Integer(0);
                    break;
            }
            themeRes.put(uiid + "textDecoration", v);
        }
        if(!deriveTransparency.isSelected()) {
            themeRes.put(uiid + "transparency", "" + transparencyValue.getValue());
        }
    }

    /*public String getKey() {
        if(componentName.getSelectedIndex() == 0) {
            return prefix + (String)attributeName.getSelectedItem();
        }
        return ((String)componentName.getSelectedItem()) + "." + prefix + ((String)attributeName.getSelectedItem());
    }

    public Object getValue() {
        brokenImage = false;
        String s = (String)attributeName.getSelectedItem();
        if(s != null) {
            if(s.indexOf("border") > -1) {
                return currentBorder;
            }
            if(s.indexOf("Color") > -1) {
                return colorValue.getText();
            }
            if(s.indexOf("derive") > -1) {
                if(baseStyleType.getSelectedIndex() == 0) {
                    return baseStyle.getSelectedItem();
                } else {
                    switch(baseStyleType.getSelectedIndex()) {
                        case 1:
                            return baseStyle.getSelectedItem() + ".sel";
                        case 2:
                            return baseStyle.getSelectedItem() + ".press";
                        default:
                            return baseStyle.getSelectedItem() + ".dis";
                    }
                }
            }
            if(s.indexOf("font") > -1) {
                if(bitmapFont.isSelected()) {
                    String val = (String)bitmapFontValue.getSelectedItem();
                    if(val != null) {
                        return resources.getFont(val);
                    }
                    return Font.getDefaultFont();
                }
                return Font.createSystemFont(FONT_FACE_VALUES[fontFace.getSelectedIndex()],
                    FONT_STYLE_VALUES[fontStyle.getSelectedIndex()], FONT_SIZE_VALUES[fontSize.getSelectedIndex()]);
            }
            if(s.indexOf("bgImage") > -1) {
                if(imagesCombo.getSelectedItem() != null && imagesCombo.getSelectedItem().toString().length() > 0) {
                    return resources.getImage((String)imagesCombo.getSelectedItem());
                }
                brokenImage = true;
                return com.codename1.ui.Image.createImage(5, 5);
            }
            if(s.indexOf("transparency") > -1) {
                return "" + transparencyValue.getValue();
            }
            if(s.indexOf("padding") > -1 || s.indexOf("margin") > -1) {
                return paddingTop.getValue() + "," + paddingBottom.getValue() + "," +
                    paddingLeft.getValue() + "," + paddingRight.getValue();
            }
            if(s.indexOf("align") > -1) {
                switch(alignmentCombo.getSelectedIndex()) {
                    case 0:
                        return new Integer(com.codename1.ui.Component.LEFT);
                    case 1:
                        return new Integer(com.codename1.ui.Component.RIGHT);
                }
                return new Integer(com.codename1.ui.Component.CENTER);
            }
            if(s.indexOf("textDecoration") > -1) {
                switch(textDecorationCombo.getSelectedIndex()) {
                    case 1:
                        return new Integer(com.codename1.ui.plaf.Style.TEXT_DECORATION_UNDERLINE);
                    case 2:
                        return new Integer(com.codename1.ui.plaf.Style.TEXT_DECORATION_STRIKETHRU);
                }
                return new Integer(0);
            }
            if(s.indexOf("bgType") > -1) {
                return new Byte(BACKGROUND_VALUES[backgroundType.getSelectedIndex()]);
            }

            if(s.indexOf("bgGradient") > -1) {
                return new Object[] {
                    Integer.valueOf(gradientStartColor.getText(), 16),
                    Integer.valueOf(gradientEndColor.getText(), 16),
                    new Float(((Number)gradientX.getValue()).floatValue()),
                    new Float(((Number)gradientY.getValue()).floatValue()),
                    new Float(((Number)gradientSize.getValue()).floatValue())
                };
            }

        }
        return null;
    }*/
    
    public boolean isBrokenImage() {
        return brokenImage;
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jLabel1 = new javax.swing.JLabel();
        componentName = new javax.swing.JComboBox();
        previewPane = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        addTabs = new javax.swing.JTabbedPane();
        jPanel3 = new javax.swing.JPanel();
        jLabel23 = new javax.swing.JLabel();
        imagesCombo = new javax.swing.JComboBox();
        addNewImage = new javax.swing.JButton();
        jLabel16 = new javax.swing.JLabel();
        backgroundType = new javax.swing.JComboBox();
        jLabel18 = new javax.swing.JLabel();
        gradientStartColor = new javax.swing.JTextField();
        changeGradientStartColorButton = new javax.swing.JButton();
        gradientEndColor = new javax.swing.JTextField();
        changeGradientEndColorButton = new javax.swing.JButton();
        jLabel19 = new javax.swing.JLabel();
        gradientX = new javax.swing.JSpinner();
        gradientY = new javax.swing.JSpinner();
        gradientSize = new javax.swing.JSpinner();
        deriveBackground = new javax.swing.JCheckBox();
        deriveHelp = new javax.swing.JButton();
        backgroundHelp = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        colorValueFG = new javax.swing.JTextField();
        changeColorButtonFG = new javax.swing.JButton();
        deriveForegroundColor = new javax.swing.JCheckBox();
        jLabel3 = new javax.swing.JLabel();
        colorValueBG = new javax.swing.JTextField();
        changeColorButtonBG = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        deriveBackgroundColor = new javax.swing.JCheckBox();
        deriveTransparency = new javax.swing.JCheckBox();
        jLabel5 = new javax.swing.JLabel();
        transparencyValue = new javax.swing.JSpinner();
        deriveHelp1 = new javax.swing.JButton();
        colorHelp = new javax.swing.JButton();
        jLabel15 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabel20 = new javax.swing.JLabel();
        alignmentCombo = new javax.swing.JComboBox();
        deriveAlignment = new javax.swing.JCheckBox();
        deriveHelp2 = new javax.swing.JButton();
        alignHelp = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        paddingLeft = new javax.swing.JSpinner();
        jLabel9 = new javax.swing.JLabel();
        paddingRight = new javax.swing.JSpinner();
        jLabel10 = new javax.swing.JLabel();
        paddingTop = new javax.swing.JSpinner();
        jLabel11 = new javax.swing.JLabel();
        paddingBottom = new javax.swing.JSpinner();
        derivePadding = new javax.swing.JCheckBox();
        deriveHelp3 = new javax.swing.JButton();
        paddingHelp = new javax.swing.JButton();
        paddingLeftUnit = new javax.swing.JComboBox();
        paddingRightUnit = new javax.swing.JComboBox();
        paddingTopUnit = new javax.swing.JComboBox();
        paddingBottomUnit = new javax.swing.JComboBox();
        jPanel6 = new javax.swing.JPanel();
        jLabel17 = new javax.swing.JLabel();
        marginLeft = new javax.swing.JSpinner();
        jLabel24 = new javax.swing.JLabel();
        marginRight = new javax.swing.JSpinner();
        jLabel25 = new javax.swing.JLabel();
        marginTop = new javax.swing.JSpinner();
        jLabel26 = new javax.swing.JLabel();
        marginBottom = new javax.swing.JSpinner();
        deriveMargin = new javax.swing.JCheckBox();
        deriveHelp4 = new javax.swing.JButton();
        marginHelp = new javax.swing.JButton();
        marginLeftUnit = new javax.swing.JComboBox();
        marginRightUnit = new javax.swing.JComboBox();
        marginTopUnit = new javax.swing.JComboBox();
        marginBottomUnit = new javax.swing.JComboBox();
        jPanel8 = new javax.swing.JPanel();
        borderLabel = new com.codename1.ui.resource.util.CodenameOneComponentWrapper(new com.codename1.ui.Label("Border"));
        customizeBorder = new javax.swing.JButton();
        deriveBorder = new javax.swing.JCheckBox();
        imageBorderWizard = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        deriveHelp5 = new javax.swing.JButton();
        borderHelp = new javax.swing.JButton();
        jPanel10 = new javax.swing.JPanel();
        baseStyle = new javax.swing.JComboBox();
        baseStyleType = new javax.swing.JComboBox();
        defineAttribute = new javax.swing.JCheckBox();
        deriveHelp6 = new javax.swing.JButton();
        jPanel7 = new javax.swing.JPanel();
        bitmapFont = new javax.swing.JRadioButton();
        systemFont = new javax.swing.JRadioButton();
        bitmapFontValue = new javax.swing.JComboBox();
        addNewBitmapFont = new javax.swing.JButton();
        fontFace = new javax.swing.JComboBox();
        fontStyle = new javax.swing.JComboBox();
        fontSize = new javax.swing.JComboBox();
        deriveFont = new javax.swing.JCheckBox();
        textDecorationCombo = new javax.swing.JComboBox();
        deriveTextDecoration = new javax.swing.JCheckBox();
        jLabel7 = new javax.swing.JLabel();
        deriveHelp7 = new javax.swing.JButton();
        fontHelp = new javax.swing.JButton();
        jLabel12 = new javax.swing.JLabel();
        trueTypeFont = new javax.swing.JComboBox();
        jLabel13 = new javax.swing.JLabel();
        trueTypeFontSizeOption = new javax.swing.JComboBox();
        trueTypeFontSizeValue = new javax.swing.JSpinner();
        jScrollPane2 = new javax.swing.JScrollPane();
        help = new javax.swing.JTextPane();
        styleType = new javax.swing.JLabel();
        styleHelp = new javax.swing.JButton();
        videoTutorial = new javax.swing.JButton();

        FormListener formListener = new FormListener();

        setName("Form"); // NOI18N

        jLabel1.setText("Component");
        jLabel1.setName("jLabel1"); // NOI18N

        componentName.setEditable(true);
        componentName.setName("componentName"); // NOI18N
        componentName.setPrototypeDisplayValue("XXXXXXXXXXXXXX");
        componentName.addActionListener(formListener);

        previewPane.setName("previewPane"); // NOI18N
        previewPane.setLayout(new java.awt.BorderLayout());

        jLabel6.setText("Preview");
        jLabel6.setName("jLabel6"); // NOI18N

        addTabs.setName("addTabs"); // NOI18N

        jPanel3.setName("jPanel3"); // NOI18N
        jPanel3.setOpaque(false);

        jLabel23.setText("Image");
        jLabel23.setName("jLabel23"); // NOI18N

        imagesCombo.setEnabled(false);
        imagesCombo.setName("imagesCombo"); // NOI18N
        imagesCombo.addActionListener(formListener);

        addNewImage.setText("...");
        addNewImage.setEnabled(false);
        addNewImage.setName("addNewImage"); // NOI18N
        addNewImage.addActionListener(formListener);

        jLabel16.setText("Type");
        jLabel16.setName("jLabel16"); // NOI18N

        backgroundType.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "IMAGE_SCALED", "IMAGE_TILE_BOTH", "IMAGE_TILE_VERTICAL_ALIGN_LEFT", "IMAGE_TILE_VERTICAL_ALIGN_CENTER", "IMAGE_TILE_VERTICAL_ALIGN_RIGHT", "IMAGE_TILE_HORIZONTAL_ALIGN_TOP", "IMAGE_TILE_HORIZONTAL_ALIGN_CENTER", "IMAGE_TILE_HORIZONTAL_ALIGN_BOTTOM", "IMAGE_ALIGNED_TOP", "IMAGE_ALIGNED_BOTTOM", "IMAGE_ALIGNED_LEFT", "IMAGE_ALIGNED_RIGHT", "IMAGE_ALIGNED_TOP_LEFT", "IMAGE_ALIGNED_TOP_RIGHT", "IMAGE_ALIGNED_BOTTOM_LEFT", "IMAGE_ALIGNED_BOTTOM_RIGHT", "IMAGE_ALIGNED_CENTER", "GRADIENT_LINEAR_HORIZONTAL", "GRADIENT_LINEAR_VERTICAL", "GRADIENT_RADIAL", "NONE" }));
        backgroundType.setEnabled(false);
        backgroundType.setName("backgroundType"); // NOI18N
        backgroundType.addActionListener(formListener);

        jLabel18.setText("Gradient");
        jLabel18.setName("jLabel18"); // NOI18N

        gradientStartColor.setText("000000");
        gradientStartColor.setEnabled(false);
        gradientStartColor.setName("gradientStartColor"); // NOI18N

        changeGradientStartColorButton.setText("...");
        changeGradientStartColorButton.setEnabled(false);
        changeGradientStartColorButton.setName("changeGradientStartColorButton"); // NOI18N
        changeGradientStartColorButton.addActionListener(formListener);

        gradientEndColor.setText("000000");
        gradientEndColor.setEnabled(false);
        gradientEndColor.setName("gradientEndColor"); // NOI18N

        changeGradientEndColorButton.setText("...");
        changeGradientEndColorButton.setEnabled(false);
        changeGradientEndColorButton.setName("changeGradientEndColorButton"); // NOI18N
        changeGradientEndColorButton.addActionListener(formListener);

        jLabel19.setText("Gradient X/Y");
        jLabel19.setName("jLabel19"); // NOI18N

        gradientX.setToolTipText("Gradient Relative X");
        gradientX.setEnabled(false);
        gradientX.setName("gradientX"); // NOI18N
        gradientX.addChangeListener(formListener);

        gradientY.setToolTipText("Gradient Relative Y");
        gradientY.setEnabled(false);
        gradientY.setName("gradientY"); // NOI18N
        gradientY.addChangeListener(formListener);

        gradientSize.setToolTipText("Gradient Relaitve Size");
        gradientSize.setEnabled(false);
        gradientSize.setName("gradientSize"); // NOI18N
        gradientSize.addChangeListener(formListener);

        deriveBackground.setSelected(true);
        deriveBackground.setText("Derive");
        deriveBackground.setName("deriveBackground"); // NOI18N
        deriveBackground.addActionListener(formListener);

        deriveHelp.setText("Derive Help");
        deriveHelp.setName("deriveHelp"); // NOI18N
        deriveHelp.addActionListener(formListener);

        backgroundHelp.setText("Background Help");
        backgroundHelp.setName("backgroundHelp"); // NOI18N
        backgroundHelp.addActionListener(formListener);

        jLabel2.setText("Gradient Size");
        jLabel2.setName("jLabel2"); // NOI18N

        jLabel14.setText("<html><body><b>Notice:</b> If a border is defined the background will have no effect! Set the border<br>property to Empty to override the border of a base style");
        jLabel14.setName("jLabel14"); // NOI18N

        org.jdesktop.layout.GroupLayout jPanel3Layout = new org.jdesktop.layout.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel3Layout.createSequentialGroup()
                        .add(10, 10, 10)
                        .add(deriveBackground)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 504, Short.MAX_VALUE)
                        .add(deriveHelp))
                    .add(jPanel3Layout.createSequentialGroup()
                        .addContainerGap()
                        .add(jLabel14, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(jPanel3Layout.createSequentialGroup()
                        .addContainerGap()
                        .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel16)
                            .add(jLabel23)
                            .add(jLabel18)
                            .add(jLabel19)
                            .add(jLabel2))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(gradientSize, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                                .add(jPanel3Layout.createSequentialGroup()
                                    .add(backgroundType, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 325, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .add(backgroundHelp))
                                .add(jPanel3Layout.createSequentialGroup()
                                    .add(imagesCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 325, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .add(addNewImage))
                                .add(jPanel3Layout.createSequentialGroup()
                                    .add(gradientStartColor, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 54, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                    .add(changeGradientStartColorButton)
                                    .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                    .add(gradientEndColor, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 52, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .add(6, 6, 6)
                                    .add(changeGradientEndColorButton)
                                    .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 228, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .add(jPanel3Layout.createSequentialGroup()
                                    .add(gradientX, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                    .add(gradientY, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))))
                .addContainerGap())
        );

        jPanel3Layout.linkSize(new java.awt.Component[] {gradientEndColor, gradientSize, gradientStartColor, gradientX, gradientY}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        jPanel3Layout.linkSize(new java.awt.Component[] {backgroundType, imagesCombo}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        jPanel3Layout.linkSize(new java.awt.Component[] {backgroundHelp, deriveHelp}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel3Layout.createSequentialGroup()
                        .addContainerGap()
                        .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(deriveBackground)
                            .add(deriveHelp))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel14, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(18, 18, 18)
                        .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel16)
                            .add(backgroundType, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(backgroundHelp))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel23)
                            .add(imagesCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(addNewImage))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.CENTER)
                            .add(jLabel18)
                            .add(gradientStartColor, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(changeGradientStartColorButton)
                            .add(changeGradientEndColorButton))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.CENTER)
                            .add(jLabel19)
                            .add(gradientX, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(gradientY, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.CENTER)
                            .add(jLabel2)
                            .add(gradientSize, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                    .add(jPanel3Layout.createSequentialGroup()
                        .add(178, 178, 178)
                        .add(gradientEndColor, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(111, Short.MAX_VALUE))
        );

        addTabs.addTab("Background", jPanel3);

        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setOpaque(false);

        colorValueFG.setText("000000");
        colorValueFG.setEnabled(false);
        colorValueFG.setName("colorValueFG"); // NOI18N

        changeColorButtonFG.setText("...");
        changeColorButtonFG.setEnabled(false);
        changeColorButtonFG.setName("changeColorButtonFG"); // NOI18N
        changeColorButtonFG.addActionListener(formListener);

        deriveForegroundColor.setSelected(true);
        deriveForegroundColor.setText("Derive Foreground");
        deriveForegroundColor.setName("deriveForegroundColor"); // NOI18N
        deriveForegroundColor.addActionListener(formListener);

        jLabel3.setText("Background");
        jLabel3.setName("jLabel3"); // NOI18N

        colorValueBG.setText("000000");
        colorValueBG.setEnabled(false);
        colorValueBG.setName("colorValueBG"); // NOI18N

        changeColorButtonBG.setText("...");
        changeColorButtonBG.setEnabled(false);
        changeColorButtonBG.setName("changeColorButtonBG"); // NOI18N
        changeColorButtonBG.addActionListener(formListener);

        jLabel4.setText("Foreground");
        jLabel4.setName("jLabel4"); // NOI18N

        deriveBackgroundColor.setSelected(true);
        deriveBackgroundColor.setText("Derive Background");
        deriveBackgroundColor.setName("deriveBackgroundColor"); // NOI18N
        deriveBackgroundColor.addActionListener(formListener);

        deriveTransparency.setSelected(true);
        deriveTransparency.setText("Derive Transparency");
        deriveTransparency.setName("deriveTransparency"); // NOI18N
        deriveTransparency.addActionListener(formListener);

        jLabel5.setText("Transparency");
        jLabel5.setName("jLabel5"); // NOI18N

        transparencyValue.setEnabled(false);
        transparencyValue.setName("transparencyValue"); // NOI18N
        transparencyValue.addChangeListener(formListener);

        deriveHelp1.setText("Derive Help");
        deriveHelp1.setName("deriveHelp1"); // NOI18N
        deriveHelp1.addActionListener(formListener);

        colorHelp.setText("Color Help");
        colorHelp.setName("colorHelp"); // NOI18N
        colorHelp.addActionListener(formListener);

        jLabel15.setText("<html><body><b>Notice:</b> If a border is defined the background will have no effect! Set the border<br>property to Empty to override the border of a base style");
        jLabel15.setName("jLabel15"); // NOI18N

        jLabel21.setText("<html><body><b>Notice:</b> some types of backgrounds might override the background color.<br/>Transparency should be 255 to achieve full opacity");
        jLabel21.setName("jLabel21"); // NOI18N

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(20, 20, 20)
                        .add(deriveForegroundColor)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 435, Short.MAX_VALUE)
                        .add(deriveHelp1))
                    .add(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .add(jLabel15, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .add(jLabel21, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jPanel1Layout.createSequentialGroup()
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 9, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(jLabel4)
                                    .add(jLabel3)
                                    .add(jLabel5))
                                .add(12, 12, 12)
                                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(jPanel1Layout.createSequentialGroup()
                                        .add(colorValueFG, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 66, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(changeColorButtonFG)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 331, Short.MAX_VALUE)
                                        .add(colorHelp))
                                    .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                                        .add(org.jdesktop.layout.GroupLayout.LEADING, transparencyValue)
                                        .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel1Layout.createSequentialGroup()
                                            .add(colorValueBG, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 66, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                            .add(changeColorButtonBG)))))
                            .add(deriveBackgroundColor)
                            .add(deriveTransparency))))
                .addContainerGap())
        );

        jPanel1Layout.linkSize(new java.awt.Component[] {colorHelp, deriveHelp1}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(deriveForegroundColor)
                    .add(deriveHelp1))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jLabel15, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jLabel21, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(18, 18, 18)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(colorValueFG, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(changeColorButtonFG)
                    .add(jLabel4)
                    .add(colorHelp))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(deriveBackgroundColor)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(colorValueBG, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(changeColorButtonBG)
                    .add(jLabel3))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(deriveTransparency)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.CENTER)
                    .add(jLabel5)
                    .add(transparencyValue, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(109, Short.MAX_VALUE))
        );

        addTabs.addTab("Color", jPanel1);

        jPanel2.setName("jPanel2"); // NOI18N
        jPanel2.setOpaque(false);

        jLabel20.setText("Alignment");
        jLabel20.setName("jLabel20"); // NOI18N

        alignmentCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Left", "Right", "Center" }));
        alignmentCombo.setEnabled(false);
        alignmentCombo.setName("alignmentCombo"); // NOI18N
        alignmentCombo.addActionListener(formListener);

        deriveAlignment.setSelected(true);
        deriveAlignment.setText("Derive");
        deriveAlignment.setName("deriveAlignment"); // NOI18N
        deriveAlignment.addActionListener(formListener);

        deriveHelp2.setText("Derive Help");
        deriveHelp2.setName("deriveHelp2"); // NOI18N
        deriveHelp2.addActionListener(formListener);

        alignHelp.setText("Alignment Help");
        alignHelp.setName("alignHelp"); // NOI18N
        alignHelp.addActionListener(formListener);

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(deriveAlignment)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 486, Short.MAX_VALUE)
                        .add(deriveHelp2))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel2Layout.createSequentialGroup()
                        .add(jLabel20)
                        .add(9, 9, 9)
                        .add(alignmentCombo, 0, 478, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(alignHelp)))
                .addContainerGap())
        );

        jPanel2Layout.linkSize(new java.awt.Component[] {alignHelp, deriveHelp2}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(deriveAlignment)
                    .add(deriveHelp2))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel20)
                    .add(alignmentCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(alignHelp))
                .addContainerGap(310, Short.MAX_VALUE))
        );

        addTabs.addTab("Alignment", jPanel2);

        jPanel5.setName("jPanel5"); // NOI18N
        jPanel5.setOpaque(false);

        jLabel8.setText("Left");
        jLabel8.setName("jLabel8"); // NOI18N

        paddingLeft.setEnabled(false);
        paddingLeft.setName("paddingLeft"); // NOI18N
        paddingLeft.addChangeListener(formListener);

        jLabel9.setText("Right");
        jLabel9.setName("jLabel9"); // NOI18N

        paddingRight.setEnabled(false);
        paddingRight.setName("paddingRight"); // NOI18N
        paddingRight.addChangeListener(formListener);

        jLabel10.setText("Top");
        jLabel10.setName("jLabel10"); // NOI18N

        paddingTop.setEnabled(false);
        paddingTop.setName("paddingTop"); // NOI18N
        paddingTop.addChangeListener(formListener);

        jLabel11.setText("Bottom");
        jLabel11.setName("jLabel11"); // NOI18N

        paddingBottom.setEnabled(false);
        paddingBottom.setName("paddingBottom"); // NOI18N
        paddingBottom.addChangeListener(formListener);

        derivePadding.setSelected(true);
        derivePadding.setText("Derive");
        derivePadding.setName("derivePadding"); // NOI18N
        derivePadding.addActionListener(formListener);

        deriveHelp3.setText("Derive Help");
        deriveHelp3.setName("deriveHelp3"); // NOI18N
        deriveHelp3.addActionListener(formListener);

        paddingHelp.setText("Padding Help");
        paddingHelp.setName("paddingHelp"); // NOI18N
        paddingHelp.addActionListener(formListener);

        paddingLeftUnit.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Pixels", "Screen Size Percentage", "Millimeters (approximate)" }));
        paddingLeftUnit.setName("paddingLeftUnit"); // NOI18N
        paddingLeftUnit.addActionListener(formListener);

        paddingRightUnit.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Pixels", "Screen Size Percentage", "Millimeters (approximate)" }));
        paddingRightUnit.setName("paddingRightUnit"); // NOI18N
        paddingRightUnit.addActionListener(formListener);

        paddingTopUnit.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Pixels", "Screen Size Percentage", "Millimeters (approximate)" }));
        paddingTopUnit.setName("paddingTopUnit"); // NOI18N
        paddingTopUnit.addActionListener(formListener);

        paddingBottomUnit.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Pixels", "Screen Size Percentage", "Millimeters (approximate)" }));
        paddingBottomUnit.setName("paddingBottomUnit"); // NOI18N
        paddingBottomUnit.addActionListener(formListener);

        org.jdesktop.layout.GroupLayout jPanel5Layout = new org.jdesktop.layout.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel5Layout.createSequentialGroup()
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(jPanel5Layout.createSequentialGroup()
                        .add(10, 10, 10)
                        .add(derivePadding))
                    .add(jPanel5Layout.createSequentialGroup()
                        .addContainerGap()
                        .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel8)
                            .add(jLabel9)
                            .add(jLabel10)
                            .add(jLabel11))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(paddingRight, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 75, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(paddingTop, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 75, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(paddingBottom, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 75, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(paddingLeft, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 75, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(paddingBottomUnit, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(paddingTopUnit, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(paddingLeftUnit, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(paddingRightUnit, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                .add(219, 219, 219)
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(deriveHelp3)
                    .add(paddingHelp))
                .addContainerGap())
        );

        jPanel5Layout.linkSize(new java.awt.Component[] {deriveHelp3, paddingHelp}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        jPanel5Layout.linkSize(new java.awt.Component[] {paddingBottom, paddingLeft, paddingRight, paddingTop}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(derivePadding)
                    .add(deriveHelp3))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel8)
                    .add(paddingLeft, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(paddingLeftUnit, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(paddingHelp))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel9)
                    .add(paddingRight, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(paddingRightUnit, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel10)
                    .add(paddingTop, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(paddingTopUnit, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel11)
                    .add(paddingBottom, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(paddingBottomUnit, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(198, Short.MAX_VALUE))
        );

        jPanel5Layout.linkSize(new java.awt.Component[] {paddingBottom, paddingLeft, paddingRight, paddingTop}, org.jdesktop.layout.GroupLayout.VERTICAL);

        addTabs.addTab("Padding", jPanel5);

        jPanel6.setName("jPanel6"); // NOI18N
        jPanel6.setOpaque(false);

        jLabel17.setText("Left");
        jLabel17.setName("jLabel17"); // NOI18N

        marginLeft.setEnabled(false);
        marginLeft.setName("marginLeft"); // NOI18N
        marginLeft.addChangeListener(formListener);

        jLabel24.setText("Right");
        jLabel24.setName("jLabel24"); // NOI18N

        marginRight.setEnabled(false);
        marginRight.setName("marginRight"); // NOI18N
        marginRight.addChangeListener(formListener);

        jLabel25.setText("Top");
        jLabel25.setName("jLabel25"); // NOI18N

        marginTop.setEnabled(false);
        marginTop.setName("marginTop"); // NOI18N
        marginTop.addChangeListener(formListener);

        jLabel26.setText("Bottom");
        jLabel26.setName("jLabel26"); // NOI18N

        marginBottom.setEnabled(false);
        marginBottom.setName("marginBottom"); // NOI18N
        marginBottom.addChangeListener(formListener);

        deriveMargin.setSelected(true);
        deriveMargin.setText("Derive");
        deriveMargin.setName("deriveMargin"); // NOI18N
        deriveMargin.addActionListener(formListener);

        deriveHelp4.setText("Derive Help");
        deriveHelp4.setName("deriveHelp4"); // NOI18N
        deriveHelp4.addActionListener(formListener);

        marginHelp.setText("Margin Help");
        marginHelp.setName("marginHelp"); // NOI18N
        marginHelp.addActionListener(formListener);

        marginLeftUnit.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Pixels", "Screen Size Percentage", "Millimeters (approximate)" }));
        marginLeftUnit.setName("marginLeftUnit"); // NOI18N
        marginLeftUnit.addActionListener(formListener);

        marginRightUnit.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Pixels", "Screen Size Percentage", "Millimeters (approximate)" }));
        marginRightUnit.setName("marginRightUnit"); // NOI18N
        marginRightUnit.addActionListener(formListener);

        marginTopUnit.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Pixels", "Screen Size Percentage", "Millimeters (approximate)" }));
        marginTopUnit.setName("marginTopUnit"); // NOI18N
        marginTopUnit.addActionListener(formListener);

        marginBottomUnit.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Pixels", "Screen Size Percentage", "Millimeters (approximate)" }));
        marginBottomUnit.setName("marginBottomUnit"); // NOI18N
        marginBottomUnit.addActionListener(formListener);

        org.jdesktop.layout.GroupLayout jPanel6Layout = new org.jdesktop.layout.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel6Layout.createSequentialGroup()
                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel6Layout.createSequentialGroup()
                        .addContainerGap()
                        .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel17)
                            .add(jLabel24)
                            .add(jLabel25)
                            .add(jLabel26))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(marginBottom)
                            .add(marginTop)
                            .add(marginRight)
                            .add(marginLeft, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 100, Short.MAX_VALUE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(marginBottomUnit, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(marginTopUnit, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jPanel6Layout.createSequentialGroup()
                                .add(marginLeftUnit, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(201, 201, 201)
                                .add(marginHelp))
                            .add(marginRightUnit, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel6Layout.createSequentialGroup()
                        .add(10, 10, 10)
                        .add(deriveMargin)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 518, Short.MAX_VALUE)
                        .add(deriveHelp4)))
                .addContainerGap())
        );

        jPanel6Layout.linkSize(new java.awt.Component[] {deriveHelp4, marginHelp}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        jPanel6Layout.linkSize(new java.awt.Component[] {marginBottomUnit, marginLeftUnit, marginRightUnit, marginTopUnit}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel6Layout.createSequentialGroup()
                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel6Layout.createSequentialGroup()
                        .addContainerGap()
                        .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(deriveMargin)
                            .add(deriveHelp4))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.CENTER)
                            .add(marginLeftUnit, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(marginLeft, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel17))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.CENTER)
                            .add(jLabel24)
                            .add(marginRight, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(marginRightUnit, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.CENTER)
                            .add(jLabel25)
                            .add(marginTop, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(marginTopUnit, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.CENTER)
                            .add(jLabel26)
                            .add(marginBottom, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(marginBottomUnit, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                    .add(jPanel6Layout.createSequentialGroup()
                        .add(55, 55, 55)
                        .add(marginHelp)))
                .addContainerGap(200, Short.MAX_VALUE))
        );

        jPanel6Layout.linkSize(new java.awt.Component[] {marginBottom, marginLeft, marginRight, marginTop}, org.jdesktop.layout.GroupLayout.VERTICAL);

        jPanel6Layout.linkSize(new java.awt.Component[] {marginBottomUnit, marginLeftUnit, marginRightUnit, marginTopUnit}, org.jdesktop.layout.GroupLayout.VERTICAL);

        addTabs.addTab("Margin", jPanel6);

        jPanel8.setName("jPanel8"); // NOI18N
        jPanel8.setOpaque(false);

        borderLabel.setText("Border");
        borderLabel.setName("borderLabel"); // NOI18N

        customizeBorder.setText("...");
        customizeBorder.setEnabled(false);
        customizeBorder.setName("customizeBorder"); // NOI18N
        customizeBorder.addActionListener(formListener);

        deriveBorder.setSelected(true);
        deriveBorder.setText("Derive");
        deriveBorder.setName("deriveBorder"); // NOI18N
        deriveBorder.addActionListener(formListener);

        imageBorderWizard.setText("Image Border Wizard");
        imageBorderWizard.setName("imageBorderWizard"); // NOI18N
        imageBorderWizard.addActionListener(formListener);

        jScrollPane1.setBorder(null);
        jScrollPane1.setName("jScrollPane1"); // NOI18N
        jScrollPane1.setOpaque(false);

        jTextArea1.setColumns(20);
        jTextArea1.setEditable(false);
        jTextArea1.setLineWrap(true);
        jTextArea1.setRows(5);
        jTextArea1.setText("Please notice when using the image border wizard to generate images you are in effect creating additional images in the theme. This means that if you use this wizard you MUST NOT cancel this dialog since the images created by the wizard would remain! You would need to go and delete them (try the \"delete unused images\" option in the menu).\nHaving too many images in the theme can be expensive so try to reuse the same images for multiple component types rather than recreate these images over and over again.");
        jTextArea1.setWrapStyleWord(true);
        jTextArea1.setBorder(null);
        jTextArea1.setName("jTextArea1"); // NOI18N
        jTextArea1.setOpaque(false);
        jScrollPane1.setViewportView(jTextArea1);

        deriveHelp5.setText("Derive Help");
        deriveHelp5.setName("deriveHelp5"); // NOI18N
        deriveHelp5.addActionListener(formListener);

        borderHelp.setText("Border Help");
        borderHelp.setName("borderHelp"); // NOI18N
        borderHelp.addActionListener(formListener);

        org.jdesktop.layout.GroupLayout jPanel8Layout = new org.jdesktop.layout.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel8Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 697, Short.MAX_VALUE)
                    .add(imageBorderWizard)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel8Layout.createSequentialGroup()
                        .add(jPanel8Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jPanel8Layout.createSequentialGroup()
                                .add(deriveBorder)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 343, Short.MAX_VALUE))
                            .add(jPanel8Layout.createSequentialGroup()
                                .add(borderLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 415, Short.MAX_VALUE)
                                .add(23, 23, 23)))
                        .add(61, 61, 61)
                        .add(jPanel8Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel8Layout.createSequentialGroup()
                                .add(customizeBorder)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(borderHelp))
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, deriveHelp5))))
                .addContainerGap())
        );

        jPanel8Layout.linkSize(new java.awt.Component[] {borderHelp, deriveHelp5}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel8Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(deriveBorder)
                    .add(deriveHelp5))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel8Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(borderLabel)
                    .add(customizeBorder)
                    .add(borderHelp))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(imageBorderWizard)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 250, Short.MAX_VALUE)
                .addContainerGap())
        );

        addTabs.addTab("Border", jPanel8);

        jPanel10.setName("jPanel10"); // NOI18N
        jPanel10.setOpaque(false);

        baseStyle.setEditable(true);
        baseStyle.setEnabled(false);
        baseStyle.setName("baseStyle"); // NOI18N

        baseStyleType.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Unselected", "Selected", "Pressed", "Disabled" }));
        baseStyleType.setEnabled(false);
        baseStyleType.setName("baseStyleType"); // NOI18N

        defineAttribute.setSelected(true);
        defineAttribute.setText("Override Attribute");
        defineAttribute.setName("defineAttribute"); // NOI18N
        defineAttribute.addActionListener(formListener);

        deriveHelp6.setText("Derive Help");
        deriveHelp6.setName("deriveHelp6"); // NOI18N
        deriveHelp6.addActionListener(formListener);

        org.jdesktop.layout.GroupLayout jPanel10Layout = new org.jdesktop.layout.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel10Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel10Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel10Layout.createSequentialGroup()
                        .add(defineAttribute)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 441, Short.MAX_VALUE)
                        .add(deriveHelp6))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel10Layout.createSequentialGroup()
                        .add(baseStyle, 0, 560, Short.MAX_VALUE)
                        .add(18, 18, 18)
                        .add(baseStyleType, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel10Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel10Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(defineAttribute)
                    .add(deriveHelp6))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel10Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(baseStyle, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(baseStyleType, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(309, Short.MAX_VALUE))
        );

        addTabs.addTab("Derive", jPanel10);

        jPanel7.setName("jPanel7"); // NOI18N
        jPanel7.setOpaque(false);

        buttonGroup1.add(bitmapFont);
        bitmapFont.setText("Bitmap Fonts (deprecated!)");
        bitmapFont.setEnabled(false);
        bitmapFont.setName("bitmapFont"); // NOI18N
        bitmapFont.addActionListener(formListener);

        buttonGroup1.add(systemFont);
        systemFont.setSelected(true);
        systemFont.setText("Standard Font");
        systemFont.setEnabled(false);
        systemFont.setName("systemFont"); // NOI18N
        systemFont.addActionListener(formListener);

        bitmapFontValue.setEnabled(false);
        bitmapFontValue.setName("bitmapFontValue"); // NOI18N
        bitmapFontValue.setPrototypeDisplayValue("XXXXXXXXXXXXXXXX");
        bitmapFontValue.addActionListener(formListener);

        addNewBitmapFont.setText("...");
        addNewBitmapFont.setEnabled(false);
        addNewBitmapFont.setName("addNewBitmapFont"); // NOI18N
        addNewBitmapFont.addActionListener(formListener);

        fontFace.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "SYSTEM", "MONOSPACE", "PROPORTIONAL" }));
        fontFace.setEnabled(false);
        fontFace.setName("fontFace"); // NOI18N
        fontFace.addActionListener(formListener);

        fontStyle.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "PLAIN", "BOLD", "ITALIC", "BOLD ITALIC" }));
        fontStyle.setEnabled(false);
        fontStyle.setName("fontStyle"); // NOI18N
        fontStyle.addActionListener(formListener);

        fontSize.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "MEDIUM", "SMALL", "LARGE" }));
        fontSize.setEnabled(false);
        fontSize.setName("fontSize"); // NOI18N
        fontSize.addActionListener(formListener);

        deriveFont.setSelected(true);
        deriveFont.setText("Derive Font");
        deriveFont.setName("deriveFont"); // NOI18N
        deriveFont.addActionListener(formListener);

        textDecorationCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "None", "Underline", "Strike Through,", "3D Text Raised", "3D Text Lowered" }));
        textDecorationCombo.setEnabled(false);
        textDecorationCombo.setName("textDecorationCombo"); // NOI18N
        textDecorationCombo.addActionListener(formListener);

        deriveTextDecoration.setSelected(true);
        deriveTextDecoration.setText("Derive Text Decoration");
        deriveTextDecoration.setName("deriveTextDecoration"); // NOI18N
        deriveTextDecoration.addActionListener(formListener);

        jLabel7.setText("Text Decoration");
        jLabel7.setName("jLabel7"); // NOI18N

        deriveHelp7.setText("Derive Help");
        deriveHelp7.setName("deriveHelp7"); // NOI18N
        deriveHelp7.addActionListener(formListener);

        fontHelp.setText("Font Help");
        fontHelp.setName("fontHelp"); // NOI18N
        fontHelp.addActionListener(formListener);

        jLabel12.setText("True Type");
        jLabel12.setToolTipText("<html><body>\nTruetype fonts are only supported on some platforms (iOS/Android)<br>\nto use them you need to place the file in the src directory next to the<br>\nresource file and make sure the name of the font is correct in the<br>\ntext field (for iOS). When unavailable the standard fonts will be used.<br>\n<b>Important</b> the file name must have a .ttf extension!");
        jLabel12.setName("jLabel12"); // NOI18N

        trueTypeFont.setToolTipText("<html><body>\nTruetype fonts are only supported on some platforms (iOS/Android)<br>\nto use them you need to place the file in the src directory next to the<br>\nresource file and make sure the name of the font is correct in the<br>\ntext field (for iOS). When unavailable the standard fonts will be used.<br>\n<b>Important</b> the file name must have a .ttf extension!");
        trueTypeFont.setEnabled(false);
        trueTypeFont.setName("trueTypeFont"); // NOI18N
        trueTypeFont.addActionListener(formListener);

        jLabel13.setText("True Type Size");
        jLabel13.setToolTipText("<html><body>\nTruetype fonts are only supported on some platforms (iOS/Android)<br>\nto use them you need to place the file in the src directory next to the<br>\nresource file and make sure the name of the font is correct in the<br>\ntext field (for iOS). When unavailable the standard fonts will be used.<br>\n<b>Important</b> the file name must have a .ttf extension!"); // NOI18N
        jLabel13.setName("jLabel13"); // NOI18N

        trueTypeFontSizeOption.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Small", "Medium", "Large", "Millimeters", "Pixels" }));
        trueTypeFontSizeOption.setSelectedIndex(1);
        trueTypeFontSizeOption.setToolTipText("<html><body>\nTruetype fonts are only supported on some platforms (iOS/Android)<br>\nto use them you need to place the file in the src directory next to the<br>\nresource file and make sure the name of the font is correct in the<br>\ntext field (for iOS). When unavailable the standard fonts will be used.<br>\n<b>Important</b> the file name must have a .ttf extension!");
        trueTypeFontSizeOption.setEnabled(false);
        trueTypeFontSizeOption.setName("trueTypeFontSizeOption"); // NOI18N
        trueTypeFontSizeOption.addActionListener(formListener);

        trueTypeFontSizeValue.setToolTipText("<html><body>\nTruetype fonts are only supported on some platforms (iOS/Android)<br>\nto use them you need to place the file in the src directory next to the<br>\nresource file and make sure the name of the font is correct in the<br>\ntext field (for iOS). When unavailable the standard fonts will be used.<br>\n<b>Important</b> the file name must have a .ttf extension!");
        trueTypeFontSizeValue.setEnabled(false);
        trueTypeFontSizeValue.setName("trueTypeFontSizeValue"); // NOI18N
        trueTypeFontSizeValue.addChangeListener(formListener);

        org.jdesktop.layout.GroupLayout jPanel7Layout = new org.jdesktop.layout.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel7Layout.createSequentialGroup()
                        .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, deriveFont)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel7Layout.createSequentialGroup()
                                .add(systemFont)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(bitmapFont)))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 185, Short.MAX_VALUE)
                        .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(fontHelp)
                            .add(deriveHelp7))
                        .addContainerGap())
                    .add(jPanel7Layout.createSequentialGroup()
                        .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel7Layout.createSequentialGroup()
                                .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(jLabel13)
                                    .add(jLabel12))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(trueTypeFontSizeOption, 0, 147, Short.MAX_VALUE)
                                    .add(trueTypeFont, 0, 147, Short.MAX_VALUE)))
                            .add(org.jdesktop.layout.GroupLayout.LEADING, fontFace, 0, 243, Short.MAX_VALUE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(trueTypeFontSizeValue, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 214, Short.MAX_VALUE)
                            .add(fontStyle, 0, 214, Short.MAX_VALUE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(fontSize, 0, 213, Short.MAX_VALUE)
                        .add(33, 33, 33))
                    .add(jPanel7Layout.createSequentialGroup()
                        .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(deriveTextDecoration)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel7Layout.createSequentialGroup()
                                .add(jLabel7)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(textDecorationCombo, 0, 465, Short.MAX_VALUE))
                            .add(jPanel7Layout.createSequentialGroup()
                                .add(bitmapFontValue, 0, 490, Short.MAX_VALUE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(addNewBitmapFont)))
                        .add(146, 146, 146))))
        );

        jPanel7Layout.linkSize(new java.awt.Component[] {bitmapFont, systemFont}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        jPanel7Layout.linkSize(new java.awt.Component[] {deriveHelp7, fontHelp}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jPanel7Layout.createSequentialGroup()
                        .add(deriveFont)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(bitmapFont)
                            .add(systemFont)))
                    .add(jPanel7Layout.createSequentialGroup()
                        .add(deriveHelp7)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(fontHelp)))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(fontFace, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(fontStyle, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(fontSize, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel12)
                    .add(trueTypeFont, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(9, 9, 9)
                .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                        .add(jLabel13)
                        .add(trueTypeFontSizeOption, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(trueTypeFontSizeValue, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(addNewBitmapFont)
                    .add(bitmapFontValue, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(deriveTextDecoration)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(textDecorationCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel7))
                .addContainerGap(108, Short.MAX_VALUE))
        );

        addTabs.addTab("Font", jPanel7);

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        help.setContentType("text/html");
        help.setEditable(false);
        help.setName("help"); // NOI18N
        jScrollPane2.setViewportView(help);

        addTabs.addTab("Help", jScrollPane2);

        styleType.setText("Unselected");
        styleType.setName("styleType"); // NOI18N

        styleHelp.setText("Component Help");
        styleHelp.setName("styleHelp"); // NOI18N
        styleHelp.addActionListener(formListener);

        videoTutorial.setText("Video Tutorial");
        videoTutorial.setName("videoTutorial"); // NOI18N
        videoTutorial.addActionListener(formListener);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, previewPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 758, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, addTabs, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 758, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, layout.createSequentialGroup()
                        .add(jLabel1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(componentName, 0, 290, Short.MAX_VALUE)
                        .add(18, 18, 18)
                        .add(styleType)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(styleHelp)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(videoTutorial))
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jLabel6))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(componentName, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(styleType)
                    .add(styleHelp)
                    .add(videoTutorial))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(addTabs, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 437, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jLabel6)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(previewPane, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 92, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }

    // Code for dispatching events from components to event handlers.

    private class FormListener implements java.awt.event.ActionListener, javax.swing.event.ChangeListener {
        FormListener() {}
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            if (evt.getSource() == componentName) {
                AddThemeEntry.this.componentNameActionPerformed(evt);
            }
            else if (evt.getSource() == imagesCombo) {
                AddThemeEntry.this.imagesComboActionPerformed(evt);
            }
            else if (evt.getSource() == addNewImage) {
                AddThemeEntry.this.addNewImageActionPerformed(evt);
            }
            else if (evt.getSource() == backgroundType) {
                AddThemeEntry.this.backgroundTypeActionPerformed(evt);
            }
            else if (evt.getSource() == changeGradientStartColorButton) {
                AddThemeEntry.this.changeGradientStartColorButtonActionPerformed(evt);
            }
            else if (evt.getSource() == changeGradientEndColorButton) {
                AddThemeEntry.this.changeGradientEndColorButtonActionPerformed(evt);
            }
            else if (evt.getSource() == deriveBackground) {
                AddThemeEntry.this.deriveBackgroundActionPerformed(evt);
            }
            else if (evt.getSource() == deriveHelp) {
                AddThemeEntry.this.deriveHelpActionPerformed(evt);
            }
            else if (evt.getSource() == backgroundHelp) {
                AddThemeEntry.this.backgroundHelpActionPerformed(evt);
            }
            else if (evt.getSource() == changeColorButtonFG) {
                AddThemeEntry.this.changeColorButtonFGActionPerformed(evt);
            }
            else if (evt.getSource() == deriveForegroundColor) {
                AddThemeEntry.this.deriveForegroundColorActionPerformed(evt);
            }
            else if (evt.getSource() == changeColorButtonBG) {
                AddThemeEntry.this.changeColorButtonBGActionPerformed(evt);
            }
            else if (evt.getSource() == deriveBackgroundColor) {
                AddThemeEntry.this.deriveBackgroundColorActionPerformed(evt);
            }
            else if (evt.getSource() == deriveTransparency) {
                AddThemeEntry.this.deriveTransparencyActionPerformed(evt);
            }
            else if (evt.getSource() == deriveHelp1) {
                AddThemeEntry.this.deriveHelp1ActionPerformed(evt);
            }
            else if (evt.getSource() == colorHelp) {
                AddThemeEntry.this.colorHelpActionPerformed(evt);
            }
            else if (evt.getSource() == alignmentCombo) {
                AddThemeEntry.this.alignmentComboActionPerformed(evt);
            }
            else if (evt.getSource() == deriveAlignment) {
                AddThemeEntry.this.deriveAlignmentActionPerformed(evt);
            }
            else if (evt.getSource() == deriveHelp2) {
                AddThemeEntry.this.deriveHelp2ActionPerformed(evt);
            }
            else if (evt.getSource() == alignHelp) {
                AddThemeEntry.this.alignHelpActionPerformed(evt);
            }
            else if (evt.getSource() == derivePadding) {
                AddThemeEntry.this.derivePaddingActionPerformed(evt);
            }
            else if (evt.getSource() == deriveHelp3) {
                AddThemeEntry.this.deriveHelp3ActionPerformed(evt);
            }
            else if (evt.getSource() == paddingHelp) {
                AddThemeEntry.this.paddingHelpActionPerformed(evt);
            }
            else if (evt.getSource() == paddingLeftUnit) {
                AddThemeEntry.this.paddingLeftUnitActionPerformed(evt);
            }
            else if (evt.getSource() == paddingRightUnit) {
                AddThemeEntry.this.paddingRightUnitActionPerformed(evt);
            }
            else if (evt.getSource() == paddingTopUnit) {
                AddThemeEntry.this.paddingTopUnitActionPerformed(evt);
            }
            else if (evt.getSource() == paddingBottomUnit) {
                AddThemeEntry.this.paddingBottomUnitActionPerformed(evt);
            }
            else if (evt.getSource() == deriveMargin) {
                AddThemeEntry.this.deriveMarginActionPerformed(evt);
            }
            else if (evt.getSource() == deriveHelp4) {
                AddThemeEntry.this.deriveHelp4ActionPerformed(evt);
            }
            else if (evt.getSource() == marginHelp) {
                AddThemeEntry.this.marginHelpActionPerformed(evt);
            }
            else if (evt.getSource() == marginLeftUnit) {
                AddThemeEntry.this.marginLeftUnitActionPerformed(evt);
            }
            else if (evt.getSource() == marginRightUnit) {
                AddThemeEntry.this.marginRightUnitActionPerformed(evt);
            }
            else if (evt.getSource() == marginTopUnit) {
                AddThemeEntry.this.marginTopUnitActionPerformed(evt);
            }
            else if (evt.getSource() == marginBottomUnit) {
                AddThemeEntry.this.marginBottomUnitActionPerformed(evt);
            }
            else if (evt.getSource() == customizeBorder) {
                AddThemeEntry.this.customizeBorderActionPerformed(evt);
            }
            else if (evt.getSource() == deriveBorder) {
                AddThemeEntry.this.deriveBorderActionPerformed(evt);
            }
            else if (evt.getSource() == imageBorderWizard) {
                AddThemeEntry.this.imageBorderWizardActionPerformed(evt);
            }
            else if (evt.getSource() == deriveHelp5) {
                AddThemeEntry.this.deriveHelp5ActionPerformed(evt);
            }
            else if (evt.getSource() == borderHelp) {
                AddThemeEntry.this.borderHelpActionPerformed(evt);
            }
            else if (evt.getSource() == defineAttribute) {
                AddThemeEntry.this.defineAttributeActionPerformed(evt);
            }
            else if (evt.getSource() == deriveHelp6) {
                AddThemeEntry.this.deriveHelp6ActionPerformed(evt);
            }
            else if (evt.getSource() == bitmapFont) {
                AddThemeEntry.this.bitmapFontActionPerformed(evt);
            }
            else if (evt.getSource() == systemFont) {
                AddThemeEntry.this.systemFontActionPerformed(evt);
            }
            else if (evt.getSource() == bitmapFontValue) {
                AddThemeEntry.this.bitmapFontValueActionPerformed(evt);
            }
            else if (evt.getSource() == addNewBitmapFont) {
                AddThemeEntry.this.addNewBitmapFontActionPerformed(evt);
            }
            else if (evt.getSource() == fontFace) {
                AddThemeEntry.this.fontFaceActionPerformed(evt);
            }
            else if (evt.getSource() == fontStyle) {
                AddThemeEntry.this.actionInEditableComponent(evt);
            }
            else if (evt.getSource() == fontSize) {
                AddThemeEntry.this.fontSizeActionPerformed(evt);
            }
            else if (evt.getSource() == deriveFont) {
                AddThemeEntry.this.deriveFontActionPerformed(evt);
            }
            else if (evt.getSource() == textDecorationCombo) {
                AddThemeEntry.this.textDecorationComboActionPerformed(evt);
            }
            else if (evt.getSource() == deriveTextDecoration) {
                AddThemeEntry.this.deriveTextDecorationActionPerformed(evt);
            }
            else if (evt.getSource() == deriveHelp7) {
                AddThemeEntry.this.deriveHelp7ActionPerformed(evt);
            }
            else if (evt.getSource() == fontHelp) {
                AddThemeEntry.this.fontHelpActionPerformed(evt);
            }
            else if (evt.getSource() == trueTypeFont) {
                AddThemeEntry.this.trueTypeFontActionPerformed(evt);
            }
            else if (evt.getSource() == trueTypeFontSizeOption) {
                AddThemeEntry.this.trueTypeFontSizeOptionActionPerformed(evt);
            }
            else if (evt.getSource() == styleHelp) {
                AddThemeEntry.this.styleHelpActionPerformed(evt);
            }
            else if (evt.getSource() == videoTutorial) {
                AddThemeEntry.this.videoTutorialActionPerformed(evt);
            }
        }

        public void stateChanged(javax.swing.event.ChangeEvent evt) {
            if (evt.getSource() == gradientX) {
                AddThemeEntry.this.gradientXStateChanged(evt);
            }
            else if (evt.getSource() == gradientY) {
                AddThemeEntry.this.gradientYStateChanged(evt);
            }
            else if (evt.getSource() == gradientSize) {
                AddThemeEntry.this.gradientSizeStateChanged(evt);
            }
            else if (evt.getSource() == transparencyValue) {
                AddThemeEntry.this.transparencyValueStateChanged(evt);
            }
            else if (evt.getSource() == paddingLeft) {
                AddThemeEntry.this.paddingLeftStateChanged(evt);
            }
            else if (evt.getSource() == paddingRight) {
                AddThemeEntry.this.spinnerChanged(evt);
            }
            else if (evt.getSource() == paddingTop) {
                AddThemeEntry.this.paddingTopStateChanged(evt);
            }
            else if (evt.getSource() == paddingBottom) {
                AddThemeEntry.this.paddingBottomStateChanged(evt);
            }
            else if (evt.getSource() == marginLeft) {
                AddThemeEntry.this.marginLeftStateChanged(evt);
            }
            else if (evt.getSource() == marginRight) {
                AddThemeEntry.this.marginRightspinnerChanged(evt);
            }
            else if (evt.getSource() == marginTop) {
                AddThemeEntry.this.marginTopStateChanged(evt);
            }
            else if (evt.getSource() == marginBottom) {
                AddThemeEntry.this.marginBottomStateChanged(evt);
            }
            else if (evt.getSource() == trueTypeFontSizeValue) {
                AddThemeEntry.this.trueTypeFontSizeValueStateChanged(evt);
            }
        }
    }// </editor-fold>//GEN-END:initComponents
    
    private void addNewBitmapFontActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addNewBitmapFontActionPerformed
        view.addNewFontWizard();
        bitmapFontValue.setModel(new DefaultComboBoxModel(resources.getFontResourceNames()));        
    }//GEN-LAST:event_addNewBitmapFontActionPerformed

    private void actionInEditableComponent(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_actionInEditableComponent
        updateThemePreview();
    }//GEN-LAST:event_actionInEditableComponent

    private void spinnerChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerChanged
        updateThemePreview();
    }//GEN-LAST:event_spinnerChanged

private void componentNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_componentNameActionPerformed
        if(disableRefresh) {
            return;
        }
        themeHash.clear();
        themeHash.putAll(originalTheme);
        updateThemePreview();
}//GEN-LAST:event_componentNameActionPerformed

private void fontFaceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fontFaceActionPerformed
        updateThemePreview();
}//GEN-LAST:event_fontFaceActionPerformed

private void fontSizeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fontSizeActionPerformed
        updateThemePreview();
}//GEN-LAST:event_fontSizeActionPerformed

private void bitmapFontActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bitmapFontActionPerformed
        fontFace.setEnabled(false);
        fontSize.setEnabled(false);
        fontStyle.setEnabled(false);
        trueTypeFont.setEnabled(false);
        trueTypeFontSizeOption.setEnabled(false);
        trueTypeFontSizeValue.setEnabled(false);
        bitmapFontValue.setEnabled(true);
        addNewBitmapFont.setEnabled(true);
        updateThemePreview();
}//GEN-LAST:event_bitmapFontActionPerformed

private void systemFontActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_systemFontActionPerformed
        fontFace.setEnabled(true);
        fontSize.setEnabled(true);
        fontStyle.setEnabled(true);
        trueTypeFont.setEnabled(trueTypeFont.getModel().getSize() > 0);
        trueTypeFontSizeOption.setEnabled(trueTypeFont.getModel().getSize() > 0);
        trueTypeFontSizeValue.setEnabled(trueTypeFont.getModel().getSize() > 0);
        bitmapFontValue.setEnabled(false);
        addNewBitmapFont.setEnabled(false);
        updateThemePreview();
}//GEN-LAST:event_systemFontActionPerformed

private void paddingLeftStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_paddingLeftStateChanged
        updateThemePreview();
}//GEN-LAST:event_paddingLeftStateChanged

private void paddingTopStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_paddingTopStateChanged
        updateThemePreview();
}//GEN-LAST:event_paddingTopStateChanged

private void paddingBottomStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_paddingBottomStateChanged
        updateThemePreview();
}//GEN-LAST:event_paddingBottomStateChanged

private void transparencyValueStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_transparencyValueStateChanged
        updateThemePreview();
}//GEN-LAST:event_transparencyValueStateChanged

private void bitmapFontValueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bitmapFontValueActionPerformed
        updateThemePreview();
}//GEN-LAST:event_bitmapFontValueActionPerformed

private JColorChooser colorChooser;

private void pickColor(final JTextComponent colorText) {
        int color = Integer.decode("0x" + colorText.getText());
        if(colorChooser == null) {
            colorChooser = new JColorChooser();
        }
        colorChooser.setColor(color);

        JDialog dlg = JColorChooser.createDialog(this, "Pick color", true, colorChooser, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int i = colorChooser.getColor().getRGB() & 0xffffff;
                colorText.setText(Integer.toHexString(i));
                //themeHash.put(getKey(), getValue());
                updateThemeHashtable(themeHash);
                refreshTheme(themeHash);
            }
        }, null);
        dlg.setLocationByPlatform(true);
        dlg.pack();
        dlg.setVisible(true);
}

private void customizeBorderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_customizeBorderActionPerformed
    BorderEditor editor = new BorderEditor(currentBorder, resources);
    JDialog dialog = new JDialog((JDialog)SwingUtilities.windowForComponent(this), "Border");
    dialog.setLayout(new BorderLayout());
    dialog.add(BorderLayout.CENTER, editor);
    dialog.pack();
    dialog.setLocationRelativeTo(customizeBorder);
    dialog.setModal(true);
    dialog.setVisible(true);
    currentBorder = editor.getResult();
    updateThemePreview();
}//GEN-LAST:event_customizeBorderActionPerformed

private void changeGradientStartColorButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_changeGradientStartColorButtonActionPerformed
    pickColor(gradientStartColor);
}//GEN-LAST:event_changeGradientStartColorButtonActionPerformed

private void changeGradientEndColorButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_changeGradientEndColorButtonActionPerformed
    pickColor(gradientEndColor);
}//GEN-LAST:event_changeGradientEndColorButtonActionPerformed

private void backgroundTypeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backgroundTypeActionPerformed
    updateThemePreview();
}//GEN-LAST:event_backgroundTypeActionPerformed

private void gradientXStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_gradientXStateChanged
    updateThemePreview();
}//GEN-LAST:event_gradientXStateChanged

private void gradientYStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_gradientYStateChanged
    updateThemePreview();
}//GEN-LAST:event_gradientYStateChanged

private void gradientSizeStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_gradientSizeStateChanged
    updateThemePreview();
}//GEN-LAST:event_gradientSizeStateChanged

private void changeColorButtonFGActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_changeColorButtonFGActionPerformed
     pickColor(colorValueFG);
}//GEN-LAST:event_changeColorButtonFGActionPerformed

private void imagesComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_imagesComboActionPerformed
     updateThemePreview();
}//GEN-LAST:event_imagesComboActionPerformed

private void addNewImageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addNewImageActionPerformed
    AddResourceDialog addResource = new AddResourceDialog(resources, AddResourceDialog.IMAGE, false);

    if(JOptionPane.OK_OPTION ==
        JOptionPane.showConfirmDialog(this, addResource, "Select Name",
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)) {
        addResource.addResource(resources, null);

        initImagesCombo();
    }
}//GEN-LAST:event_addNewImageActionPerformed

private void changeColorButtonBGActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_changeColorButtonBGActionPerformed
     pickColor(colorValueBG);
}//GEN-LAST:event_changeColorButtonBGActionPerformed

private void marginLeftStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_marginLeftStateChanged
        updateThemePreview();
}//GEN-LAST:event_marginLeftStateChanged

private void marginRightspinnerChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_marginRightspinnerChanged
        updateThemePreview();
}//GEN-LAST:event_marginRightspinnerChanged

private void marginTopStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_marginTopStateChanged
        updateThemePreview();
}//GEN-LAST:event_marginTopStateChanged

private void marginBottomStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_marginBottomStateChanged
        updateThemePreview();
}//GEN-LAST:event_marginBottomStateChanged

private void deriveBackgroundActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deriveBackgroundActionPerformed
    imagesCombo.setEnabled(!deriveBackground.isSelected());
    addNewImage.setEnabled(!deriveBackground.isSelected());
    backgroundType.setEnabled(!deriveBackground.isSelected());
    gradientStartColor.setEnabled(!deriveBackground.isSelected());
    gradientEndColor.setEnabled(!deriveBackground.isSelected());
    gradientSize.setEnabled(!deriveBackground.isSelected());
    gradientX.setEnabled(!deriveBackground.isSelected());
    gradientY.setEnabled(!deriveBackground.isSelected());
    changeGradientEndColorButton.setEnabled(!deriveBackground.isSelected());
    changeGradientStartColorButton.setEnabled(!deriveBackground.isSelected());
    updateThemePreview();
}//GEN-LAST:event_deriveBackgroundActionPerformed

private void deriveForegroundColorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deriveForegroundColorActionPerformed
    colorValueFG.setEnabled(!deriveForegroundColor.isSelected());
    changeColorButtonFG.setEnabled(!deriveForegroundColor.isSelected());
    updateThemePreview();
}//GEN-LAST:event_deriveForegroundColorActionPerformed

private void deriveBackgroundColorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deriveBackgroundColorActionPerformed
    colorValueBG.setEnabled(!deriveBackgroundColor.isSelected());
    changeColorButtonBG.setEnabled(!deriveBackgroundColor.isSelected());
    updateThemePreview();
}//GEN-LAST:event_deriveBackgroundColorActionPerformed

private void deriveTransparencyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deriveTransparencyActionPerformed
    transparencyValue.setEnabled(!deriveTransparency.isSelected());
    updateThemePreview();
}//GEN-LAST:event_deriveTransparencyActionPerformed

private void deriveAlignmentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deriveAlignmentActionPerformed
    alignmentCombo.setEnabled(!deriveAlignment.isSelected());
    updateThemePreview();
}//GEN-LAST:event_deriveAlignmentActionPerformed

private void derivePaddingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_derivePaddingActionPerformed
    paddingBottom.setEnabled(!derivePadding.isSelected());
    paddingTop.setEnabled(!derivePadding.isSelected());
    paddingLeft.setEnabled(!derivePadding.isSelected());
    paddingRight.setEnabled(!derivePadding.isSelected());
    paddingBottomUnit.setEnabled(!derivePadding.isSelected());
    paddingTopUnit.setEnabled(!derivePadding.isSelected());
    paddingLeftUnit.setEnabled(!derivePadding.isSelected());
    paddingRightUnit.setEnabled(!derivePadding.isSelected());
    updateThemePreview();
}//GEN-LAST:event_derivePaddingActionPerformed

private void deriveMarginActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deriveMarginActionPerformed
    marginBottom.setEnabled(!deriveMargin.isSelected());
    marginTop.setEnabled(!deriveMargin.isSelected());
    marginLeft.setEnabled(!deriveMargin.isSelected());
    marginRight.setEnabled(!deriveMargin.isSelected());
    marginBottomUnit.setEnabled(!deriveMargin.isSelected());
    marginTopUnit.setEnabled(!deriveMargin.isSelected());
    marginLeftUnit.setEnabled(!deriveMargin.isSelected());
    marginRightUnit.setEnabled(!deriveMargin.isSelected());
    updateThemePreview();
}//GEN-LAST:event_deriveMarginActionPerformed

private void deriveBorderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deriveBorderActionPerformed
    customizeBorder.setEnabled(!deriveBorder.isSelected());
    updateThemePreview();
}//GEN-LAST:event_deriveBorderActionPerformed

private void defineAttributeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_defineAttributeActionPerformed
    baseStyle.setEnabled(!defineAttribute.isSelected());
    baseStyleType.setEnabled(!defineAttribute.isSelected());
    updateThemePreview();
}//GEN-LAST:event_defineAttributeActionPerformed

private void deriveFontActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deriveFontActionPerformed
    bitmapFont.setEnabled(!deriveFont.isSelected());
    systemFont.setEnabled(!deriveFont.isSelected());
    boolean enableBitmap = bitmapFont.isSelected() && !deriveFont.isSelected();
    boolean enableSystem = !bitmapFont.isSelected() && !deriveFont.isSelected();
    bitmapFontValue.setEnabled(enableBitmap);
    addNewBitmapFont.setEnabled(enableBitmap);
    fontFace.setEnabled(enableSystem);
    fontSize.setEnabled(enableSystem);
    fontStyle.setEnabled(enableSystem);
    trueTypeFont.setEnabled(enableSystem && trueTypeFont.getModel().getSize() > 0);
    trueTypeFontSizeOption.setEnabled(enableSystem && trueTypeFont.getModel().getSize() > 0);
    trueTypeFontSizeValue.setEnabled(enableSystem && trueTypeFont.getModel().getSize() > 0 && trueTypeFontSizeOption.getSelectedIndex() > 2);
    updateThemePreview();
}//GEN-LAST:event_deriveFontActionPerformed

private void deriveTextDecorationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deriveTextDecorationActionPerformed
    textDecorationCombo.setEnabled(!deriveTextDecoration.isSelected());
    updateThemePreview();
}//GEN-LAST:event_deriveTextDecorationActionPerformed

private void imageBorderWizardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_imageBorderWizardActionPerformed
    deriveBorder.setSelected(false);
    ImageBorderWizardTabbedPane iw = new ImageBorderWizardTabbedPane(resources, themeName);
    String name = (String)componentName.getSelectedItem();
    String uiid;
    if(prefix == null || prefix.length() == 0) {
        uiid = name + ".border";
    } else {
        uiid = name + "." + prefix + "border";
    }
    iw.addToAppliesToList(uiid);
    JDialog dlg = new JDialog(SwingUtilities.windowForComponent(this));
    dlg.setLayout(new java.awt.BorderLayout());
    dlg.add(java.awt.BorderLayout.CENTER, iw);
    dlg.pack();
    dlg.setLocationRelativeTo(this);
    dlg.setModal(true);
    dlg.setVisible(true);
    Border b = (Border)resources.getTheme(themeName).get(uiid);
    if(b != null) {
        currentBorder = b;
        ((CodenameOneComponentWrapper)borderLabel).getCodenameOneComponent().getStyle().setBorder(b);
        borderLabel.repaint();
    }
}//GEN-LAST:event_imageBorderWizardActionPerformed

private void deriveHelpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deriveHelpActionPerformed
    help("derive");
}//GEN-LAST:event_deriveHelpActionPerformed

private void styleHelpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_styleHelpActionPerformed
    help("style");
}//GEN-LAST:event_styleHelpActionPerformed

private void deriveHelp1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deriveHelp1ActionPerformed
    help("derive");
}//GEN-LAST:event_deriveHelp1ActionPerformed

private void deriveHelp2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deriveHelp2ActionPerformed
    help("derive");
}//GEN-LAST:event_deriveHelp2ActionPerformed

private void deriveHelp3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deriveHelp3ActionPerformed
    help("derive");
}//GEN-LAST:event_deriveHelp3ActionPerformed

private void deriveHelp4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deriveHelp4ActionPerformed
    help("derive");
}//GEN-LAST:event_deriveHelp4ActionPerformed

private void deriveHelp5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deriveHelp5ActionPerformed
    help("derive");
}//GEN-LAST:event_deriveHelp5ActionPerformed

private void deriveHelp6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deriveHelp6ActionPerformed
    help("derive");
}//GEN-LAST:event_deriveHelp6ActionPerformed

private void deriveHelp7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deriveHelp7ActionPerformed
    help("derive");
}//GEN-LAST:event_deriveHelp7ActionPerformed

private void backgroundHelpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backgroundHelpActionPerformed
    help("background");
}//GEN-LAST:event_backgroundHelpActionPerformed

private void colorHelpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_colorHelpActionPerformed
    help("color");
}//GEN-LAST:event_colorHelpActionPerformed

private void alignHelpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_alignHelpActionPerformed
    help("align");
}//GEN-LAST:event_alignHelpActionPerformed

private void paddingHelpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_paddingHelpActionPerformed
    help("paddingAndMargin");
}//GEN-LAST:event_paddingHelpActionPerformed

private void marginHelpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_marginHelpActionPerformed
    help("paddingAndMargin");
}//GEN-LAST:event_marginHelpActionPerformed

private void borderHelpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_borderHelpActionPerformed
    help("border");
}//GEN-LAST:event_borderHelpActionPerformed

private void fontHelpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fontHelpActionPerformed
    help("font");
}//GEN-LAST:event_fontHelpActionPerformed

private void textDecorationComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_textDecorationComboActionPerformed
    updateThemePreview();
}//GEN-LAST:event_textDecorationComboActionPerformed

private void alignmentComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_alignmentComboActionPerformed
        updateThemePreview();
}//GEN-LAST:event_alignmentComboActionPerformed

private void videoTutorialActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_videoTutorialActionPerformed
    ResourceEditorView.helpVideo("http://codenameone.blogspot.com/2011/04/mini-tutorial-on-editing-theme.html");
}//GEN-LAST:event_videoTutorialActionPerformed

private void paddingLeftUnitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_paddingLeftUnitActionPerformed
    updateThemePreview();
}//GEN-LAST:event_paddingLeftUnitActionPerformed

private void marginLeftUnitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_marginLeftUnitActionPerformed
    updateThemePreview();
}//GEN-LAST:event_marginLeftUnitActionPerformed

private void marginRightUnitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_marginRightUnitActionPerformed
    updateThemePreview();
}//GEN-LAST:event_marginRightUnitActionPerformed

private void marginTopUnitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_marginTopUnitActionPerformed
    updateThemePreview();
}//GEN-LAST:event_marginTopUnitActionPerformed

private void marginBottomUnitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_marginBottomUnitActionPerformed
    updateThemePreview();
}//GEN-LAST:event_marginBottomUnitActionPerformed

private void paddingRightUnitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_paddingRightUnitActionPerformed
    updateThemePreview();
}//GEN-LAST:event_paddingRightUnitActionPerformed

private void paddingTopUnitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_paddingTopUnitActionPerformed
    updateThemePreview();
}//GEN-LAST:event_paddingTopUnitActionPerformed

private void paddingBottomUnitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_paddingBottomUnitActionPerformed
    updateThemePreview();
}//GEN-LAST:event_paddingBottomUnitActionPerformed

private void trueTypeFontSizeOptionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_trueTypeFontSizeOptionActionPerformed
    trueTypeFontSizeValue.setEnabled(trueTypeFontSizeOption.getSelectedIndex() > 2);
    updateThemePreview();
}//GEN-LAST:event_trueTypeFontSizeOptionActionPerformed

private void trueTypeFontActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_trueTypeFontActionPerformed
        updateThemePreview();
}//GEN-LAST:event_trueTypeFontActionPerformed

private void trueTypeFontSizeValueStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_trueTypeFontSizeValueStateChanged
    updateThemePreview();
}//GEN-LAST:event_trueTypeFontSizeValueStateChanged

    private void updateThemePreview() {
        if(disableRefresh) {
            return;
        }

        updateThemeHashtable(themeHash);
        refreshTheme(themeHash);

        String name = codenameOnePreview.getComponentAt(0).getClass().getName();
        String selectedName = (String)componentName.getSelectedItem();
        if(!name.endsWith(selectedName)) {
            try {
                Class cls = Class.forName("com.codename1.ui." + selectedName);
                com.codename1.ui.Component c = (com.codename1.ui.Component)cls.newInstance();
                if(c instanceof com.codename1.ui.Label) {
                    ((com.codename1.ui.Label)c).setText("Preview");
                } else {
                    if(c instanceof com.codename1.ui.List) {
                        ((com.codename1.ui.List)c).setModel(new
                                com.codename1.ui.list.DefaultListModel(new Object[] {"Preview 1", "Preview 2", "Preview 3"}));
                    }
                }
                codenameOnePreview.removeAll();
                codenameOnePreview.addComponent(com.codename1.ui.layouts.BorderLayout.CENTER, c);
            } catch(Throwable t) {
                codenameOnePreview.removeAll();
                com.codename1.ui.Label l = new com.codename1.ui.Label("Preview");
                l.setUIID(selectedName);
                codenameOnePreview.addComponent(com.codename1.ui.layouts.BorderLayout.CENTER, l);
            }
        }
        com.codename1.ui.plaf.Style s;
        if(prefix != null && prefix.length() > 0) {
            s = com.codename1.ui.plaf.UIManager.getInstance().getComponentCustomStyle(selectedName, prefix.substring(0, prefix.length() - 1));
        } else {
            s = com.codename1.ui.plaf.UIManager.getInstance().getComponentStyle(selectedName);
        }
        codenameOnePreview.getComponentAt(0).setUnselectedStyle(s);
        codenameOnePreview.revalidate();
        previewPane.repaint();
    }
    
    private void refreshTheme(Hashtable theme) {
        if(disableRefresh) {
            return;
        }

        Accessor.setTheme(theme);
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                com.codename1.ui.Form f = Display.getInstance().getCurrent();
                f.refreshTheme();
                f.revalidate();
            }
        });
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addNewBitmapFont;
    private javax.swing.JButton addNewImage;
    private javax.swing.JTabbedPane addTabs;
    private javax.swing.JButton alignHelp;
    private javax.swing.JComboBox alignmentCombo;
    private javax.swing.JButton backgroundHelp;
    private javax.swing.JComboBox backgroundType;
    private javax.swing.JComboBox baseStyle;
    private javax.swing.JComboBox baseStyleType;
    private javax.swing.JRadioButton bitmapFont;
    private javax.swing.JComboBox bitmapFontValue;
    private javax.swing.JButton borderHelp;
    private javax.swing.JLabel borderLabel;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton changeColorButtonBG;
    private javax.swing.JButton changeColorButtonFG;
    private javax.swing.JButton changeGradientEndColorButton;
    private javax.swing.JButton changeGradientStartColorButton;
    private javax.swing.JButton colorHelp;
    private javax.swing.JTextField colorValueBG;
    private javax.swing.JTextField colorValueFG;
    private javax.swing.JComboBox componentName;
    private javax.swing.JButton customizeBorder;
    private javax.swing.JCheckBox defineAttribute;
    private javax.swing.JCheckBox deriveAlignment;
    private javax.swing.JCheckBox deriveBackground;
    private javax.swing.JCheckBox deriveBackgroundColor;
    private javax.swing.JCheckBox deriveBorder;
    private javax.swing.JCheckBox deriveFont;
    private javax.swing.JCheckBox deriveForegroundColor;
    private javax.swing.JButton deriveHelp;
    private javax.swing.JButton deriveHelp1;
    private javax.swing.JButton deriveHelp2;
    private javax.swing.JButton deriveHelp3;
    private javax.swing.JButton deriveHelp4;
    private javax.swing.JButton deriveHelp5;
    private javax.swing.JButton deriveHelp6;
    private javax.swing.JButton deriveHelp7;
    private javax.swing.JCheckBox deriveMargin;
    private javax.swing.JCheckBox derivePadding;
    private javax.swing.JCheckBox deriveTextDecoration;
    private javax.swing.JCheckBox deriveTransparency;
    private javax.swing.JComboBox fontFace;
    private javax.swing.JButton fontHelp;
    private javax.swing.JComboBox fontSize;
    private javax.swing.JComboBox fontStyle;
    private javax.swing.JTextField gradientEndColor;
    private javax.swing.JSpinner gradientSize;
    private javax.swing.JTextField gradientStartColor;
    private javax.swing.JSpinner gradientX;
    private javax.swing.JSpinner gradientY;
    private javax.swing.JTextPane help;
    private javax.swing.JButton imageBorderWizard;
    private javax.swing.JComboBox imagesCombo;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JSpinner marginBottom;
    private javax.swing.JComboBox marginBottomUnit;
    private javax.swing.JButton marginHelp;
    private javax.swing.JSpinner marginLeft;
    private javax.swing.JComboBox marginLeftUnit;
    private javax.swing.JSpinner marginRight;
    private javax.swing.JComboBox marginRightUnit;
    private javax.swing.JSpinner marginTop;
    private javax.swing.JComboBox marginTopUnit;
    private javax.swing.JSpinner paddingBottom;
    private javax.swing.JComboBox paddingBottomUnit;
    private javax.swing.JButton paddingHelp;
    private javax.swing.JSpinner paddingLeft;
    private javax.swing.JComboBox paddingLeftUnit;
    private javax.swing.JSpinner paddingRight;
    private javax.swing.JComboBox paddingRightUnit;
    private javax.swing.JSpinner paddingTop;
    private javax.swing.JComboBox paddingTopUnit;
    private javax.swing.JPanel previewPane;
    private javax.swing.JButton styleHelp;
    private javax.swing.JLabel styleType;
    private javax.swing.JRadioButton systemFont;
    private javax.swing.JComboBox textDecorationCombo;
    private javax.swing.JSpinner transparencyValue;
    private javax.swing.JComboBox trueTypeFont;
    private javax.swing.JComboBox trueTypeFontSizeOption;
    private javax.swing.JSpinner trueTypeFontSizeValue;
    private javax.swing.JButton videoTutorial;
    // End of variables declaration//GEN-END:variables
}
