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

import com.codename1.designer.PickMIDlet;
import com.codename1.designer.ResourceEditorView;
import com.codename1.ui.Display;
import com.codename1.ui.EditorFont;
import com.codename1.ui.Form;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.impl.javase.JavaSEPortWithSVGSupport;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.Accessor;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.util.EditableResources;
import com.codename1.ui.util.UIBuilderOverride;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;
import java.util.Vector;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Allows changing the theme data
 *
 * @author  Shai Almog
 */
public class ThemeEditor extends BaseForm {
    boolean dirty;
    private static TableTranferable copyInstance;
    private EditableResources resources;
    //private ThemeModel model;
    private static MouseHandler oldHandler;
    private ResourceEditorView view;
    private String flashingProperty;
    private Object originalFlashingPropertyValue;
    private javax.swing.Timer flashingTimer;
    private Hashtable themeHash;
    
    private String themeName;
    private JPanel previewOptionsPanel = new JPanel();
    private JButton previewOptions = new JButton("Advanced Options");
    private Box previewOptionsPosition = new Box(BoxLayout.X_AXIS);
    private boolean initialized;
    private static boolean themeWasLoaded;

    static boolean wasThemeLoaded() {
        return themeWasLoaded;
    }
    
    public static void resetThemeLoaded() {
        themeWasLoaded = false;
    }
    
