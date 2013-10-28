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

import com.codename1.designer.ResourceEditorView;
import com.codename1.ui.plaf.Accessor;
import com.codename1.ui.resource.util.SwingRenderer;
import com.codename1.ui.util.EditableResources;
import com.codename1.ui.util.UIBuilderOverride;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Editor for resource localization data
 *
 * @author  Shai Almog
 */
public class L10nEditor extends BaseForm {
    //private Hashtable bundle;
    private List keys = new ArrayList();
    private List localeList = new ArrayList();
    private EditableResources res;
    private String localeName;
    
    /** Creates new form L10nEditor */
    public L10nEditor(EditableResources res, String localeName) {
        this.res = res;
        this.localeName = localeName;
        //bundle = resources;
        initLocaleList();
        for(Object locale : localeList) {
            Hashtable current = res.getL10N(localeName, (String)locale);
            for(Object key : current.keySet()) {
                if(!keys.contains(key)) {
                    keys.add(key);
                }
            }
        }
        Collections.sort(keys);
        initComponents();
        bindSearch(searchField, bundleTable);
        initTable();
        bundleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bundleTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                boolean v = bundleTable.getSelectedRowCount() == 1;
                removeProperty.setEnabled(v);
                renameProperty.setEnabled(v);
                v = v && bundleTable.getSelectedColumn() > 0;
                editText.setEnabled(v);
                editHTML.setEnabled(v);
            }
        });
    }
    
    private void initLocaleList() {
        Iterator localeIter = res.getLocales(localeName);
        localeList.clear();
        while(localeIter.hasNext()) {
            localeList.add(localeIter.next());
        }
        Collections.sort(localeList);
    }
    
    
    private void initTable() {
        bundleTable.setModel(new AbstractTableModel() {
            public int getRowCount() {
                return keys.size();
            }

            public int getColumnCount() {
                return 1 + localeList.size();
            }

            public boolean isCellEditable(int row, int col) {
                return col != 0;
            }
            
            public String getColumnName(int columnIndex) {
                if(columnIndex == 0) {
                    return "Key";
                }
                return (String)localeList.get(columnIndex - 1);
            }
            
            public Object getValueAt(int rowIndex, int columnIndex) {
                if(columnIndex == 0) {
                    return keys.get(rowIndex);
                }
                Hashtable h = res.getL10N(localeName, (String)localeList.get(columnIndex - 1));
                return h.get(keys.get(rowIndex));
            }

            public void setValueAt(Object val, int rowIndex, int columnIndex) {
                res.setModified();
                if(columnIndex == 0) {
                    if(!keys.contains(val)) {
                        // ... 
                    }
                    return;
                }
                //Hashtable h = (Hashtable)bundle.get(localeList.get(columnIndex - 1));
                //h.put(keys.get(rowIndex), val);
                String currentKey = (String)keys.get(rowIndex);
                res.setLocaleProperty(localeName, (String)localeList.get(columnIndex - 1), 
                    currentKey, val);
                if(currentKey.equals("@im")) {
                    StringTokenizer tok = new StringTokenizer((String)val, "|");
                    boolean modified = false;
                    while(tok.hasMoreTokens()) {
                        String currentIm = tok.nextToken();
                        if("ABC".equals(currentIm) ||  "123".equals(currentIm) || "Abc".equals(currentIm) || "abc".equals(currentIm)) {
                            continue;
                        }
                        String prop = "@im-" + currentIm;
                        if(!keys.contains(prop)) {
                            keys.add(prop);
                            for(Object locale : localeList) {
                                res.setLocaleProperty(localeName, (String)locale, prop, "");
                            }
                           modified = true;
                        }
                    }
                    if(modified) {
                        fireTableDataChanged();
                    }
                    return;
                }
                if(currentKey.equals("@vkb")) {
                    boolean modified = false;
                    StringTokenizer tok = new StringTokenizer((String)val, "|");
                    while(tok.hasMoreTokens()) {
                        String currentIm = tok.nextToken();
                        if("ABC".equals(currentIm) ||  "123".equals(currentIm) || ".,123".equals(currentIm) || ".,?".equals(currentIm)) {
                            continue;
                        }
                        String prop = "@vkb-" + currentIm;
                        if(!keys.contains(prop)) {
                            keys.add(prop);
                            for(Object locale : localeList) {
                                res.setLocaleProperty(localeName, (String)locale, prop, "");
                            }
                           modified = true;
                        }
                    }
                    if(modified) {
                        fireTableDataChanged();
                    }
                }
            }
        });
        bundleTable.setDefaultRenderer(Object.class, new SwingRenderer() {
            private JCheckBox chk = new JCheckBox();

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                if(column > 0) {
                    // constant value
                    String key = (String)keys.get(row);
                    if(key.startsWith("@")) {
                        if(key.equalsIgnoreCase("@rtl")) {
                            chk.setSelected(value != null && "true".equalsIgnoreCase(value.toString()));
                            updateComponentSelectedState(chk, isSelected, table, row, column, hasFocus);
                            return chk;
                        }
                        if(key.startsWith("@vkb") || key.startsWith("@im")) {
                            JButton b = new JButton("...");
                            updateComponentSelectedState(b, isSelected, table, row, column, hasFocus);
                            return b;
                        }
                    }
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        });
        bundleTable.setDefaultEditor(Object.class, new DefaultCellEditor(new JTextField())  {
            private Object currentValue;
            String editedKey;
            private DefaultCellEditor standardEditor = new DefaultCellEditor(new JTextField());
            private DefaultCellEditor buttonEditor = new DefaultCellEditor(new JTextField()) {
                private JButton button = new JButton("...");
                {
                    button.setBorderPainted(false);
                    button.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            if(editedKey.equals("@vkb") || editedKey.equals("@im")) {
                                currentValue = editInputModeOrder((String)currentValue, editedKey.equals("@vkb"));
                                fireEditingStoppedExt();
                                return;
                            }
                            if(editedKey.startsWith("@vkb")) {
                                VKBEditor v = new VKBEditor(button, editedKey.substring(5), (String)currentValue);
                                currentValue = v.getValue();
                                fireEditingStoppedExt();
                                return;
                            }
                            if(editedKey.startsWith("@im")) {
                                currentValue = editTextFieldInputMode((String)currentValue);
                                fireEditingStoppedExt();
                                return;
                            }
                        }
                    });
                }
                public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                    editedKey =  (String)keys.get(row);
                    return button;
                }
            };
            private DefaultCellEditor checkBoxEditor = new DefaultCellEditor(new JCheckBox()) {
                public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                    return super.getTableCellEditorComponent(table, new Boolean("true".equalsIgnoreCase((String)value)), isSelected, row, column);
                }

                public Object getCellEditorValue() {
                    Boolean b = (Boolean)super.getCellEditorValue();
                    if(b.booleanValue()) {
                        return "true";
                    }
                    return "false";
                }
            };
            private TableCellEditor current = standardEditor;

            {
                buttonEditor.setClickCountToStart(1);
                checkBoxEditor.setClickCountToStart(1);
            }

            private void updateEditor(int row) {
                // constant value
                final String key = (String)keys.get(row);
                if(key.startsWith("@")) {
                    if(key.equalsIgnoreCase("@rtl")) {
                        current = checkBoxEditor;
                        return;
                    }
                    if(key.startsWith("@vkb") || key.startsWith("@im")) {
                        current = buttonEditor;
                        return;
                    }
                }
                current = standardEditor;
            }

            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                updateEditor(row);
                currentValue = value;
                return current.getTableCellEditorComponent(table, value, isSelected, row, column);
            }

            public void fireEditingStoppedExt() {
                fireEditingStopped();
            }

            public Object getCellEditorValue() {
                if(current == buttonEditor) {
                    return currentValue;
                }
                return current.getCellEditorValue();
            }

            public boolean stopCellEditing() {
                return current.stopCellEditing();
            }

            public void cancelCellEditing() {
                current.cancelCellEditing();
            }

            public void addCellEditorListener(CellEditorListener l) {
                current.addCellEditorListener(l);
                super.addCellEditorListener(l);
            }

            public void removeCellEditorListener(CellEditorListener l) {
                current.removeCellEditorListener(l);
                super.removeCellEditorListener(l);
            }

            public boolean isCellEditable(EventObject anEvent) {
                return current.isCellEditable(anEvent);
            }

            public boolean shouldSelectCell(EventObject anEvent) {
                return current.shouldSelectCell(anEvent);
            }

        });
        locales.setModel(new DefaultComboBoxModel(localeList.toArray()));
        removeLocale.setEnabled(localeList.size() > 1);
    }

    String editTextFieldInputMode(String inputMode) {
        String[] array;
        if(inputMode == null || inputMode.length() == 0) {
            array = new String[0];
        } else {
            StringTokenizer t = new StringTokenizer(inputMode, "|");
            array = new String[t.countTokens()];
            for(int iter = 0 ; iter < array.length ; iter++) {
                array[iter] = t.nextToken();
            }
        }
        ArrayEditorDialog ed = new ArrayEditorDialog(this, null, array, "Input Mode", "/help/TextFieldInputModes.html") {
            protected Object edit(Object o) {
                int key = 0;
                String value = "";
                if(o != null) {
                    String t = (String)o;
                    int pos = t.indexOf('=');
                    key = Integer.parseInt(t.substring(0, pos));
                    value = t.substring(pos + 1);
                }
                InputModeKeyEditor keyEdit = new InputModeKeyEditor(key, value);
                if(showEditDialog(keyEdit)) {
                    return keyEdit.getKeycode() + "=" + keyEdit.getToggle();
                }
                return o;
            }
        };
        if(ed.isOK()) {
            List result = ed.getResult();
            StringBuilder r = new StringBuilder();
            boolean first = true;
            for(Object o : result) {
                if(!first) {
                    r.append('|');
                }
                first = false;
                r.append((String)o);
            }
            return r.toString();
        }
        return inputMode;
    }


    String editInputModeOrder(String inputMode, final boolean isVKBEdit) {
        String[] array;
        if(inputMode == null || inputMode.length() == 0) {
            array = new String[0];
        } else {
            StringTokenizer t = new StringTokenizer(inputMode, "|");
            array = new String[t.countTokens()];
            for(int iter = 0 ; iter < array.length ; iter++) {
                array[iter] = t.nextToken();
            }
        }
        ArrayEditorDialog ed = new ArrayEditorDialog(this, null, array, "Input Mode", "/help/InputModes.html") {
            protected Object edit(Object o) {
                String[] entries;
                if(isVKBEdit) {
                    entries = new String[] {"", "ABC", "123", ".,123", ".,?"};
                } else {
                    entries = new String[] {"", "ABC", "123", "Abc", "abc"};
                }
                JComboBox f = new JComboBox(entries);
                f.setEditable(true);
                if(o != null) {
                    f.setSelectedItem((String)o);
                }
                if(showEditDialog(f)) {
                    String selectedItem = (String)f.getSelectedItem();
                    // this feature breaks table cell editing by modifying the underlying table!
                    /*for(String current : entries) {
                        if(current.equals(selectedItem)) {
                            return selectedItem;
                        }
                    }
                    if(isVKBEdit) {
                        String prop = "@vkb-" + selectedItem;
                        if(!keys.contains(prop)) {
                            keys.add(prop);
                            for(Object locale : localeList) {
                                res.setLocaleProperty(localeName, (String)locale, prop, "");
                            }
                            initTable();
                        }
                    } else {
                        String prop = "@im-" + selectedItem;
                        if(!keys.contains(prop)) {
                            keys.add(prop);
                            for(Object locale : localeList) {
                                res.setLocaleProperty(localeName, (String)locale, prop, "");
                            }
                            initTable();
                        }
                    }*/
                    return selectedItem;
                }
                return o;
            }
        };
        if(ed.isOK()) {
            List result = ed.getResult();
            StringBuilder r = new StringBuilder();
            boolean first = true;
            for(Object o : result) {
                if(!first) {
                    r.append('|');
                }
                first = false;
                r.append((String)o);
            }
            return r.toString();
        }
        return inputMode;
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        bundleTable = createTable();
        jLabel1 = new javax.swing.JLabel();
        locales = new javax.swing.JComboBox();
        removeLocale = new javax.swing.JButton();
        addLocale = new javax.swing.JButton();
        addProperty = new javax.swing.JButton();
        removeProperty = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        searchField = new javax.swing.JTextField();
        importResource = new javax.swing.JButton();
        exportResource = new javax.swing.JButton();
        editText = new javax.swing.JButton();
        editHTML = new javax.swing.JButton();
        syncWithUI = new javax.swing.JButton();
        renameProperty = new javax.swing.JButton();

        FormListener formListener = new FormListener();

        setName("Form"); // NOI18N

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        bundleTable.setName("bundleTable"); // NOI18N
        jScrollPane1.setViewportView(bundleTable);

        jLabel1.setText("Locale");
        jLabel1.setName("jLabel1"); // NOI18N

        locales.setName("locales"); // NOI18N

        removeLocale.setText("Remove Locale");
        removeLocale.setName("removeLocale"); // NOI18N
        removeLocale.addActionListener(formListener);

        addLocale.setText("Add Locale");
        addLocale.setName("addLocale"); // NOI18N
        addLocale.addActionListener(formListener);

        addProperty.setText("Add Property");
        addProperty.setName("addProperty"); // NOI18N
        addProperty.addActionListener(formListener);

        removeProperty.setText("Remove Property");
        removeProperty.setEnabled(false);
        removeProperty.setName("removeProperty"); // NOI18N
        removeProperty.addActionListener(formListener);

        jLabel2.setText("Filter");
        jLabel2.setName("jLabel2"); // NOI18N

        searchField.setName("searchField"); // NOI18N

        importResource.setText("Import");
        importResource.setToolTipText("Import the locale from a properties file");
        importResource.setName("importResource"); // NOI18N
        importResource.addActionListener(formListener);

        exportResource.setText("Export");
        exportResource.setToolTipText("Export the selected locale to a resource bundle");
        exportResource.setName("exportResource"); // NOI18N
        exportResource.addActionListener(formListener);

        editText.setText("Edit Text");
        editText.setEnabled(false);
        editText.setName("editText"); // NOI18N
        editText.addActionListener(formListener);

        editHTML.setText("Edit HTML");
        editHTML.setEnabled(false);
        editHTML.setName("editHTML"); // NOI18N
        editHTML.addActionListener(formListener);

        syncWithUI.setText("Sync With UI");
        syncWithUI.setName("syncWithUI"); // NOI18N
        syncWithUI.addActionListener(formListener);

        renameProperty.setText("Rename Property");
        renameProperty.setEnabled(false);
        renameProperty.setName("renameProperty"); // NOI18N
        renameProperty.addActionListener(formListener);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 650, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, layout.createSequentialGroup()
                        .add(jLabel1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(locales, 0, 0, Short.MAX_VALUE)
                        .add(7, 7, 7)
                        .add(addLocale)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(removeLocale)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(exportResource)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(importResource)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(syncWithUI))
                    .add(org.jdesktop.layout.GroupLayout.LEADING, layout.createSequentialGroup()
                        .add(addProperty)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(removeProperty)
                        .add(6, 6, 6)
                        .add(renameProperty)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(editText)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(editHTML))
                    .add(org.jdesktop.layout.GroupLayout.LEADING, layout.createSequentialGroup()
                        .add(jLabel2)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(searchField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 616, Short.MAX_VALUE)))
                .addContainerGap())
        );

        layout.linkSize(new java.awt.Component[] {addLocale, addProperty, editHTML, editText, exportResource, importResource, removeLocale, removeProperty, renameProperty, syncWithUI}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(locales, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(addLocale)
                    .add(removeLocale)
                    .add(importResource)
                    .add(exportResource)
                    .add(syncWithUI))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel2)
                    .add(searchField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 203, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(addProperty)
                    .add(removeProperty)
                    .add(editText)
                    .add(editHTML)
                    .add(renameProperty))
                .addContainerGap())
        );
    }

    // Code for dispatching events from components to event handlers.

    private class FormListener implements java.awt.event.ActionListener {
        FormListener() {}
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            if (evt.getSource() == removeLocale) {
                L10nEditor.this.removeLocaleActionPerformed(evt);
            }
            else if (evt.getSource() == addLocale) {
                L10nEditor.this.addLocaleActionPerformed(evt);
            }
            else if (evt.getSource() == addProperty) {
                L10nEditor.this.addPropertyActionPerformed(evt);
            }
            else if (evt.getSource() == removeProperty) {
                L10nEditor.this.removePropertyActionPerformed(evt);
            }
            else if (evt.getSource() == importResource) {
                L10nEditor.this.importResourceActionPerformed(evt);
            }
            else if (evt.getSource() == exportResource) {
                L10nEditor.this.exportResourceActionPerformed(evt);
            }
            else if (evt.getSource() == editText) {
                L10nEditor.this.editTextActionPerformed(evt);
            }
            else if (evt.getSource() == editHTML) {
                L10nEditor.this.editHTMLActionPerformed(evt);
            }
            else if (evt.getSource() == syncWithUI) {
                L10nEditor.this.syncWithUIActionPerformed(evt);
            }
            else if (evt.getSource() == renameProperty) {
                L10nEditor.this.renamePropertyActionPerformed(evt);
            }
        }
    }// </editor-fold>//GEN-END:initComponents

