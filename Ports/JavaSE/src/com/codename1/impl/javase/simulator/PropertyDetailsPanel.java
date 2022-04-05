/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.javase.simulator;

import com.codename1.io.Log;
import com.codename1.ui.Component;
import java.awt.BorderLayout;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

/**
 *
 * @author shannah
 */
public class PropertyDetailsPanel extends JPanel {
    private JTable propertiesTable;
    private JTextField filter;
    private Component currentComponent;
    private Map<String,ComponentProperty> propertiesMap = new LinkedHashMap<String,ComponentProperty>();
    private List<ComponentProperty> propertiesList = new ArrayList<ComponentProperty>();
    private PropertiesTableRowSorter rowSorter;
    
    
    public PropertyDetailsPanel() {
        buildUI();
    }
    
    public void setCurrentComponent(Component currentComponent) {
        if (currentComponent != this.currentComponent) {
            this.currentComponent = currentComponent;
            update();
        }
    }
    
    private void buildUI() {
        setLayout(new BorderLayout());
        propertiesTable = new JTable(new PropertiesTableModel());
        rowSorter = new PropertiesTableRowSorter((PropertiesTableModel) propertiesTable.getModel());
        propertiesTable.setRowSorter(rowSorter);
        filter = new JTextField();
        filter.putClientProperty("JTextField.variant", "search");
        filter.setToolTipText("Filter properties");
        filter.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                rowSorter.updateFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                rowSorter.updateFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                rowSorter.updateFilter();
            }
        });
        JScrollPane scroller = new JScrollPane(propertiesTable);
        add(filter, BorderLayout.NORTH);
        add(scroller, BorderLayout.CENTER);

        
    }
    
    private void update() {
        updateProperties(currentComponent);
        revalidate();
        
    }
    
    private static String methodPropertyName_(String name) {
        return name.startsWith("get") ? name.substring(3) : name.startsWith("is") ? name.substring(2) : name;
    }
    
    private class ComponentProperty {
        private Method setter, getter;
        private String name;
        private Component cmp;
        
        Object getValue() {
            if (getter != null) {
                try {
                    return getter.invoke(cmp, new Object[0]);
                } catch (Exception ex){}
            }
            return null;
        }
        
        String getStringValue() {
            return String.valueOf(getValue());
        }
        
        void setValue(Object value) {
            if (setter != null) {
                try {
                    setter.invoke(cmp, new Object[]{value});
                
                } catch (Exception ex) {
                    Log.e(ex);
                }
            }
        }
    }
    
    private void addGetter(String propertyName, Method getter) {
        ComponentProperty prop = propertiesMap.get(propertyName);
        if (prop == null) {
            prop = new ComponentProperty();
            prop.name = propertyName;
            prop.cmp = currentComponent;
            propertiesMap.put(propertyName, prop);
            propertiesList.add(prop);
        }
        prop.getter = getter;
    }
    
    private void addSetter(String propertyName, Method setter) {
        ComponentProperty prop = propertiesMap.get(propertyName);
        if (prop == null) {
            prop = new ComponentProperty();
            prop.name = propertyName;
            prop.cmp = currentComponent;
            propertiesMap.put(propertyName, prop);
            propertiesList.add(prop);
        }
        prop.setter = setter;
    }
    
    
    
    
    private void updateProperties(Object cmp) {
        
        propertiesMap.clear();
        propertiesList.clear();
        if (cmp == null) return;
        Class cls = cmp.getClass();
        Method[] methods = cls.getMethods();
        Arrays.sort(methods, new Comparator<Method>() {

            @Override
            public int compare(Method o1, Method o2) {
                return methodPropertyName_(o1.getName()).toLowerCase().compareTo(methodPropertyName_(o2.getName()).toLowerCase());
            }
        });
       
        for (int i=0; i<methods.length; i++) {
            Method method = methods[i];
            method.setAccessible(true);
            String name = method.getName();
            String propertyName = methodPropertyName_(name);
            if ((name.startsWith("get") || name.startsWith("is") || name.equalsIgnoreCase("scrollableYFlag") || name.equalsIgnoreCase("scrollableXFlag")) && method.getParameterCount() == 0 && method.getReturnType() != Void.class) {
                try {
                    addGetter(propertyName, method);
                    
                } catch (Exception ex){}
            } else if (name.startsWith("set") &&  method.getParameterCount() == 1 && method.getReturnType() == Void.class) {
                try {
                    addSetter(propertyName, method);
                    
                } catch (Exception ex){}
            }
        }
        propertiesTable.setModel(new PropertiesTableModel());
        rowSorter.setModel((PropertiesTableModel)propertiesTable.getModel());
        
    }
    
    private class PropertiesTableModel extends AbstractTableModel {

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0: return "Name";
                default: return "Value";
            }
        }

        @Override
        public int getRowCount() {
            return propertiesMap.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public Object getValueAt(int row, int col) {
            ComponentProperty prop = propertiesList.get(row);
            if (col == 0) {
                return prop.name;
            } else {
                return prop.getStringValue();
            }
        }
        
    }


    private class PropertiesTableRowSorter extends TableRowSorter<PropertiesTableModel> {

        PropertiesTableRowSorter(PropertiesTableModel model) {
            super(model);
            if (filter != null && filter.getText().length() > 0) {
                setRowFilter(RowFilter.regexFilter(filter.getText(), 0));
            } else {
                setRowFilter(null);
            }
        }

        void updateFilter() {

            if (filter != null && !filter.getText().isEmpty()) {
                setRowFilter(RowFilter.regexFilter(filter.getText(), 0));
            } else {
                setRowFilter(null);
            }
        }
    }
   
    
}