    /** Creates new form ThemeEditor */
    public ThemeEditor(EditableResources resources, String themeName, Hashtable themeHash, ResourceEditorView view) {
        if(resources.isOverrideMode() && !resources.isOverridenResource(themeName)) {
            setOverrideMode(true, view.getComponent());
        }
        this.resources = resources;
        this.view = view;
        this.themeHash = themeHash;
        this.themeName = themeName;
        UIBuilderOverride.setIgnorBaseForm(false);
        initComponents();
        try {
            help.setPage(getClass().getResource("/help/themeEditorHelp.html"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        previewParentPanel.remove(configPane);
        previewOptionsPanel.setLayout(new BoxLayout(previewOptionsPanel, BoxLayout.X_AXIS));
        previewOptionsPanel.add(new JLabel("Form:"));
        JComboBox formPicker = new JComboBox();
        formPicker.setLightWeightPopupEnabled(false);
        previewOptionsPanel.add(formPicker);        
        JButton previewInSimulator = new JButton("Simulator");
        previewInSimulator.setToolTipText("Open the current form in the device simulator");
        previewInSimulator.setEnabled(view.getProjectGeneratorSettings() != null);
        previewInSimulator.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                simulatorActionPerformed(ae);
            }
        });
        previewOptionsPanel.add(previewInSimulator);
        previewOptionsPanel.add(previewOptions);
        previewOptionsPosition.add(previewOptionsPanel);
        previewParentPanel.add(java.awt.BorderLayout.NORTH, previewOptionsPosition);
        previewOptions.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                previewParentPanel.remove(previewOptionsPosition);
                previewParentPanel.add(java.awt.BorderLayout.NORTH, configPane);
                previewParentPanel.revalidate();
                previewParentPanel.repaint();
            }
        });
        Vector localeVector = new Vector();
        String localeValue = Preferences.userNodeForPackage(getClass()).get("localeValue", null);
        String localeLanguageValue = Preferences.userNodeForPackage(getClass()).get("localeLanguageValue", null);
        localeVector.addElement(null);
        int selectedLocaleIndex = -1;
        for(String currentLocale : resources.getL10NResourceNames()) {
            Enumeration e = resources.listL10NLocales(currentLocale);
            while(e.hasMoreElements()) {
                String lang = (String)e.nextElement();
                if(currentLocale.equals(localeValue) && lang.equals(localeLanguageValue)) {
                    selectedLocaleIndex = localeVector.size();
                    Accessor.setResourceBundle(resources.getL10N(localeValue, localeLanguageValue));
                }
                localeVector.addElement(new String[] {currentLocale, lang});
            }
        }
        localePicker.setModel(new DefaultComboBoxModel(localeVector));
        if(selectedLocaleIndex > -1) {
            localePicker.setSelectedIndex(selectedLocaleIndex);
        }
        localePicker.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if(value == null) {
                    value = "[None]";
                } else {
                    String[] s = (String[])value;
                    value = s[0] + " - " + s[1];
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });

        int deviceTypeValue = Preferences.userNodeForPackage(getClass()).getInt("deviceTypeValue", 3);
        updateDeviceType(deviceTypeValue);
        deviceType.setSelectedIndex(deviceTypeValue);

        Vector uiPreviewConentVector = new Vector();
        String[] sortedUIElements = new String[resources.getUIResourceNames().length];
        System.arraycopy(resources.getUIResourceNames(), 0, sortedUIElements, 0, sortedUIElements.length);
        Arrays.sort(sortedUIElements, String.CASE_INSENSITIVE_ORDER);
        for(String currentUI : sortedUIElements) {
            uiPreviewConentVector.addElement(currentUI);
        }
        uiPreviewConentVector.addElement("Default Demo");
        uiPreviewContent.setModel(new DefaultComboBoxModel(uiPreviewConentVector));
        String selectionInUiPreviewContent = Preferences.userNodeForPackage(getClass()).get("uiPreviewContent", null);
        if(selectionInUiPreviewContent != null) {
            uiPreviewContent.setSelectedItem(selectionInUiPreviewContent);
        }
        formPicker.setModel(uiPreviewContent.getModel());
        formPicker.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                initMIDlet();
            }
        });
        
        int widthResoltutionValue = Preferences.userNodeForPackage(getClass()).getInt("selectedSizeWidth", 320);
        int heightResoltutionValue = Preferences.userNodeForPackage(getClass()).getInt("selectedSizeHeight", 480);
        int fontSizeValue = Preferences.userNodeForPackage(getClass()).getInt("selectedSizeFont", 13);
        int fontSizeSmallValue = Preferences.userNodeForPackage(getClass()).getInt("selectedSizeFontSmall", 11);
        int fontSizeLargeValue = Preferences.userNodeForPackage(getClass()).getInt("selectedSizeFontLarge", 16);
        widthResoltution.setModel(new SpinnerNumberModel(widthResoltutionValue, 128, 1024, 1));
        heightResolution.setModel(new SpinnerNumberModel(heightResoltutionValue, 128, 1024, 1));
        systemFontSize.setModel(new SpinnerNumberModel(fontSizeValue, 7, 40, 1));
        smallFontSize.setModel(new SpinnerNumberModel(fontSizeSmallValue, 7, 40, 1));
        largeFontSize.setModel(new SpinnerNumberModel(fontSizeLargeValue, 7, 40, 1));
        float scaleValue = Preferences.userNodeForPackage(getClass()).getFloat("scaleValue", 1.0f);

        initializeTable(theme, null);
        initializeTable(selectedStyles, "sel#");
        initializeTable(pressedStyles, "press#");
        initializeTable(disabledStyles, "dis#");
        initializeTable(constantsTable, "@");
        theme.setRowHeight(34);
        pressedStyles.setRowHeight(34);
        selectedStyles.setRowHeight(34);
        disabledStyles.setRowHeight(34);
        constantsTable.setRowHeight(34);
        MouseHandler m = new MouseHandler();
        previewPanel.addMouseMotionListener(m);
        previewPanel.addMouseListener(m);
        oldHandler = m;


        JavaSEPortWithSVGSupport.setFontSize(get(systemFontSize), get(smallFontSize), get(largeFontSize));
        initMIDlet();

        resources.refreshThemeMultiImages();
        refreshTheme(themeHash);
        previewScroll.revalidate();

        // a race condition causes CodenameOne to sometimes paint a blank screen
        new Thread() {
            public void run() {
                try {
                    sleep(3000);
                    dirty = true;
                    while(com.codename1.ui.Display.getInstance().getCurrent() == null) {
                        sleep(200);
                    }
                    // repaint the UI every 1.5 seconds to allow delayed repaints to appear. E.g.
                    // changing the text of the HTML components body happens asynchroniously
                    while(SwingUtilities.windowForComponent(ThemeEditor.this) != null) {
                        sleep(1500);
                        if(dirty) {
                            com.codename1.ui.Display.getInstance().getCurrent().repaint();
                            sleep(500);
                            previewPanel.repaint();
                            dirty = false;
                        }
                    }
                } catch(InterruptedException e) {}
            }
        }.start();
        initialized = true;
        themeWasLoaded = true;
    }


    private void updateDeviceType(int i) {
        Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_DEFAULT);
        switch(i) {
            case 0:
                com.codename1.ui.Display.getInstance().setTouchScreenDevice(true);
                JavaSEPortWithSVGSupport.setSoftkeyCount(2);
                JavaSEPortWithSVGSupport.setTablet(false);
                break;
            case 1:
                com.codename1.ui.Display.getInstance().setTouchScreenDevice(false);
                com.codename1.ui.Display.getInstance().setPureTouch(false);
                JavaSEPortWithSVGSupport.setSoftkeyCount(2);
                JavaSEPortWithSVGSupport.setTablet(false);
                break;
            case 2:
                com.codename1.ui.Display.getInstance().setTouchScreenDevice(false);
                com.codename1.ui.Display.getInstance().setPureTouch(false);
                JavaSEPortWithSVGSupport.setSoftkeyCount(1);
                JavaSEPortWithSVGSupport.setTablet(false);
                break;
            case 3:
                com.codename1.ui.Display.getInstance().setTouchScreenDevice(true);
                JavaSEPortWithSVGSupport.setSoftkeyCount(1);
                JavaSEPortWithSVGSupport.setTablet(false);
                break;
            case 4:
                com.codename1.ui.Display.getInstance().setTouchScreenDevice(true);
                JavaSEPortWithSVGSupport.setSoftkeyCount(0);
                JavaSEPortWithSVGSupport.setTablet(true);
                break;
            case 5:
                com.codename1.ui.Display.getInstance().setTouchScreenDevice(true);
                JavaSEPortWithSVGSupport.setSoftkeyCount(1);
                JavaSEPortWithSVGSupport.setTablet(true);
                break;
        }
    }

    private int get(JSpinner s) {
        return ((Number)s.getValue()).intValue();
    }

    private float getFloat(JSpinner s) {
        return ((Number)s.getValue()).floatValue();
    }

    private void initMIDlet() {        
        JavaSEPortWithSVGSupport.setShowEDTWarnings(false);
        JavaSEPortWithSVGSupport.setShowEDTViolationStacks(false);
        // if the last element is selected in the combo its a MIDlet otherwise
        // its a UI form
        if(uiPreviewContent.getSelectedIndex() == uiPreviewContent.getModel().getSize() - 1) {
            previewPanel.removeAll();
            if(com.codename1.ui.Display.isInitialized()) {
                com.codename1.ui.Display.deinitialize();
            }
            JavaSEPortWithSVGSupport.setDefaultInitTarget(previewPanel);
            com.codename1.ui.Display.init(previewPanel);
            previewPanel.getComponent(0).setBounds(0, 0, get(widthResoltution), get(heightResolution));
            previewPanel.getComponent(0).setPreferredSize(new java.awt.Dimension(get(widthResoltution), get(heightResolution)));
            PickMIDlet.startMIDlet(themeHash);
        } else {
            Preferences.userNodeForPackage(getClass()).put("uiPreviewContent", (String)uiPreviewContent.getSelectedItem());
            Accessor.setTheme(themeHash);
            if(com.codename1.ui.Display.isInitialized()) {
                com.codename1.ui.Display.deinitialize();
            }
            previewPanel.removeAll();
            com.codename1.ui.Display.init(previewPanel);
            previewPanel.getComponent(0).setBounds(0, 0, get(widthResoltution), get(heightResolution));
            previewPanel.getComponent(0).setPreferredSize(new java.awt.Dimension(get(widthResoltution), get(heightResolution)));
            com.codename1.ui.util.UIBuilder.registerCustomComponent("Table", com.codename1.ui.table.Table.class);
            com.codename1.ui.util.UIBuilder.registerCustomComponent("MediaPlayer", com.codename1.components.MediaPlayer.class);
            com.codename1.ui.util.UIBuilder.registerCustomComponent("ContainerList", com.codename1.ui.list.ContainerList.class);
            com.codename1.ui.util.UIBuilder.registerCustomComponent("ComponentGroup", com.codename1.ui.ComponentGroup.class);
            com.codename1.ui.util.UIBuilder.registerCustomComponent("Tree", com.codename1.ui.tree.Tree.class);
            com.codename1.ui.util.UIBuilder.registerCustomComponent("HTMLComponent", com.codename1.ui.html.HTMLComponent.class);
            com.codename1.ui.util.UIBuilder.registerCustomComponent("RSSReader", com.codename1.components.RSSReader.class);
            com.codename1.ui.util.UIBuilder.registerCustomComponent("FileTree", com.codename1.components.FileTree.class);
            com.codename1.ui.util.UIBuilder.registerCustomComponent("WebBrowser", com.codename1.components.WebBrowser.class);
            com.codename1.ui.util.UIBuilder.registerCustomComponent("NumericSpinner", com.codename1.ui.spinner.NumericSpinner.class);
            com.codename1.ui.util.UIBuilder.registerCustomComponent("DateSpinner", com.codename1.ui.spinner.DateSpinner.class);
            com.codename1.ui.util.UIBuilder.registerCustomComponent("TimeSpinner", com.codename1.ui.spinner.TimeSpinner.class);
            com.codename1.ui.util.UIBuilder.registerCustomComponent("DateTimeSpinner", com.codename1.ui.spinner.DateTimeSpinner.class);
            com.codename1.ui.util.UIBuilder.registerCustomComponent("GenericSpinner", com.codename1.ui.spinner.GenericSpinner.class);
            com.codename1.ui.util.UIBuilder.registerCustomComponent("LikeButton", com.codename1.facebook.ui.LikeButton.class);
            com.codename1.ui.util.UIBuilder.registerCustomComponent("InfiniteProgress", com.codename1.components.InfiniteProgress.class);
            com.codename1.ui.util.UIBuilder.registerCustomComponent("MultiButton", com.codename1.components.MultiButton.class);
            com.codename1.ui.util.UIBuilder.registerCustomComponent("Ads", com.codename1.components.Ads.class);
            com.codename1.ui.util.UIBuilder.registerCustomComponent("MapComponent", com.codename1.maps.MapComponent.class);
            com.codename1.ui.util.UIBuilder.registerCustomComponent("MultiList", com.codename1.ui.list.MultiList.class);
            com.codename1.ui.util.UIBuilder.registerCustomComponent("ShareButton", com.codename1.components.ShareButton.class);
            com.codename1.ui.util.UIBuilder builder = new com.codename1.ui.util.UIBuilder();
            com.codename1.ui.Container c = builder.createContainer(resources, (String)uiPreviewContent.getSelectedItem());
            if(c instanceof com.codename1.ui.Form) {
                ((com.codename1.ui.Form)c).refreshTheme();
                if(c instanceof com.codename1.ui.Dialog) {
                    ((com.codename1.ui.Dialog)c).showModeless();
                } else {
                    ((com.codename1.ui.Form)c).show();
                }
            } else {
                com.codename1.ui.Form f = new Form();
                f.setLayout(new com.codename1.ui.layouts.BorderLayout());
                f.addComponent(com.codename1.ui.layouts.BorderLayout.CENTER, c);
                f.refreshTheme();
                f.show();
            }
        }
    }
    
    static class TableTranferable {
        Object attribute;
        String uiid;
        Hashtable values;
        public TableTranferable() {}
        public TableTranferable(String uiid, Hashtable values) {
            this.uiid = uiid;
            this.values = values;
        }
        public TableTranferable(String uiid, Object attribute) {
            this.uiid = uiid;
            this.attribute = attribute;
        }
    }

    private void initializeTable(final JTable table, String stylePrefix) {
        InputMap input = table.getInputMap(JComponent.WHEN_FOCUSED);
        bindSearch(searchField, table);
        table.setDefaultRenderer(Object.class, new ThemeRenderer());

        class Copy extends AbstractAction implements ListSelectionListener {
            Copy() {
                putValue(NAME, "Copy");
                table.getSelectionModel().addListSelectionListener(this);
                setEnabled(false);
            }
            public void actionPerformed(ActionEvent e) {
                int r = table.getSelectedRow();
                if(r > -1) {
                    r = getModelSelection(getCurrentStyleTable());
                    String key = (String)getCurrentStyleModel().getValueAt(r, 0);
                    Hashtable values = new Hashtable();
                    String prefix = getCurrentStyleModel().prefix;
                    for(Object k : themeHash.keySet()) {
                        String currentKey = (String)k;
                        String origCurrentKey = currentKey;
                        if(currentKey.startsWith("@")) {
                            continue;
                        }
                        if(currentKey.startsWith(key + ".")) {
                            if(prefix == null) {
                                if(currentKey.indexOf('#') > -1) {
                                    continue;
                                }
                            } else {
                                if(currentKey.indexOf(prefix) < 0) {
                                    continue;
                                }
                                currentKey = currentKey.replace(prefix, "");
                            }
                            values.put(currentKey, themeHash.get(origCurrentKey));
                        }
                    }
                    copyInstance = new TableTranferable(key, values);
                }
            }

            public void valueChanged(ListSelectionEvent e) {
                setEnabled(table.getSelectedRow() > -1);
            }
        }
        
        class Derive extends AbstractAction implements ListSelectionListener {
            private String destination;
            Derive(String title, String destination) {
                putValue(NAME, title);
                this.destination = destination;
                table.getSelectionModel().addListSelectionListener(this);
                valueChanged(null);
            }
            public void actionPerformed(ActionEvent e) {
                int r = table.getSelectedRow();
                if(r > -1) {
                    r = getModelSelection(getCurrentStyleTable());
                    String key = (String)getCurrentStyleModel().getValueAt(r, 0);
                    String deriveKey = key + ".";
                    if(getCurrentStyleModel().prefix != null) {
                        deriveKey += getCurrentStyleModel().prefix;
                    }
                    if(destination == null) {
                        resources.setThemeProperty(themeName, key + ".sel#derive", deriveKey);
                        resources.setThemeProperty(themeName, key + ".press#derive", deriveKey);
                        resources.setThemeProperty(themeName, key + ".dis#derive", deriveKey);
                    } else {
                        resources.setThemeProperty(themeName, key + "." + destination + "derive", deriveKey);
                    }
                    initTableModel(theme, null);
                    initTableModel(selectedStyles, "sel#");
                    initTableModel(pressedStyles, "press#");
                    initTableModel(disabledStyles, "dis#");
                    refreshTheme(themeHash);
                }
            }

            public void valueChanged(ListSelectionEvent e) {
                if(destination == null && getCurrentStyleModel().prefix != null) {
                    setEnabled(false);
                    return;
                }
                if(getCurrentStyleModel().prefix != null && getCurrentStyleModel().prefix.equals(destination)) {
                    setEnabled(false);
                    return;
                }
                setEnabled(table.getSelectedRow() > -1);
            }
        }
        
        class CopyAttribute extends AbstractAction implements ListSelectionListener {
            CopyAttribute() {
                putValue(NAME, "Copy Attribute");
                table.getSelectionModel().addListSelectionListener(this);
                setEnabled(false);
            }
            public void actionPerformed(ActionEvent e) {
                int r = table.getSelectedRow();
                if(r > -1) {
                    r = getModelSelection(getCurrentStyleTable());
                    String key = (String)getCurrentStyleModel().getValueAt(r, 0);
                    JComboBox attributeList = new JComboBox(new Object[] {
                        "fgColor", "bgColor","derive",
                        "align", "textDecoration", "border", "font", "bgImage",
                        "transparency", "padding", "margin", "bgType", "bgGradient"
                    });
                    int selection = JOptionPane.showConfirmDialog(ThemeEditor.this, attributeList, "Select Attribute", JOptionPane.OK_CANCEL_OPTION);
                    if(selection != JOptionPane.OK_OPTION) {
                        return;
                    }
                    if(getCurrentStyleModel().prefix == null) {
                        key += "." + attributeList.getSelectedItem();
                    } else {
                        key += "." + getCurrentStyleModel().prefix + attributeList.getSelectedItem();
                    }
                    Object value = themeHash.get(key);
                    if(value != null) {
                        copyInstance = new TableTranferable((String)attributeList.getSelectedItem(), value);
                    } else {
                        JOptionPane.showMessageDialog(ThemeEditor.this, "Attribute " + key + " undefined", "Undefined Attribute", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }

            public void valueChanged(ListSelectionEvent e) {
                setEnabled(table.getSelectedRow() > -1);
            }
        }
        class Cut extends Copy {
            Cut() {
                putValue(NAME, "Cut");
            }
            public void actionPerformed(ActionEvent e) {
                super.actionPerformed(e);
                int r = table.getSelectedRow();
                if(r > -1) {
                    removeThemeEntryActionPerformed(e);
                }
            }
        }
        class Paste extends AbstractAction {
            Paste() {
                putValue(NAME, "Paste");
            }
            public void actionPerformed(ActionEvent e) {
                if(copyInstance != null) {
                    if(copyInstance.attribute != null) {
                        int r = table.getSelectedRow();
                        if(r > -1) {
                            r = getModelSelection(getCurrentStyleTable());
                            String key = (String)getCurrentStyleModel().getValueAt(r, 0);
                            if(key == null) {
                                resources.setThemeProperty(themeName, copyInstance.uiid, copyInstance.attribute);
                            } else {
                                if(getCurrentStyleModel().prefix != null) {
                                    key += "." + getCurrentStyleModel().prefix + copyInstance.uiid;
                                } else {
                                    key += "." + copyInstance.uiid;
                                }
                                resources.setThemeProperty(themeName, key, copyInstance.attribute);
                            }
                            themeHash = resources.getTheme(themeName);
                            refreshTheme(themeHash);
                        }
                    } else {
                        AddThemeEntry entry = new AddThemeEntry(true, resources, view, 
                                new Hashtable(themeHash), getCurrentStyleModel().prefix,
                                themeName);
                        entry.pasteKeyValues(copyInstance.values);
                        showAddThemeEntry(entry);
                    }
                }
            }
        }
        
        final Copy copy = new Copy();
        final Cut cut = new Cut();
        final CopyAttribute copyAttr = new CopyAttribute();
        final Paste paste = new Paste();
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK), "copy");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK), "copyAttr");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK), "cut");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK), "paste");
        table.getActionMap().put("copy", copy);
        table.getActionMap().put("copyAttr", copyAttr);
        table.getActionMap().put("cut", cut);
        table.getActionMap().put("paste", paste);
        initTableModel(table, stylePrefix);
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "edit");
        final AbstractAction delete = new AbstractAction("Delete") {
            {
                putValue(AbstractAction.NAME, "Delete");
            }
            public void actionPerformed(ActionEvent e) {
                if(table.getSelectedRowCount() == 1) {
                    removeThemeEntryActionPerformed(e);
                }
            }
        };
        table.getActionMap().put("delete", delete);
        final AbstractAction edit = new AbstractAction("Edit") {
            {
                putValue(AbstractAction.NAME, "Edit");
            }
            public void actionPerformed(ActionEvent e) {
                if(table.getSelectedRowCount() == 1) {
                    editEntryActionPerformed(e);
                }
            }
        };
        table.getActionMap().put("edit", edit);
        table.addMouseListener(new MouseAdapter() {
            private JPopupMenu popupMenu;
            public void mousePressed(MouseEvent e) {
                showPopup(e);
            }

            public void mouseReleased(MouseEvent e) {
                showPopup(e);
            }

            private void showPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    if(popupMenu == null) {
                        popupMenu = new JPopupMenu();
                        popupMenu.add(copy);
                        popupMenu.add(copyAttr);
                        popupMenu.add(cut);
                        popupMenu.add(paste);
                        popupMenu.add(edit);
                        popupMenu.add(delete);
                        JMenu deriveMenu = new JMenu("Derive");
                        popupMenu.add(deriveMenu);
                        deriveMenu.add(new Derive("Derive All", null));
                        deriveMenu.add(new Derive("Derive Selected", "sel#"));
                        deriveMenu.add(new Derive("Derive Pressed", "press#"));
                        deriveMenu.add(new Derive("Derive Disabled", "dis#"));
                    }
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    private void initTableModel(final JTable table, String stylePrefix) {
        final ThemeModel model = new ThemeModel(themeHash, stylePrefix);
        table.setModel(model);
        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                int row = getModelSelection(getCurrentStyleTable());
                editEntry.setEnabled(row > -1);
                removeThemeEntry.setEnabled(row > -1);
                /*if(liveHighlighting.isSelected() && row > -1 && table != constantsTable && row < model.getRowCount()) {
                    flashSelectedProperty(model, (String)model.getValueAt(row, 0));
                }*/
            }
        });
    }

    
    /**
     * Returns a "contrasting" value for the property to flash, e.g. for a font, return a differnet font
     * or for a color return a ligher/darker color...
     */
    private Object findFlushingPropertyContrust() {
        if(flashingProperty.indexOf("Color") > -1) {
            // flash to white or black depending on whether the color is closer to white
            int val = Integer.decode("0x" + originalFlashingPropertyValue);
            if(val > 0xf0f0f0) {
                return "000000";
            } else {
                return "ffffff";
            }
        }
        if(flashingProperty.indexOf("derive") > -1) {
            return "NoPropertyUIIDExists";
        }
        if(flashingProperty.indexOf("font") > -1) {
            // if this is not a bold font then just return a system bold font
            if((((com.codename1.ui.Font)originalFlashingPropertyValue).getStyle() & com.codename1.ui.Font.STYLE_BOLD) != 0) {
                return com.codename1.ui.Font.createSystemFont(com.codename1.ui.Font.FACE_SYSTEM, com.codename1.ui.Font.STYLE_PLAIN, com.codename1.ui.Font.SIZE_LARGE);
            }
            return com.codename1.ui.Font.createSystemFont(com.codename1.ui.Font.FACE_SYSTEM, com.codename1.ui.Font.STYLE_BOLD, com.codename1.ui.Font.SIZE_LARGE);
        }
        if(flashingProperty.indexOf("bgImage") > -1) {
            com.codename1.ui.Image i  = (com.codename1.ui.Image)originalFlashingPropertyValue;
            return i.modifyAlpha((byte)128);
        }
        if(flashingProperty.indexOf("transparency") > -1) {
            int v = Integer.parseInt((String)originalFlashingPropertyValue);
            if(v < 128) {
                return "255";
            } else {
                return "100";
            }
        }
        /*if(flashingProperty.indexOf("scale") > -1) {
            return "false";
        }*/
        if(flashingProperty.indexOf("padding") > -1 || flashingProperty.indexOf("margin") > -1) {
            return "10,10,10,10";
        }
        if(flashingProperty.indexOf("border") > -1) {
            if(originalFlashingPropertyValue != null) {
                Border pressed = ((Border)originalFlashingPropertyValue).createPressedVersion();
                if(pressed != null) {
                    return pressed;
                }
            }
            return Border.createBevelRaised();
        }
        if(flashingProperty.indexOf("bgType") > -1) {
            return originalFlashingPropertyValue;
        }
        if(flashingProperty.indexOf("bgGradient") > -1) {
            Object[] gradient = new Object[4];
            System.arraycopy(originalFlashingPropertyValue, 0, gradient, 0, 4);
            gradient[0] = ((Object[])originalFlashingPropertyValue)[1];
            gradient[1] = ((Object[])originalFlashingPropertyValue)[0];
            return gradient;
        }
        if(flashingProperty.indexOf("align") > -1 || flashingProperty.indexOf("textDecoration") > -1) {
            return originalFlashingPropertyValue;
        }
        throw new IllegalArgumentException("Unsupported property type: " + flashingProperty);
    }
    
    
    /**
     * Causes the selected property in the table to flash on 
     * @param key
     */
    private void flashSelectedProperty(ThemeModel model, String key) {
        /*refreshRam();
        clearFlashingTimer();
        flashingProperty = key;
        originalFlashingPropertyValue = model.getPropertyValue(key);
        final Object contrastingColor = findFlushingPropertyContrust();
        
        flashingTimer = new javax.swing.Timer(250, new ActionListener() {
            private long time = System.currentTimeMillis();
            private boolean flashValue = true;
            private Hashtable flashHash;
            public void actionPerformed(ActionEvent e) {
                if(System.currentTimeMillis() - time > 3000) {
                    clearFlashingTimer();
                    return;
                }
                Hashtable h = themeHash;
                if(flashValue) {
                    if(flashHash == null) {
                        flashHash = new Hashtable(themeHash);
                        flashHash.put(flashingProperty, contrastingColor);
                    }
                    h = flashHash;
                    flashValue = false;
                } else {
                    flashValue = true;
                }
                refreshTheme(h);
            }
        });
        flashingTimer.setRepeats(true);
        flashingTimer.start();*/
    }
    
    private void clearFlashingTimer() {
        if(flashingTimer != null) {
            if(flashingTimer.isRunning()) {
                flashingTimer.stop();
            }
            flashingTimer = null;
            flashingProperty = null;
            originalFlashingPropertyValue = null;
            refreshTheme(themeHash);
        }
    }
    
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        previewParentPanel = new javax.swing.JPanel();
        previewScroll = new javax.swing.JScrollPane();
        previewPanel = new javax.swing.JPanel();
        configPane = new javax.swing.JPanel();
        uiPreviewContent = new javax.swing.JComboBox();
        smallFontSize = new javax.swing.JSpinner();
        largeFontSize = new javax.swing.JSpinner();
        widthResoltution = new javax.swing.JSpinner();
        localePicker = new javax.swing.JComboBox();
        deviceType = new javax.swing.JComboBox();
        heightResolution = new javax.swing.JSpinner();
        systemFontSize = new javax.swing.JSpinner();
        hideConfig = new javax.swing.JButton();
        simulator = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        addThemeEntry = new javax.swing.JButton();
        editEntry = new javax.swing.JButton();
        removeThemeEntry = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        searchField = new javax.swing.JTextField();
        stylesTabbedPane = new javax.swing.JTabbedPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        theme = createTable();
        jScrollPane2 = new javax.swing.JScrollPane();
        selectedStyles = createTable();
        jScrollPane3 = new javax.swing.JScrollPane();
        pressedStyles = createTable();
        jScrollPane5 = new javax.swing.JScrollPane();
        disabledStyles = createTable();
        jScrollPane4 = new javax.swing.JScrollPane();
        constantsTable = createTable();
        jScrollPane6 = new javax.swing.JScrollPane();
        help = new javax.swing.JEditorPane();
        manageStyles = new javax.swing.JButton();
        borderWizard = new javax.swing.JButton();

        FormListener formListener = new FormListener();

        setName("Form"); // NOI18N
        setOpaque(false);

        jSplitPane1.setName("jSplitPane1"); // NOI18N
        jSplitPane1.setOneTouchExpandable(true);
        jSplitPane1.setOpaque(false);

        previewParentPanel.setName("previewParentPanel"); // NOI18N
        previewParentPanel.setOpaque(false);
        previewParentPanel.setLayout(new java.awt.BorderLayout());

        previewScroll.setBorder(null);
        previewScroll.setName("previewScroll"); // NOI18N
        previewScroll.setOpaque(false);

        previewPanel.setName("previewPanel"); // NOI18N
        previewPanel.setOpaque(false);
        previewPanel.addMouseListener(formListener);
        previewPanel.addKeyListener(formListener);
        previewPanel.setLayout(null);
        previewScroll.setViewportView(previewPanel);

        previewParentPanel.add(previewScroll, java.awt.BorderLayout.CENTER);

        configPane.setName("configPane"); // NOI18N
        configPane.setOpaque(false);

        uiPreviewContent.setToolTipText("UI Preview Content");
        uiPreviewContent.setLightWeightPopupEnabled(false);
        uiPreviewContent.setName("uiPreviewContent"); // NOI18N
        uiPreviewContent.addActionListener(formListener);

        smallFontSize.setToolTipText("Small Font Size");
        smallFontSize.setName("smallFontSize"); // NOI18N
        smallFontSize.addChangeListener(formListener);

        largeFontSize.setToolTipText("Large Font Size");
        largeFontSize.setName("largeFontSize"); // NOI18N
        largeFontSize.addChangeListener(formListener);

        widthResoltution.setToolTipText("Width");
        widthResoltution.setName("widthResoltution"); // NOI18N
        widthResoltution.addChangeListener(formListener);

        localePicker.setToolTipText("Force Locale");
        localePicker.setName("localePicker"); // NOI18N
        localePicker.addActionListener(formListener);

        deviceType.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Touch Device", "None Touch", "Menu Button", "Menu Button & Touch", "Tablet Without Menu Button", "Tablet With Menu Button", " " }));
        deviceType.setToolTipText("Device Type");
        deviceType.setName("deviceType"); // NOI18N
        deviceType.addActionListener(formListener);

        heightResolution.setToolTipText("Height");
        heightResolution.setName("heightResolution"); // NOI18N
        heightResolution.addChangeListener(formListener);

        systemFontSize.setToolTipText("Medium Font Size");
        systemFontSize.setName("systemFontSize"); // NOI18N
        systemFontSize.addChangeListener(formListener);

        hideConfig.setText("Hide");
        hideConfig.setName("hideConfig"); // NOI18N
        hideConfig.addActionListener(formListener);

        simulator.setText("Preview In Simulator");
        simulator.setName("simulator"); // NOI18N
        simulator.addActionListener(formListener);

        org.jdesktop.layout.GroupLayout configPaneLayout = new org.jdesktop.layout.GroupLayout(configPane);
        configPane.setLayout(configPaneLayout);
        configPaneLayout.setHorizontalGroup(
            configPaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(configPaneLayout.createSequentialGroup()
                .addContainerGap()
                .add(configPaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(configPaneLayout.createSequentialGroup()
                        .add(deviceType, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 125, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(uiPreviewContent, 0, 244, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(localePicker, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(configPaneLayout.createSequentialGroup()
                        .add(widthResoltution, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(heightResolution, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(smallFontSize, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(systemFontSize, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(largeFontSize, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(configPaneLayout.createSequentialGroup()
                        .add(simulator)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(hideConfig)))
                .addContainerGap())
        );
        configPaneLayout.setVerticalGroup(
            configPaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(configPaneLayout.createSequentialGroup()
                .add(configPaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(deviceType, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(uiPreviewContent, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(localePicker, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(configPaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(widthResoltution, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(heightResolution, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(smallFontSize, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(largeFontSize, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(systemFontSize, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(configPaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(hideConfig)
                    .add(simulator))
                .addContainerGap())
        );

        previewParentPanel.add(configPane, java.awt.BorderLayout.NORTH);

        jSplitPane1.setRightComponent(previewParentPanel);

        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setOpaque(false);

        addThemeEntry.setMnemonic('A');
        addThemeEntry.setText("Add");
        addThemeEntry.setName("addThemeEntry"); // NOI18N
        addThemeEntry.addActionListener(formListener);

        editEntry.setMnemonic('d');
        editEntry.setText("Edit");
        editEntry.setEnabled(false);
        editEntry.setName("editEntry"); // NOI18N
        editEntry.addActionListener(formListener);

        removeThemeEntry.setMnemonic('R');
        removeThemeEntry.setText("Remove");
        removeThemeEntry.setEnabled(false);
        removeThemeEntry.setName("removeThemeEntry"); // NOI18N
        removeThemeEntry.addActionListener(formListener);

        jLabel2.setLabelFor(searchField);
        jLabel2.setText("Filter");
        jLabel2.setName("jLabel2"); // NOI18N

        searchField.setName("searchField"); // NOI18N

        stylesTabbedPane.setName("stylesTabbedPane"); // NOI18N

        jScrollPane1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jScrollPane1.setName("jScrollPane1"); // NOI18N
        jScrollPane1.setOpaque(false);

        theme.setName("theme"); // NOI18N
        theme.addMouseListener(formListener);
        jScrollPane1.setViewportView(theme);

        stylesTabbedPane.addTab("Unselected", jScrollPane1);
        jScrollPane1.getAccessibleContext().setAccessibleName("Unselected");
        jScrollPane1.getAccessibleContext().setAccessibleDescription("Unselected");

        jScrollPane2.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jScrollPane2.setName("jScrollPane2"); // NOI18N
        jScrollPane2.setOpaque(false);

        selectedStyles.setName("selectedStyles"); // NOI18N
        selectedStyles.addMouseListener(formListener);
        jScrollPane2.setViewportView(selectedStyles);

        stylesTabbedPane.addTab("Selected", jScrollPane2);

        jScrollPane3.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jScrollPane3.setName("jScrollPane3"); // NOI18N
        jScrollPane3.setOpaque(false);

        pressedStyles.setName("pressedStyles"); // NOI18N
        pressedStyles.addMouseListener(formListener);
        jScrollPane3.setViewportView(pressedStyles);

        stylesTabbedPane.addTab("Pressed", jScrollPane3);

        jScrollPane5.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jScrollPane5.setName("jScrollPane5"); // NOI18N
        jScrollPane5.setOpaque(false);

        disabledStyles.setName("disabledStyles"); // NOI18N
        disabledStyles.addMouseListener(formListener);
        jScrollPane5.setViewportView(disabledStyles);

        stylesTabbedPane.addTab("Disabled", jScrollPane5);

        jScrollPane4.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jScrollPane4.setName("jScrollPane4"); // NOI18N
        jScrollPane4.setOpaque(false);

        constantsTable.setName("constantsTable"); // NOI18N
        constantsTable.addMouseListener(formListener);
        jScrollPane4.setViewportView(constantsTable);

        stylesTabbedPane.addTab("Constants", jScrollPane4);

        jScrollPane6.setName("jScrollPane6"); // NOI18N

        help.setName("help"); // NOI18N
        jScrollPane6.setViewportView(help);

        stylesTabbedPane.addTab("Help", jScrollPane6);

        manageStyles.setMnemonic('S');
        manageStyles.setText("Styles");
        manageStyles.setEnabled(false);
        manageStyles.setName("manageStyles"); // NOI18N
        manageStyles.addActionListener(formListener);

        borderWizard.setText("Border Wizard");
        borderWizard.setName("borderWizard"); // NOI18N
        borderWizard.addActionListener(formListener);

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(jLabel2)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(searchField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 391, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(borderWizard))
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(addThemeEntry)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(editEntry)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(removeThemeEntry)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(manageStyles))
                    .add(stylesTabbedPane, 0, 0, Short.MAX_VALUE))
                .addContainerGap())
        );

        jPanel1Layout.linkSize(new java.awt.Component[] {addThemeEntry, editEntry, manageStyles, removeThemeEntry}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel2)
                    .add(searchField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(borderWizard))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(stylesTabbedPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 327, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(addThemeEntry)
                    .add(editEntry)
                    .add(removeThemeEntry)
                    .add(manageStyles))
                .addContainerGap())
        );

        addThemeEntry.getAccessibleContext().setAccessibleDescription("Add");
        editEntry.getAccessibleContext().setAccessibleDescription("Edit");
        removeThemeEntry.getAccessibleContext().setAccessibleDescription("Remove");
        searchField.getAccessibleContext().setAccessibleName("Filter");
        searchField.getAccessibleContext().setAccessibleDescription("Filter");
        stylesTabbedPane.getAccessibleContext().setAccessibleName("Selectors");
        stylesTabbedPane.getAccessibleContext().setAccessibleDescription("Selectors");
        manageStyles.getAccessibleContext().setAccessibleDescription("Styles");

        jSplitPane1.setLeftComponent(jPanel1);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jSplitPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 1084, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jSplitPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 431, Short.MAX_VALUE)
        );
    }

    // Code for dispatching events from components to event handlers.

    private class FormListener implements java.awt.event.ActionListener, java.awt.event.KeyListener, java.awt.event.MouseListener, javax.swing.event.ChangeListener {
        FormListener() {}
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            if (evt.getSource() == uiPreviewContent) {
                ThemeEditor.this.uiPreviewContentActionPerformed(evt);
            }
            else if (evt.getSource() == localePicker) {
                ThemeEditor.this.localePickerActionPerformed(evt);
            }
            else if (evt.getSource() == deviceType) {
                ThemeEditor.this.deviceTypeActionPerformed(evt);
            }
            else if (evt.getSource() == hideConfig) {
                ThemeEditor.this.hideConfigActionPerformed(evt);
            }
            else if (evt.getSource() == simulator) {
                ThemeEditor.this.simulatorActionPerformed(evt);
            }
            else if (evt.getSource() == addThemeEntry) {
                ThemeEditor.this.addThemeEntryActionPerformed(evt);
            }
            else if (evt.getSource() == editEntry) {
                ThemeEditor.this.editEntryActionPerformed(evt);
            }
            else if (evt.getSource() == removeThemeEntry) {
                ThemeEditor.this.removeThemeEntryActionPerformed(evt);
            }
            else if (evt.getSource() == manageStyles) {
                ThemeEditor.this.manageStylesActionPerformed(evt);
            }
            else if (evt.getSource() == borderWizard) {
                ThemeEditor.this.borderWizardActionPerformed(evt);
            }
        }

        public void keyPressed(java.awt.event.KeyEvent evt) {
            if (evt.getSource() == previewPanel) {
                ThemeEditor.this.previewPanelKeyPressed(evt);
            }
        }

        public void keyReleased(java.awt.event.KeyEvent evt) {
            if (evt.getSource() == previewPanel) {
                ThemeEditor.this.previewPanelKeyReleased(evt);
            }
        }

        public void keyTyped(java.awt.event.KeyEvent evt) {
        }

        public void mouseClicked(java.awt.event.MouseEvent evt) {
            if (evt.getSource() == theme) {
                ThemeEditor.this.themeMouseClicked(evt);
            }
            else if (evt.getSource() == selectedStyles) {
                ThemeEditor.this.selectedStylesMouseClicked(evt);
            }
            else if (evt.getSource() == pressedStyles) {
                ThemeEditor.this.pressedStylesMouseClicked(evt);
            }
            else if (evt.getSource() == disabledStyles) {
                ThemeEditor.this.disabledStylesMouseClicked(evt);
            }
            else if (evt.getSource() == constantsTable) {
                ThemeEditor.this.constantsTableMouseClicked(evt);
            }
        }

        public void mouseEntered(java.awt.event.MouseEvent evt) {
        }

        public void mouseExited(java.awt.event.MouseEvent evt) {
        }

        public void mousePressed(java.awt.event.MouseEvent evt) {
        }

        public void mouseReleased(java.awt.event.MouseEvent evt) {
            if (evt.getSource() == previewPanel) {
                ThemeEditor.this.previewPanelMouseReleased(evt);
            }
        }

        public void stateChanged(javax.swing.event.ChangeEvent evt) {
            if (evt.getSource() == smallFontSize) {
                ThemeEditor.this.smallFontSizeStateChanged(evt);
            }
            else if (evt.getSource() == largeFontSize) {
                ThemeEditor.this.largeFontSizeStateChanged(evt);
            }
            else if (evt.getSource() == widthResoltution) {
                ThemeEditor.this.widthResoltutionStateChanged(evt);
            }
            else if (evt.getSource() == heightResolution) {
                ThemeEditor.this.heightResolutionStateChanged(evt);
            }
            else if (evt.getSource() == systemFontSize) {
                ThemeEditor.this.systemFontSizeStateChanged(evt);
            }
        }
    }// </editor-fold>//GEN-END:initComponents

    private void themeMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_themeMouseClicked
        // a double click should map to the edit action
        if(evt.getClickCount() == 2 && editEntry.isEnabled() && SwingUtilities.isLeftMouseButton(evt)) {
            editEntryActionPerformed(null);
        }
    }//GEN-LAST:event_themeMouseClicked

    /**
     * Returns the currently editable table based on the tabbed pane selection
     */
    private EditorTable getCurrentStyleTable() {
        JScrollPane scroll = (JScrollPane)stylesTabbedPane.getSelectedComponent();
        return (EditorTable)scroll.getViewport().getView();
    }

    /**
     * Returns the currently editable table model on the tabbed pane selection
     */
    private ThemeModel getCurrentStyleModel() {
        return (ThemeModel)getCurrentStyleTable().getModel();//getInternalModel();
    }

private void addThemeEntryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addThemeEntryActionPerformed
        clearFlashingTimer();
        AddThemeEntry entry = new AddThemeEntry(true, resources, view, new Hashtable(themeHash),
                getCurrentStyleModel().prefix, themeName);
        showAddThemeEntry(entry);
}//GEN-LAST:event_addThemeEntryActionPerformed

private void showAddThemeEntry(AddThemeEntry entry) {
        if(getCurrentStyleTable() == constantsTable) {
            ConstantEditor prompt = new ConstantEditor(null, null, resources);
            if(JOptionPane.showConfirmDialog(this, prompt, "Add", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
                   == JOptionPane.OK_OPTION) {
                if(prompt.isValidState()) {
                    getCurrentStyleModel().addKeyValue(prompt.getConstant(), prompt.getValue());
                    resources.setModified();
                }
            }
            return;
        }
        if(JOptionPane.showConfirmDialog(this, entry, "Add", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) ==
            JOptionPane.OK_OPTION) {
            String uiid = entry.getUIID();
            Hashtable tmp = new Hashtable(themeHash);
            if(uiid == null || uiid.length() == 0) {
                // default style what should I do here?
                entry.updateThemeHashtable(tmp);
            } else {
                for(Object k : getCurrentStyleModel().keys) {
                    if(uiid.equals(k)) {
                        int res = JOptionPane.showConfirmDialog(this, "The property " + uiid +
                                " is already defined.\nDo you want to overwrite it?",
                                "Selector Already Defined", JOptionPane.OK_CANCEL_OPTION,
                                JOptionPane.QUESTION_MESSAGE);
                        if(res != JOptionPane.OK_OPTION) {
                            return;
                        }
                    }
                }
                entry.updateThemeHashtable(tmp);
            }
            resources.setTheme(themeName, tmp);
            themeHash = resources.getTheme(themeName);
            refreshTheme(themeHash);
            initTableModel(theme, null);
            initTableModel(selectedStyles, "sel#");
            initTableModel(pressedStyles, "press#");
            initTableModel(disabledStyles, "dis#");
            /*if(themeHash.containsKey(entry.getKey())) {
                int result = JOptionPane.showConfirmDialog(this, entry.getKey() + " is already defined would you like to modify it?", "Selector Exists", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                if(result == JOptionPane.YES_OPTION) {
                    getCurrentStyleModel().setKeyValue(entry.getKey(), entry.getValue());
                    return;
                }
                
                // cancel or closing the dialog are the same...
                if(result != JOptionPane.NO_OPTION) {
                    return;
                }
                
                // this is the NO option which means going back to editing
                showAddThemeEntry(entry);
                return;
            }
            if(entry.getKey().indexOf("bgImage") > -1) {
                if(entry.isBrokenImage()) {
                    JOptionPane.showMessageDialog(this, "You must select an Image", "Select Image", JOptionPane.ERROR_MESSAGE);
                    showAddThemeEntry(entry);
                    return;
                }
                if(entry.getKey().equals("Form.bgImage")) {
                    com.codename1.ui.Image i = (com.codename1.ui.Image)entry.getValue();
                    if(!i.isOpaque()) {
                        JOptionPane.showMessageDialog(this, "The selected image is translucent, we recommend using opaque images only for forms", "Translucent Image Selected", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
            getCurrentStyleModel().addKeyValue(entry.getKey(), entry.getValue());*/
        } else {
            refreshTheme(themeHash);
        }
}

private void editEntryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editEntryActionPerformed
        clearFlashingTimer();
        if(getCurrentStyleTable() == constantsTable) {
            int row = getModelSelection(getCurrentStyleTable());
            String key = (String)getCurrentStyleModel().getValueAt(row, 0);
            ConstantEditor prompt = new ConstantEditor(key, getCurrentStyleModel().getValueAt(row, 1), resources);
            if(JOptionPane.showConfirmDialog(this, prompt, "Edit", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
                   == JOptionPane.OK_OPTION) {
                if(prompt.isValidState()) {
                    if(prompt.getConstant().equals(key)) {
                        getCurrentStyleModel().setKeyValue(prompt.getConstant(), prompt.getValue());
                    } else {
                        getCurrentStyleModel().remove(row);
                        getCurrentStyleModel().addKeyValue(prompt.getConstant(), prompt.getValue());
                    }
                    resources.setModified();
                }
            }
            return;
        }

        AddThemeEntry entry = new AddThemeEntry(false, resources, view, 
                new Hashtable(themeHash), getCurrentStyleModel().prefix,
                themeName);
        int row = getModelSelection(getCurrentStyleTable());
        String key = (String)getCurrentStyleModel().getValueAt(row, 0);
        if(getCurrentStyleTable() == pressedStyles) {
            entry.setKeyValues(key, "press#");
        } else {
            if(getCurrentStyleTable() == disabledStyles) {
                entry.setKeyValues(key, "dis#");
            } else {
                if(getCurrentStyleTable() == selectedStyles) {
                    entry.setKeyValues(key, "sel#");
                } else {
                    entry.setKeyValues(key, "");
                }
            }
        }
        if(JOptionPane.showConfirmDialog(this, entry, "Edit", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) ==
            JOptionPane.OK_OPTION) {
            Hashtable tmp = new Hashtable(themeHash);
            entry.updateThemeHashtable(tmp);
            resources.setTheme(themeName, tmp);
            themeHash = resources.getTheme(themeName);
            refreshTheme(themeHash);
            initTableModel(theme, null);
            initTableModel(selectedStyles, "sel#");
            initTableModel(pressedStyles, "press#");
            initTableModel(disabledStyles, "dis#");
            /*String newKey = entry.getKey();
            
            // if the user changed the key in the dialog we need to remove and add a new element which
            // is the real operation performed by the user
            if(newKey.equals(key)) {
                getCurrentStyleModel().setKeyValue(key, entry.getValue());
            } else {
                getCurrentStyleModel().remove(row);
                getCurrentStyleModel().addKeyValue(newKey, entry.getValue());
            }
            resources.setModified();
            if(newKey.equals("Form.bgImage")) {
                com.codename1.ui.Image i = (com.codename1.ui.Image)entry.getValue();
                if(!i.isOpaque()) {
                    JOptionPane.showMessageDialog(this, "The selected image is translucent, we recommend using opaque images only for forms", "Translucent Image Selected", JOptionPane.ERROR_MESSAGE);
                }
            }*/
        } else {
            refreshTheme(themeHash);
        }
}//GEN-LAST:event_editEntryActionPerformed

private void removeThemeEntryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeThemeEntryActionPerformed
        if(JOptionPane.showConfirmDialog(this, "Are You Sure?", "Remove", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE) ==
            JOptionPane.YES_OPTION) {
            getCurrentStyleModel().remove(getModelSelection(getCurrentStyleTable()));
            searchField.setText("");
        }
}//GEN-LAST:event_removeThemeEntryActionPerformed

private void manageStylesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_manageStylesActionPerformed
    
}//GEN-LAST:event_manageStylesActionPerformed

private void selectedStylesMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_selectedStylesMouseClicked
        // a double click should map to the edit action
        if(evt.getClickCount() == 2 && editEntry.isEnabled()) {
            editEntryActionPerformed(null);
        }
}//GEN-LAST:event_selectedStylesMouseClicked

private void pressedStylesMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_pressedStylesMouseClicked
        // a double click should map to the edit action
        if(evt.getClickCount() == 2 && editEntry.isEnabled()) {
            editEntryActionPerformed(null);
        }
}//GEN-LAST:event_pressedStylesMouseClicked

private void uiPreviewContentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_uiPreviewContentActionPerformed
    initMIDlet();
}//GEN-LAST:event_uiPreviewContentActionPerformed

private void constantsTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_constantsTableMouseClicked
        // a double click should map to the edit action
        if(evt.getClickCount() == 2 && editEntry.isEnabled()) {
            editEntryActionPerformed(null);
        }
}//GEN-LAST:event_constantsTableMouseClicked

private void disabledStylesMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_disabledStylesMouseClicked
        // a double click should map to the edit action
        if(evt.getClickCount() == 2 && editEntry.isEnabled()) {
            editEntryActionPerformed(null);
        }
}//GEN-LAST:event_disabledStylesMouseClicked

    private int getCode(java.awt.event.KeyEvent evt) {
        switch(evt.getKeyCode()) {
            case KeyEvent.VK_UP:
                return com.codename1.ui.Display.getInstance().getKeyCode(com.codename1.ui.Display.GAME_UP);
            case KeyEvent.VK_DOWN:
                return com.codename1.ui.Display.getInstance().getKeyCode(com.codename1.ui.Display.GAME_DOWN);
            case KeyEvent.VK_LEFT:
                return com.codename1.ui.Display.getInstance().getKeyCode(com.codename1.ui.Display.GAME_LEFT);
            case KeyEvent.VK_RIGHT:
                return com.codename1.ui.Display.getInstance().getKeyCode(com.codename1.ui.Display.GAME_RIGHT);
            case KeyEvent.VK_SPACE:
            case KeyEvent.VK_ENTER:
                return com.codename1.ui.Display.getInstance().getKeyCode(com.codename1.ui.Display.GAME_FIRE);
        }
        return evt.getKeyCode();
    }

private void previewPanelKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_previewPanelKeyPressed
    com.codename1.ui.Display.getInstance().keyPressed(getCode(evt));
}//GEN-LAST:event_previewPanelKeyPressed

private void previewPanelKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_previewPanelKeyReleased
    com.codename1.ui.Display.getInstance().keyReleased(getCode(evt));
}//GEN-LAST:event_previewPanelKeyReleased