private void removeLocaleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeLocaleActionPerformed
        if(localeList.size() < 2) {
            JOptionPane.showMessageDialog(this, "You can't remove all locales", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if(JOptionPane.showConfirmDialog(this, "Are you sure you want to remove " + locales.getSelectedItem() + "?",
            "Remove Locale", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE) ==
            JOptionPane.YES_OPTION) {
            res.removeLocale(localeName, (String)locales.getSelectedItem());
            initLocaleList();
            initTable();
        }
}//GEN-LAST:event_removeLocaleActionPerformed

private void addLocaleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addLocaleActionPerformed
        res.setModified();
        String locale = JOptionPane.showInputDialog(this, "Locale Name", "Add Locale", JOptionPane.PLAIN_MESSAGE);
        if(locale != null) {
            if(localeList.contains(locale)) {
                JOptionPane.showMessageDialog(this, "Locale Already Exists", "Add Locale", JOptionPane.ERROR_MESSAGE);
                return;
            }
            res.addLocale(localeName, locale);
            initLocaleList();
            initTable();
        }
}//GEN-LAST:event_addLocaleActionPerformed

private void addPropertyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addPropertyActionPerformed
        String prop = JOptionPane.showInputDialog(this, "Add Property", "Property", JOptionPane.PLAIN_MESSAGE);
        if(prop != null && prop.length() > 0) {
            if(keys.contains(prop)) {
                JOptionPane.showMessageDialog(this, "Property Already Exists", "Add Property", JOptionPane.ERROR_MESSAGE);
                return;
            }
            keys.add(prop);
            for(Object locale : localeList) {
                res.setLocaleProperty(localeName, (String)locale, prop, "");
                //((Hashtable)bundle.get(locale)).put(prop, "");
            }
            initTable();
        }
}//GEN-LAST:event_addPropertyActionPerformed