private void previewPanelMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_previewPanelMouseReleased
    previewPanel.requestFocus();
}//GEN-LAST:event_previewPanelMouseReleased

private void widthResoltutionStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_widthResoltutionStateChanged
        previewPanel.getComponent(0).setBounds(0, 0, get(widthResoltution), get(heightResolution));
        previewPanel.getComponent(0).setPreferredSize(new java.awt.Dimension(get(widthResoltution), get(heightResolution)));
        Preferences.userNodeForPackage(getClass()).putInt("selectedSizeWidth", get(widthResoltution));
        resources.refreshThemeMultiImages();
        previewScroll.revalidate();
        dirty = true;
}//GEN-LAST:event_widthResoltutionStateChanged

private void heightResolutionStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_heightResolutionStateChanged
        previewPanel.getComponent(0).setBounds(0, 0, get(widthResoltution), get(heightResolution));
        previewPanel.getComponent(0).setPreferredSize(new java.awt.Dimension(get(widthResoltution), get(heightResolution)));
        Preferences.userNodeForPackage(getClass()).putInt("selectedSizeHeight", get(heightResolution));
        resources.refreshThemeMultiImages();
        previewScroll.revalidate();
        dirty = true;
}//GEN-LAST:event_heightResolutionStateChanged

private void systemFontSizeStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_systemFontSizeStateChanged
        JavaSEPortWithSVGSupport.setFontSize(get(systemFontSize), get(smallFontSize), get(largeFontSize));
        Preferences.userNodeForPackage(getClass()).putInt("selectedSizeFont", get(systemFontSize));
        refreshTheme(themeHash);
        previewScroll.revalidate();
}//GEN-LAST:event_systemFontSizeStateChanged

private void smallFontSizeStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_smallFontSizeStateChanged
        JavaSEPortWithSVGSupport.setFontSize(get(systemFontSize), get(smallFontSize), get(largeFontSize));
        Preferences.userNodeForPackage(getClass()).putInt("selectedSizeFontSmall", get(smallFontSize));
        refreshTheme(themeHash);
        previewScroll.revalidate();
}//GEN-LAST:event_smallFontSizeStateChanged

private void largeFontSizeStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_largeFontSizeStateChanged
        JavaSEPortWithSVGSupport.setFontSize(get(systemFontSize), get(smallFontSize), get(largeFontSize));
        Preferences.userNodeForPackage(getClass()).putInt("selectedSizeFontLarge", get(largeFontSize));
        refreshTheme(themeHash);
        previewScroll.revalidate();
}//GEN-LAST:event_largeFontSizeStateChanged

private void localePickerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_localePickerActionPerformed
    if(localePicker.getSelectedIndex() == 0) {
        Accessor.setResourceBundle(null);
        Preferences.userNodeForPackage(getClass()).remove("localeValue");
        Preferences.userNodeForPackage(getClass()).remove("localeLanguageValue");
    } else {
        String[] val = (String[])localePicker.getSelectedItem();
        Accessor.setResourceBundle(resources.getL10N(val[0], val[1]));
        Preferences.userNodeForPackage(getClass()).put("localeValue", val[0]);
        Preferences.userNodeForPackage(getClass()).put("localeLanguageValue", val[1]);
    }
}//GEN-LAST:event_localePickerActionPerformed