private void removePropertyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removePropertyActionPerformed
        Object key = bundleTable.getValueAt(getModelSelection(bundleTable), 0);
        if(JOptionPane.showConfirmDialog(this, "Are you sure you want to remove " + key + "?",
            "Remove Key", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE) ==
            JOptionPane.YES_OPTION) {
            keys.remove(key);
            for(Object locale : localeList) {
                res.setLocaleProperty(localeName, (String)locale, (String)key, null);
                //((Hashtable)bundle.get(locale)).remove(key);
            }
            initTable();
        }
}//GEN-LAST:event_removePropertyActionPerformed

    static String xmlize(String s) {
        s = s.replace("&", "&amp;");
        s = s.replace("<", "&lt;");
        s = s.replace(">", "&gt;");
        s = s.replace("\"", "&quot;");
        int charCount = s.length();
        for(int iter = 0 ; iter < charCount ; iter++) {
            char c = s.charAt(iter);
            if(c > 127) {
                // we need to localize the string...
                StringBuilder b = new StringBuilder();
                for(int counter = iter ; counter < charCount ; counter++) {
                    c = s.charAt(counter);
                    if(c > 127) {
                        b.append("&#x");
                        b.append(Integer.toHexString(c));
                        b.append(";");
                    } else {
                        b.append(c);
                    }
                }
                return b.toString();
            }
        }
        return s;
    }