private void deviceTypeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deviceTypeActionPerformed
    int deviceTypeValue = deviceType.getSelectedIndex();
    Preferences.userNodeForPackage(getClass()).putInt("deviceTypeValue", deviceTypeValue);
    updateDeviceType(deviceTypeValue);
    if(initialized) {
        initMIDlet();
    }
}//GEN-LAST:event_deviceTypeActionPerformed

private void hideConfigActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hideConfigActionPerformed
    previewParentPanel.remove(configPane);
    previewParentPanel.add(java.awt.BorderLayout.NORTH, previewOptionsPosition);
    previewParentPanel.revalidate();
    previewParentPanel.repaint();
}//GEN-LAST:event_hideConfigActionPerformed

// taken from Heinz's excellent newsletter http://www.javaspecialists.eu/archive/Issue078.html
static class MemorySizes {
  private final Map primitiveSizes = new IdentityHashMap() {
    {
      put(boolean.class, new Integer(1));
      put(byte.class, new Integer(1));
      put(char.class, new Integer(2));
      put(short.class, new Integer(2));
      put(int.class, new Integer(4));
      put(float.class, new Integer(4));
      put(double.class, new Integer(8));
      put(long.class, new Integer(8));
    }
  };
  public int getPrimitiveFieldSize(Class clazz) {
    return ((Integer) primitiveSizes.get(clazz)).intValue();
  }
  public int getPrimitiveArrayElementSize(Class clazz) {
    return getPrimitiveFieldSize(clazz);
  }
  public int getPointerSize() {
    return 4;
  }
  public int getClassSize() {
    return 8;
  }
}

private static final MemorySizes sizes = new MemorySizes();

/**
 * This class can estimate how much memory an Object uses.  It is
 * fairly accurate for JDK 1.4.2.  It is based on the newsletter #29.
 */
private  static final class MemoryCounter {
  private final Map visited = new IdentityHashMap();
  private final Stack stack = new Stack();

  public synchronized long estimate(Object obj) {
    assert visited.isEmpty();
    assert stack.isEmpty();
    long result = _estimate(obj);
    while (!stack.isEmpty()) {
      result += _estimate(stack.pop());
    }
    visited.clear();
    return result;
  }

  private boolean skipObject(Object obj) {
    if (obj instanceof String) {
      // this will not cause a memory leak since
      // unused interned Strings will be thrown away
      if (obj == ((String) obj).intern()) {
        return true;
      }
    }
    return (obj == null)
        || visited.containsKey(obj);
  }

  private long _estimate(Object obj) {
    if (skipObject(obj)) return 0;
    visited.put(obj, null);
    long result = 0;

    if(obj instanceof WeakReference) {
        return 8;
    }

    Class clazz = obj.getClass();
    if(clazz == Class.class) {
        return 0;
    }
    if(clazz.getName().startsWith("java.awt")) {
        return 0;
    }
    if(obj instanceof com.codename1.ui.util.Resources) {
        return 0;
    }
    if(obj instanceof com.codename1.ui.util.UIBuilder) {
        return 0;
    }
    if (clazz.isArray()) {
      return _estimateArray(obj);
    }
    while (clazz != null) {
      Field[] fields = clazz.getDeclaredFields();
      for (int i = 0; i < fields.length; i++) {
        if (!Modifier.isStatic(fields[i].getModifiers())) {
          if (fields[i].getType().isPrimitive()) {
            result += sizes.getPrimitiveFieldSize(
                fields[i].getType());
          } else {
            result += sizes.getPointerSize();
            fields[i].setAccessible(true);
            try {
              Object toBeDone = fields[i].get(obj);
              if (toBeDone != null) {
                stack.add(toBeDone);
              }
            } catch (IllegalAccessException ex) { assert false; }
          }
        }
      }
      clazz = clazz.getSuperclass();
    }
    result += sizes.getClassSize();
    long rounded = roundUpToNearestEightBytes(result);
    //System.out.println("Estimating: " + clazz.getName() + " is " + rounded);
    return rounded;
  }