private void exportResourceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportResourceActionPerformed
        Object[] options = new Object[] {"Properties", "CSV With ;", "CSV With ,", "Android Strings"};
        int result = JOptionPane.showOptionDialog(this, "Export file type", "File Type", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if(result == JOptionPane.CLOSED_OPTION) {
            return;
        }
        File[] file = ResourceEditorView.showSaveFileChooser();
        if(file != null) {
            FileOutputStream out = null;
            try {
                File f = file[0];
                if (f.exists()) {
                    int val = JOptionPane.showConfirmDialog(this, "do you want to overwrite?", "File Exists", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (val == JOptionPane.NO_OPTION) {
                        exportResourceActionPerformed(evt);
                        return;
                    }
                } else {
                    if(f.getName().indexOf('.') < 0) {
                        if(result == 0) {
                            f = new File(f.getAbsolutePath() + ".properties");
                        } else {
                            f = new File(f.getAbsolutePath() + ".csv");
                        }
                    }
                }
                if(result == 0) {
                    Properties prop = new Properties();
                    String locale = (String) locales.getSelectedItem();
                    Hashtable h = res.getL10N(localeName, locale);
                    prop.putAll(h);
                    out = new FileOutputStream(f);
                    prop.store(out, "Export locale from the Codename One Designer");
                    out.close();
                } else {
                    if(result == 3) {
                        out = new FileOutputStream(f);
                        Writer w = new OutputStreamWriter(out, "UTF-8");
                        w.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
                        w.write("<resources>");
                        String locale = (String) locales.getSelectedItem();
                        Hashtable<String, String> h = res.getL10N(localeName, locale);
                        
                        for(Map.Entry<String, String> e : h.entrySet()) {
                            w.write("    <string name=\"");
                            w.write(xmlize(e.getKey()));
                            w.write("\">");
                            w.write(xmlize(e.getValue()));
                            w.write("</string>\n");
                        }
                        
                        w.write("</resources>");
                        w.close();
                    } else {
                        char separator = ';';
                        if(result == 2) {
                            separator = ',';
                        }
                        out = new FileOutputStream(f);

                        // Write BOM for excel/windows apps
                        out.write(new byte[] {(byte)0xEF, (byte)0xBB, (byte)0xBF});
                        out.flush();
                        Writer w = new OutputStreamWriter(out, "UTF-8");


                        TableModel m = bundleTable.getModel();

                        int rowCount = m.getRowCount();
                        int columnCount = m.getColumnCount();
                        for(int col = 0 ; col < columnCount ; col++) {
                            w.append(m.getColumnName(col));
                            w.append(separator);
                        }
                        w.append('\n');

                        for(int row = 0 ; row < rowCount ; row++) {
                            for(int col = 0 ; col < columnCount ; col++) {
                                String c = (String)m.getValueAt(row, col);
                                c = c.replaceAll("\"", "\"\"");
                                w.append('"');
                                w.append(c);
                                w.append('"');
                                w.append(separator);
                            }
                            w.append('\n');
                        }

                        w.close();
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error: " + ex, "IO Error Occured", JOptionPane.ERROR_MESSAGE);
            } 
        }
}//GEN-LAST:event_exportResourceActionPerformed

private void importResourceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importResourceActionPerformed
        final String locale = (String) locales.getSelectedItem();
        int val = JOptionPane.showConfirmDialog(this, "This will overwrite existing values for " + locale + "\nAre you sure?", "Import", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (val == JOptionPane.YES_OPTION) {
            File[] files = ResourceEditorView.showOpenFileChooser("Properties Or XML", "prop", "properties", "l10n", "locale", "xml");
            if(files != null) {
                FileInputStream f = null;
                try {
                    f = new FileInputStream(files[0]);
                    if(files[0].getName().toLowerCase().endsWith("xml")) {
                        SAXParserFactory spf = SAXParserFactory.newInstance();
                        SAXParser saxParser = spf.newSAXParser();
                        XMLReader xmlReader = saxParser.getXMLReader();
                        xmlReader.setContentHandler(new ContentHandler() {
                            private String currentName;
                            private StringBuilder chars = new StringBuilder();
                            
                            @Override
                            public void setDocumentLocator(Locator locator) {
                            }

                            @Override
                            public void startDocument() throws SAXException {
                            }

                            @Override
                            public void endDocument() throws SAXException {
                            }

                            @Override
                            public void startPrefixMapping(String prefix, String uri) throws SAXException {
                            }

                            @Override
                            public void endPrefixMapping(String prefix) throws SAXException {
                            }

                            @Override
                            public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
                                if("string".equals(localName)) {
                                    currentName = atts.getValue("name");
                                    chars.setLength(0);
                                }
                            }

                            @Override
                            public void endElement(String uri, String localName, String qName) throws SAXException {
                                if("string".equals(localName)) {
                                    res.setLocaleProperty(localeName, locale, currentName, chars.toString());
                                }
                            }

                            @Override
                            public void characters(char[] ch, int start, int length) throws SAXException {
                                chars.append(ch, start, length);
                            }

                            @Override
                            public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
                            }

                            @Override
                            public void processingInstruction(String target, String data) throws SAXException {
                            }

                            @Override
                            public void skippedEntity(String name) throws SAXException {
                            }
                        });
                        xmlReader.parse(new InputSource(f));
                    } else {
                        Properties prop = new Properties();
                        prop.load(f);
                        for (Object key : prop.keySet()) {
                            res.setLocaleProperty(localeName, locale, (String)key, prop.getProperty((String)key));
                        }
                    }
                    f.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Error: " + ex, "Error Occured", JOptionPane.ERROR_MESSAGE);
                } 
            }
        }    
}//GEN-LAST:event_importResourceActionPerformed

private void editTextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editTextActionPerformed
    int row = bundleTable.getSelectedRow();
    int column = bundleTable.getSelectedColumn();
    JTextArea t = new JTextArea((String)bundleTable.getValueAt(row, column), 5, 40);
    t.setLineWrap(true);
    t.setWrapStyleWord(true);
    int r = JOptionPane.showConfirmDialog(this, new JScrollPane(t), "Edit", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    if(r == JOptionPane.OK_OPTION) {
        bundleTable.setValueAt(t.getText(), row, column);
    }
}//GEN-LAST:event_editTextActionPerformed

private void editHTMLActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editHTMLActionPerformed
    int row = bundleTable.getSelectedRow();
    int column = bundleTable.getSelectedColumn();
    HTMLEditor h = new HTMLEditor(res, (String)bundleTable.getValueAt(row, column));
    h.setPreferredSize(new Dimension(600, 400));
    int r = JOptionPane.showConfirmDialog(this, h, "Edit", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    if(r == JOptionPane.OK_OPTION) {
        bundleTable.setValueAt(h.getResult(), row, column);
    }

}//GEN-LAST:event_editHTMLActionPerformed

private void syncWithUIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_syncWithUIActionPerformed
    Accessor.setResourceBundle(null);
    final Map<String, String> allKeys = new HashMap<String, String>();
    com.codename1.ui.plaf.UIManager original = com.codename1.ui.plaf.UIManager.getInstance();
    Accessor.setUIManager(new com.codename1.ui.plaf.ProtectedUIManager() {
        public String localize(String key, String defaultValue) {
            if(key != null && key.length() > 0 && defaultValue != null && defaultValue.length() > 0) {
                allKeys.put(key, defaultValue);
            }
            return super.localize(key, defaultValue);
        }
    });
    UIBuilderOverride o = new UIBuilderOverride(null);
    for(String resources : res.getUIResourceNames()) {
        o.createContainer(res, resources);
    }
    Accessor.setUIManager(original);
    for(String currentKey : allKeys.keySet()) {
        if(!keys.contains(currentKey)) {
            keys.add(currentKey);
            for(Object locale : localeList) {
                res.setLocaleProperty(localeName, (String)locale, currentKey, allKeys.get(currentKey));
            }
        }
    }
    initTable();
}//GEN-LAST:event_syncWithUIActionPerformed