  private long roundUpToNearestEightBytes(long result) {
    if ((result % 8) != 0) {
      result += 8 - (result % 8);
    }
    return result;
  }

  protected long _estimateArray(Object obj) {
    long result = 16;
    int length = Array.getLength(obj);
    if (length != 0) {
      Class arrayElementClazz = obj.getClass().getComponentType();
      if (arrayElementClazz.isPrimitive()) {
        result += length *
            sizes.getPrimitiveArrayElementSize(arrayElementClazz);
      } else {
        for (int i = 0; i < length; i++) {
          result += sizes.getPointerSize() +
              _estimate(Array.get(obj, i));
        }
      }
    }
    return result;
  }
}

private void simulatorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_simulatorActionPerformed
    if(resources.getUIResourceNames().length > 0) { 
        String ui = (String)uiPreviewContent.getSelectedItem();
        if(ui == null) {
            ui = resources.getUIResourceNames()[0];
        }
        PreviewInSimulator.execute(this, themeName, view.getTemporarySaveOfCurrentFile(), ui);
    }
}//GEN-LAST:event_simulatorActionPerformed

private void borderWizardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_borderWizardActionPerformed
    ImageBorderWizardTabbedPane iw = new ImageBorderWizardTabbedPane(resources, themeName);
    JDialog dlg = new JDialog(SwingUtilities.windowForComponent(this), "Border Wizard");
    dlg.setLayout(new java.awt.BorderLayout());
    dlg.add(java.awt.BorderLayout.CENTER, iw);
    dlg.pack();
    dlg.setLocationRelativeTo(this);
    dlg.setModal(true);
    dlg.setVisible(true);
    themeHash = resources.getTheme(themeName);
    initTableModel(theme, null);
    initTableModel(selectedStyles, "sel#");
    initTableModel(pressedStyles, "press#");
    initTableModel(disabledStyles, "dis#");
    refreshTheme(themeHash);
}//GEN-LAST:event_borderWizardActionPerformed

    private Map<String, Long> sort(final Map<String, Long> m) {
        TreeMap<String, Long> result = new TreeMap(new Comparator<String>() {
            public int compare(String o1, String o2) {
                long v1 = m.get(o1);
                long v2 = m.get(o2);
                if(v1 == v2) {
                    return String.CASE_INSENSITIVE_ORDER.compare(o1, o2);
                }
                return (int)(v2 - v1);
            }
        });
        result.putAll(m);
        return result;
    }

    private void measureComponentPaintLayoutTime(com.codename1.ui.Component c, Map<String, Long> paintTimes, Map<String, Long> layoutTimes) {
        if(c.getName() != null && c.getWidth() > 0 && c.getHeight() > 0) {
            long t1;
            if(c instanceof com.codename1.ui.Container) {
                t1 = System.nanoTime();
                ((com.codename1.ui.Container)c).revalidate();
                ((com.codename1.ui.Container)c).revalidate();
                ((com.codename1.ui.Container)c).revalidate();
                ((com.codename1.ui.Container)c).revalidate();
                long layoutTime = (System.nanoTime() - t1) / 4;
                layoutTimes.put(c.getName(), layoutTime);
            }
            com.codename1.ui.Graphics lg = com.codename1.ui.Image.createImage(c.getWidth(), c.getHeight()).getGraphics();
            t1 = System.nanoTime();
            c.paintComponent(lg); c.paintComponent(lg); c.paintComponent(lg);
            long paintTime = (System.nanoTime() - t1) / 3;
            paintTimes.put(c.getName(), paintTime);
        }
        if(c instanceof com.codename1.ui.Tabs) {
            measureComponentPaintLayoutTime(((com.codename1.ui.Tabs)c).getTabComponentAt(((com.codename1.ui.Tabs)c).getSelectedIndex()),
                    paintTimes, layoutTimes);
        } else {
            if(c instanceof com.codename1.ui.Container) {
                com.codename1.ui.Container cnt = (com.codename1.ui.Container)c;
                for(int iter = 0 ; iter < cnt.getComponentCount() ; iter++) {
                    measureComponentPaintLayoutTime(cnt.getComponentAt(iter), paintTimes, layoutTimes);
                }
            }
        }
    }

    private static void refreshTheme(Hashtable theme) {
        Accessor.setTheme(theme);
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                Form f = Display.getInstance().getCurrent();
                if(f != null) {
                    f.refreshTheme();
                    f.revalidate();
                }
            }
        });
    }

    /**
     * The model connects the the UI to the editable resource information
     */
    class ThemeModel extends AbstractTableModel {
        private Hashtable theme;
        private List keys;
        private String prefix;
        public ThemeModel(Hashtable theme, String prefix) {
            this.theme = theme;
            this.prefix = prefix;
            keys = new ArrayList();
            if(prefix != null) {
                // special case for the constants table...
                if(prefix.equals("@")) {
                    for(Object keyObject : themeHash.keySet()) {
                        String key = (String)keyObject;
                        if(key.startsWith("@")) {
                            keys.add(keyObject);
                        }
                    }
                } else {
                    for(Object keyObject : themeHash.keySet()) {
                        String key = (String)keyObject;
                        if(key.indexOf(prefix) > -1) {
                            key = getUIID(key);
                            if(key != null && !keys.contains(key)) {
                                keys.add(key);
                            }
                        }
                    }
                }
            } else {
                for(Object keyObject : themeHash.keySet()) {
                    String key = (String)keyObject;
                    if(key.indexOf("#") < 0) {
                        key = getUIID(key);
                        if(key != null && !keys.contains(key)) {
                            keys.add(key);
                        }
                    }
                }
            }
            Collections.sort(keys);
            
            // place the default style first except for the case of the constants table
            if(prefix == null || prefix.indexOf('#') > -1) {
                keys.add(0, null);
            }
        }

        private String getUIID(String key) {
            if(key.indexOf('@') < 0) {
                int pos = key.indexOf('.');
                if(pos > -1) {
                    key = key.substring(0, pos);
                    return key;
                }
            }
            return null;
        }

        private void refreshTheme() {
            Accessor.setTheme(theme);
            Display.getInstance().callSeriallyAndWait(new Runnable() {
                public void run() {
                    try {
                        Display.getInstance().getCurrent().refreshTheme();
                    } catch(Throwable t) {}
                }
            });
        }


        public void remove(int offset) {
            if(prefix != null && prefix.indexOf('@') < 0) {
                if(offset == 0) {
                    return;
                }
            }
            String key = (String)keys.remove(offset);
            List<String> actualKeys = new ArrayList<String>();
            for(Object k : themeHash.keySet()) {
                String currentKey = (String)k;
                if(currentKey.startsWith(key + ".") || currentKey.equals(key)) {
                    actualKeys.add(currentKey);
                }
            }
            if(actualKeys.size() > 0) {
                String[] keys = new String[actualKeys.size()];
                actualKeys.toArray(keys);
                resources.setThemeProperties(themeName, keys, new Object[keys.length]);
            }
            //theme.remove(key);
            fireTableRowsDeleted(offset, offset);
            refreshTheme();
        }

        /*public Object getPropertyValue(String key) {
            return theme.get(key);
        }*/

        public int getRowCount() {
            return keys.size();
        }

        @Override
        public String getColumnName(int col) {
            if(col == 0) {
                if("@".equals(prefix)) {
                    return "Key";
                }
                return "Selector";
            }
            return "Value";
        }

        public int getColumnCount() {
            return 2;
        }

        /**
         * This method is only used for constants!
         */
        public void setKeyValue(String key, Object value) {
            //theme.put(key, value);
            resources.setThemeProperty(themeName, key, value);
            int row = keys.indexOf(key);
            fireTableRowsUpdated(row, row);
            refreshTheme();
        }

        /**
         * This method is only used for constants!
         */
        public void addKeyValue(String key, Object value) {
            keys.add(key);
            resources.setThemeProperty(themeName, key, value);
            //theme.put(key, value);
            Collections.sort(keys);
            int row = keys.indexOf(key);
            fireTableRowsInserted(row, row);
            refreshTheme();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            if(columnIndex == 1) {
                if("@".equals(prefix)) {
                    return theme.get(keys.get(rowIndex));
                }
            }
            return keys.get(rowIndex);
        }
    }

    private static com.codename1.ui.Button previewImage;
    private static com.codename1.ui.Image tempImage;

    public static Icon getUIIDPreviewImage(String value, boolean focus, boolean enabled, boolean pressed) {
        if(previewImage == null) {
            previewImage = new com.codename1.ui.Button("Preview");
            tempImage = com.codename1.ui.Image.createImage(100, 32);
            previewImage.setWidth(100);
            previewImage.setHeight(32);
        }
        if(value == null) {
            previewImage.setUIID("veryunlikelythatsomeonewilldefinethisuiid");
        } else {
            previewImage.setUIID((String)value);
        }
        previewImage.setFocus(focus);
        previewImage.setEnabled(enabled);
        com.codename1.ui.Graphics g = tempImage.getGraphics();
        g.setColor(0xffffff);
        g.fillRect(0, 0, tempImage.getWidth(), tempImage.getHeight());
        if(pressed) {
            previewImage.pressed();
            previewImage.paintComponent(g);
            previewImage.released();
        } else {
            previewImage.paintComponent(g);
        }
        return new CodenameOneImageIcon(tempImage, 100, 32);
    }

    class ThemeRenderer extends DefaultTableCellRenderer {
        public ThemeRenderer() {
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if(column == 0 || table == constantsTable) {
                setIcon(null);
                if(value == null || value instanceof String) {
                    String v = (String)value;
                    if(v == null) {
                        v = "<html><body><b>[Default Style]</b></body></html>";
                    } else {
                        if(v.startsWith("@")) {
                            v = v.substring(1, v.length());
                        }
                    }
                    return super.getTableCellRendererComponent(table, v, isSelected, hasFocus, row, column);
                } else {
                    if(value instanceof com.codename1.ui.Image) {
                        super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
                        setIcon(new CodenameOneImageIcon((com.codename1.ui.Image)value, 32, 32));
                        return this;
                    }
                }
            }
            super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
            boolean focus = (table == selectedStyles);
            boolean enabled = (table != disabledStyles);
            boolean pressed = (table == pressedStyles);
            
            setIcon(getUIIDPreviewImage((String)value, focus, enabled, pressed));
            return this;
        }
    }
    
    class MouseHandler implements MouseMotionListener, MouseListener {
        private Method mtd;
        private String lastComponent;
        private boolean lastComponentFocus;
        public void mouseDragged(MouseEvent e) {
        }

        public void mouseMoved(MouseEvent e) {
            Form f = Display.getInstance().getCurrent();
            if(f != null) {
                com.codename1.ui.Component c = f.getComponentAt(e.getX(), e.getY());
                if(c != null) {
                    try {
                        String tip = c.getUIID();
                        lastComponentFocus = c.hasFocus();
                        com.codename1.ui.Container parent = c.getParent();

                        // the code above shows softbuttons as buttons rather than as softbuttons
                        // which is not quite what we want...
                        if(parent != null && parent instanceof com.codename1.ui.MenuBar) {
                            // special case for title which falls into the gray area
                            if(!tip.equals("Title") || tip.equals("DialogTitle")) {
                                String parentTip = parent.getUIID();
                                if(parentTip != null) {
                                    tip = parentTip;
                                }
                            }
                        }

                        lastComponent = tip;
                        previewPanel.setToolTipText(tip);
                        return;
                    } catch(Exception err) {
                        // shouldn't happen
                        err.printStackTrace();
                    }
                }
            }
            setToolTipText("");
        }

        public void mouseClicked(MouseEvent e) {
            /*if(SwingUtilities.isRightMouseButton(e)) {
                String s = lastComponent;
                if(s != null && (!s.equals(""))) {
                    clearFlashingTimer();
                    String pref = null;
                    if(lastComponentFocus) {
                        pref = "sel#";
                    }
                    AddThemeEntry entry = new AddThemeEntry(true, resources, view, new Hashtable(themeHash), pref);
                    s = s + ".fgColor";
                    Object val = themeHash.get(s);
                    if(val != null) {
                        entry.setKeyValue(s, val);
                    } else {
                        Object fg = themeHash.get("fgColor");
                        if(fg != null) {
                            entry.setKeyValue(s, fg);
                        } else {
                            entry.setKeyValue(s, "000000");
                        }
                    }
                    showAddThemeEntry(entry);
                }
            }*/
        }

        public void mousePressed(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }
    }    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addThemeEntry;
    private javax.swing.JButton borderWizard;
    private javax.swing.JPanel configPane;
    private javax.swing.JTable constantsTable;
    private javax.swing.JComboBox deviceType;
    private javax.swing.JTable disabledStyles;
    private javax.swing.JButton editEntry;
    private javax.swing.JSpinner heightResolution;
    private javax.swing.JEditorPane help;
    private javax.swing.JButton hideConfig;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSpinner largeFontSize;
    private javax.swing.JComboBox localePicker;
    private javax.swing.JButton manageStyles;
    private javax.swing.JTable pressedStyles;
    private javax.swing.JPanel previewPanel;
    private javax.swing.JPanel previewParentPanel;
    private javax.swing.JScrollPane previewScroll;
    private javax.swing.JButton removeThemeEntry;
    private javax.swing.JTextField searchField;
    private javax.swing.JTable selectedStyles;
    private javax.swing.JButton simulator;
    private javax.swing.JSpinner smallFontSize;
    private javax.swing.JTabbedPane stylesTabbedPane;
    private javax.swing.JSpinner systemFontSize;
    private javax.swing.JTable theme;
    private javax.swing.JComboBox uiPreviewContent;
    private javax.swing.JSpinner widthResoltution;
    // End of variables declaration//GEN-END:variables
}