private void renamePropertyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_renamePropertyActionPerformed
        Object key = bundleTable.getValueAt(getModelSelection(bundleTable), 0);
        JTextField newName = new JTextField((String)key);
        if(JOptionPane.showConfirmDialog(this, newName,
            "Rename", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) ==
            JOptionPane.OK_OPTION) {
            if(keys.contains(newName.getText())) {
                JOptionPane.showMessageDialog(this, "Name Already In Use", "Rename", JOptionPane.ERROR_MESSAGE);
                return;
            }
            keys.remove(key);
            keys.add(newName.getText());
            for(Object locale : localeList) {
                Hashtable h = res.getL10N(localeName, (String)locale);
                String val = (String)h.get(key);
                res.setLocaleProperty(localeName, (String)locale, (String)key, null);
                res.setLocaleProperty(localeName, (String)locale, newName.getText(), val);
            }
            initTable();
        }

}//GEN-LAST:event_renamePropertyActionPerformed
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addLocale;
    private javax.swing.JButton addProperty;
    private javax.swing.JTable bundleTable;
    private javax.swing.JButton editHTML;
    private javax.swing.JButton editText;
    private javax.swing.JButton exportResource;
    private javax.swing.JButton importResource;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JComboBox locales;
    private javax.swing.JButton removeLocale;
    private javax.swing.JButton removeProperty;
    private javax.swing.JButton renameProperty;
    private javax.swing.JTextField searchField;
    private javax.swing.JButton syncWithUI;
    // End of variables declaration//GEN-END:variables
    
}
